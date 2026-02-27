"""RecommendationService 단위 테스트."""

import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from llm.circuit_breaker import CBState, CircuitBreaker
from llm.llm_client import LLMInvokeError
from model.common import AiSource, CBStateEnum
from model.recommendation_request import AiRecommendationRequest
from service.recommendation_service import RecommendationService


MOCK_REC_JSON = json.dumps({
    "recommendations": [
        {
            "restaurant_id": "rest-001",
            "restaurant_name": "광화문 된장마을",
            "representative_menu": "된장찌개 정식",
            "category": "한식",
            "reason_summary": "따뜻한 한식이 딱",
            "confidence_score": 87,
        }
    ]
}, ensure_ascii=False)

MOCK_TOKEN_USAGE = {"prompt_tokens": 500, "completion_tokens": 200, "total_tokens": 700}


def _make_request(is_cold_start: bool = False, feedback_count: int = 12) -> AiRecommendationRequest:
    data = {
        "memberId": "test-member-001",
        "latitude": 37.5665,
        "longitude": 126.9780,
        "requestedAt": "2026-02-26T12:00:00Z",
        "isColdStart": is_cold_start,
        "feedbackCount": feedback_count,
        "tasteVector": {"한식": 0.85, "일식": 0.70} if not is_cold_start else None,
        "onboardingSwipes": [
            {"cardId": "c1", "category": "한식", "liked": True}
        ] if is_cold_start else None,
        "allergenFilter": ["땅콩"],
        "dietType": "일반",
        "weather": {"condition": "RAINY", "temperatureCelsius": 8.5, "description": "비 오는 날"},
        "recentMealHistory": [],
        "excludeRestaurantIds": [],
        "jobCluster": "IT_OFFICE_WORKER" if is_cold_start else None,
    }
    return AiRecommendationRequest(**data)


def _make_service_with_mocks(
    llm_content: str = MOCK_REC_JSON,
    cache_hit: dict | None = None,
    stale_hit: dict | None = None,
    cb_open: bool = False,
) -> RecommendationService:
    """테스트용 서비스 인스턴스 생성 (모든 의존성 mock)."""
    mock_cache = AsyncMock()
    mock_cache.get_recommendation.return_value = cache_hit
    mock_cache.get_stale_recommendation.return_value = stale_hit
    mock_cache.get_prompt_hash.return_value = None
    mock_cache.set_recommendation = AsyncMock(return_value="rec:test:key")
    mock_cache.set_prompt_hash = AsyncMock()

    mock_llm = MagicMock()
    mock_cb = MagicMock(spec=CircuitBreaker)
    mock_cb.is_open.return_value = cb_open
    mock_cb.state.value = "OPEN" if cb_open else "CLOSED"
    mock_cb.failure_count = 5 if cb_open else 0

    mock_llm.circuit_breaker = mock_cb
    mock_llm.generate_recommendation = AsyncMock(return_value=(llm_content, MOCK_TOKEN_USAGE))
    mock_llm.generate_coldstart_recommendation = AsyncMock(
        return_value=(llm_content, MOCK_TOKEN_USAGE)
    )
    mock_llm._primary_model = MagicMock()
    mock_llm._primary_model.model_name = "claude-3-5-haiku-20241022"
    mock_llm._coldstart_model = MagicMock()
    mock_llm._coldstart_model.model_name = "claude-3-5-sonnet-20241022"

    return RecommendationService(
        cache_manager=mock_cache,
        llm_client=mock_llm,
    )


@pytest.mark.asyncio
async def test_cache_hit_returns_cached_response():
    cached_data = [
        {
            "restaurantId": "rest-001",
            "restaurantName": "광화문 된장마을",
            "representativeMenu": "된장찌개 정식",
            "category": "한식",
            "reasonSummary": "비 오는 날엔 따뜻한 한식",
            "confidenceScore": 87,
            "distanceMeters": None,
            "estimatedWalkMinutes": None,
        }
    ]
    service = _make_service_with_mocks(cache_hit=cached_data)
    request = _make_request()

    response = await service.generate(request)

    assert response.metadata.source == AiSource.CACHE
    assert len(response.recommendations) >= 1
    assert response.is_fallback is False


@pytest.mark.asyncio
async def test_normal_user_llm_call():
    service = _make_service_with_mocks()
    request = _make_request(is_cold_start=False, feedback_count=12)

    response = await service.generate(request)

    assert response.metadata.source == AiSource.LLM
    assert response.is_cold_start is False
    assert response.cold_start_tag is None
    assert response.is_fallback is False
    assert len(response.recommendations) >= 1


@pytest.mark.asyncio
async def test_coldstart_user_llm_call():
    service = _make_service_with_mocks()
    request = _make_request(is_cold_start=True, feedback_count=2)

    response = await service.generate(request)

    assert response.metadata.source == AiSource.COLD_START_LLM
    assert response.is_cold_start is True
    assert response.cold_start_tag is not None
    assert response.is_fallback is False


@pytest.mark.asyncio
async def test_cb_open_stale_cache_returns_stale():
    stale_data = [
        {
            "restaurantId": "rest-001",
            "restaurantName": "광화문 된장마을",
            "representativeMenu": "된장찌개 정식",
            "category": "한식",
            "reasonSummary": "stale 캐시",
            "confidenceScore": 80,
            "distanceMeters": None,
            "estimatedWalkMinutes": None,
        }
    ]
    service = _make_service_with_mocks(stale_hit=stale_data, cb_open=True)
    request = _make_request()

    response = await service.generate(request)

    assert response.metadata.source == AiSource.STALE_CACHE
    assert response.is_fallback is True
    assert response.cached_until is None
    assert response.metadata.circuit_breaker_state == CBStateEnum.OPEN


@pytest.mark.asyncio
async def test_cb_open_no_stale_returns_fallback():
    service = _make_service_with_mocks(stale_hit=None, cb_open=True)
    request = _make_request()

    response = await service.generate(request)

    assert response.metadata.source == AiSource.FALLBACK_RULE_BASED
    assert response.is_fallback is True
    assert len(response.recommendations) >= 1


@pytest.mark.asyncio
async def test_llm_failure_returns_fallback():
    mock_cache = AsyncMock()
    mock_cache.get_recommendation.return_value = None
    mock_cache.get_stale_recommendation.return_value = None
    mock_cache.get_prompt_hash.return_value = None
    mock_cache.set_recommendation = AsyncMock(return_value="rec:test:key")
    mock_cache.set_prompt_hash = AsyncMock()

    mock_llm = MagicMock()
    mock_llm.circuit_breaker = CircuitBreaker(failure_threshold=5)
    mock_llm.generate_recommendation = AsyncMock(
        side_effect=LLMInvokeError("LLM 연결 실패")
    )
    mock_llm._primary_model = MagicMock()
    mock_llm._coldstart_model = MagicMock()

    service = RecommendationService(cache_manager=mock_cache, llm_client=mock_llm)
    request = _make_request()

    response = await service.generate(request)

    assert response.is_fallback is True
    assert response.metadata.source == AiSource.FALLBACK_RULE_BASED
    assert len(response.recommendations) >= 1
