"""ReasonService 단위 테스트."""

import json
from unittest.mock import AsyncMock, MagicMock

import pytest

from llm.circuit_breaker import CircuitBreaker
from llm.llm_client import LLMInvokeError
from model.common import AiSource, CBStateEnum
from model.reason_request import AiReasonRequest
from service.reason_service import ReasonService


MOCK_REASON_JSON = json.dumps({
    "natural_language_reason": "비 오는 날 + 어제 양식 드셨으니 → 따뜻한 한식을 추천드려요",
    "confidence_score": 87,
    "context_tags": ["날씨", "이력"],
}, ensure_ascii=False)

MOCK_TOKEN_USAGE = {"prompt_tokens": 300, "completion_tokens": 100, "total_tokens": 400}


def _make_reason_request() -> AiReasonRequest:
    return AiReasonRequest(**{
        "recommendationId": "rec-test-001",
        "restaurantId": "rest-001",
        "restaurantName": "광화문 된장마을",
        "category": "한식",
        "representativeMenu": "된장찌개 정식",
        "memberId": "member-001",
        "tasteVector": {"한식": 0.85, "일식": 0.70},
        "weather": {"condition": "RAINY", "temperatureCelsius": 8.5, "description": "비 오는 날"},
        "recentMealHistory": [
            {"restaurantId": "rest-007", "category": "양식", "mealDate": "2026-02-25", "satisfaction": "GOOD"}
        ],
        "confidenceScore": 87,
    })


def _make_service(
    llm_content: str = MOCK_REASON_JSON,
    cache_hit: dict | None = None,
    cb_open: bool = False,
    llm_error: Exception | None = None,
) -> ReasonService:
    mock_cache = AsyncMock()
    mock_cache.get_reason.return_value = cache_hit
    mock_cache.set_reason = AsyncMock()

    mock_llm = MagicMock()
    mock_cb = MagicMock(spec=CircuitBreaker)
    mock_cb.is_open.return_value = cb_open
    mock_cb.state.value = "OPEN" if cb_open else "CLOSED"
    mock_cb.failure_count = 5 if cb_open else 0

    mock_llm.circuit_breaker = mock_cb
    if llm_error:
        mock_llm.generate_reason = AsyncMock(side_effect=llm_error)
    else:
        mock_llm.generate_reason = AsyncMock(return_value=(llm_content, MOCK_TOKEN_USAGE))
    mock_llm._reason_model = MagicMock()
    mock_llm._reason_model.model_name = "claude-3-5-haiku-20241022"

    return ReasonService(cache_manager=mock_cache, llm_client=mock_llm)


@pytest.mark.asyncio
async def test_cache_hit_returns_cached_reason():
    cached = {
        "recommendation_id": "rec-test-001",
        "natural_language_reason": "캐시된 이유",
        "confidence_score": 87,
        "context_tags": ["날씨"],
        "is_reason_ready": True,
        "fallback_reason": None,
    }
    service = _make_service(cache_hit=cached)
    request = _make_reason_request()

    response = await service.generate_reason(request)

    assert response.metadata.source == AiSource.CACHE
    assert response.natural_language_reason == "캐시된 이유"
    assert response.is_reason_ready is True


@pytest.mark.asyncio
async def test_llm_success_returns_reason():
    service = _make_service()
    request = _make_reason_request()

    response = await service.generate_reason(request)

    assert response.metadata.source == AiSource.LLM
    assert response.is_reason_ready is True
    assert response.fallback_reason is None
    assert "한식" in response.natural_language_reason or "추천" in response.natural_language_reason
    assert response.confidence_score == 87
    assert "날씨" in response.context_tags or "이력" in response.context_tags


@pytest.mark.asyncio
async def test_cb_open_returns_fallback():
    service = _make_service(cb_open=True)
    request = _make_reason_request()

    response = await service.generate_reason(request)

    assert response.metadata.source == AiSource.FALLBACK_RULE_BASED
    assert response.is_reason_ready is False
    assert response.fallback_reason is not None
    assert response.cached_until is None
    assert response.metadata.circuit_breaker_state == CBStateEnum.OPEN


@pytest.mark.asyncio
async def test_llm_failure_returns_fallback():
    service = _make_service(llm_error=LLMInvokeError("LLM 연결 실패"))
    request = _make_reason_request()

    response = await service.generate_reason(request)

    assert response.metadata.source == AiSource.FALLBACK_RULE_BASED
    assert response.is_reason_ready is False
    assert response.fallback_reason is not None


@pytest.mark.asyncio
async def test_reason_cache_saved_after_llm_success():
    service = _make_service()
    request = _make_reason_request()

    await service.generate_reason(request)

    service._cache.set_reason.assert_called_once()
