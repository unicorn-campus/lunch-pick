"""
pytest 공통 픽스처.

LLM mock 픽스처: 실제 LLM API 호출 없이 테스트.
Redis mock 픽스처: 실제 Redis 연결 없이 캐시 테스트.
"""

import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

from main import app

# ── LLM Mock 응답 데이터 ──────────────────────────────────────────────────

MOCK_RECOMMENDATION_JSON = json.dumps({
    "recommendations": [
        {
            "restaurant_id": "rest-001",
            "restaurant_name": "광화문 된장마을",
            "representative_menu": "된장찌개 정식",
            "category": "한식",
            "reason_summary": "비 오는 날엔 따뜻한 한식",
            "confidence_score": 87,
        },
        {
            "restaurant_id": "rest-002",
            "restaurant_name": "사쿠라 스시",
            "representative_menu": "런치 세트",
            "category": "일식",
            "reason_summary": "어제 양식 드셔서 오늘은 일식",
            "confidence_score": 72,
        },
        {
            "restaurant_id": "rest-003",
            "restaurant_name": "그린 샐러드바",
            "representative_menu": "프레시 볼",
            "category": "샐러드/건강식",
            "reason_summary": "건강한 취향에 맞게 추천",
            "confidence_score": 65,
        },
    ]
}, ensure_ascii=False)

MOCK_COLDSTART_JSON = json.dumps({
    "recommendations": [
        {
            "restaurant_id": "rest-004",
            "restaurant_name": "종로 한식뷔페",
            "representative_menu": "한식 뷔페",
            "category": "한식",
            "reason_summary": "IT 직장인들이 즐겨 찾는 식당",
            "confidence_score": 55,
        },
    ]
}, ensure_ascii=False)

MOCK_REASON_JSON = json.dumps({
    "natural_language_reason": "비 오는 날 + 어제 양식 드셨으니 → 따뜻한 한식을 추천드려요",
    "confidence_score": 87,
    "context_tags": ["날씨", "이력"],
}, ensure_ascii=False)


def _make_mock_llm_response(content: str) -> MagicMock:
    """LLM AIMessage mock 생성."""
    mock_response = MagicMock()
    mock_response.content = content
    mock_response.usage_metadata = MagicMock(input_tokens=500, output_tokens=200)
    mock_response.response_metadata = {"usage": {"input_tokens": 500, "output_tokens": 200}}
    return mock_response


# ── 공통 픽스처 ──────────────────────────────────────────────────────────

@pytest.fixture
def mock_llm_recommendation():
    """일반 추천 LLM mock — 실제 API 호출 차단."""
    mock_response = _make_mock_llm_response(MOCK_RECOMMENDATION_JSON)
    with patch("llm.llm_client.LLMClient._invoke_with_retry", new_callable=AsyncMock) as m:
        m.return_value = mock_response
        yield m


@pytest.fixture
def mock_llm_coldstart():
    """콜드스타트 추천 LLM mock."""
    mock_response = _make_mock_llm_response(MOCK_COLDSTART_JSON)
    with patch("llm.llm_client.LLMClient._invoke_with_retry", new_callable=AsyncMock) as m:
        m.return_value = mock_response
        yield m


@pytest.fixture
def mock_llm_reason():
    """이유 생성 LLM mock."""
    mock_response = _make_mock_llm_response(MOCK_REASON_JSON)
    with patch("llm.llm_client.LLMClient._invoke_with_retry", new_callable=AsyncMock) as m:
        m.return_value = mock_response
        yield m


@pytest.fixture
def mock_llm_error():
    """LLM 호출 오류 mock."""
    from llm.llm_client import LLMInvokeError
    with patch("llm.llm_client.LLMClient._invoke_with_retry", new_callable=AsyncMock) as m:
        m.side_effect = LLMInvokeError("LLM API 연결 오류 (mock)")
        yield m


@pytest.fixture
def mock_redis_empty():
    """Redis 캐시 미스 mock (모든 조회 None 반환)."""
    with patch("cache.cache_manager.CacheManager.get", new_callable=AsyncMock) as mock_get, \
         patch("cache.cache_manager.CacheManager.set", new_callable=AsyncMock) as mock_set, \
         patch("cache.cache_manager.CacheManager.is_connected", new_callable=AsyncMock) as mock_ping:
        mock_get.return_value = None
        mock_set.return_value = None
        mock_ping.return_value = True
        yield {"get": mock_get, "set": mock_set}


@pytest.fixture
def mock_redis_connected():
    """Redis 연결 상태 mock."""
    with patch("cache.cache_manager.CacheManager.is_connected", new_callable=AsyncMock) as m:
        m.return_value = True
        yield m


@pytest_asyncio.fixture
async def async_client():
    """비동기 테스트 클라이언트."""
    async with AsyncClient(
        transport=ASGITransport(app=app), base_url="http://test"
    ) as client:
        yield client


# ── 요청 픽스처 ──────────────────────────────────────────────────────────

@pytest.fixture
def normal_user_request() -> dict:
    return {
        "memberId": "550e8400-e29b-41d4-a716-446655440001",
        "latitude": 37.5665,
        "longitude": 126.9780,
        "requestedAt": "2026-02-26T12:00:00Z",
        "isColdStart": False,
        "feedbackCount": 12,
        "tasteVector": {
            "한식": 0.85,
            "일식": 0.70,
            "중식": 0.40,
            "양식": 0.55,
            "분식": 0.60,
            "샐러드/건강식": 0.65,
        },
        "onboardingSwipes": None,
        "allergenFilter": ["땅콩", "새우"],
        "dietType": "일반",
        "weather": {
            "condition": "RAINY",
            "temperatureCelsius": 8.5,
            "description": "비 오는 날",
        },
        "recentMealHistory": [
            {
                "restaurantId": "rest-007",
                "category": "양식",
                "mealDate": "2026-02-25",
                "satisfaction": "GOOD",
            }
        ],
        "excludeRestaurantIds": ["rest-005", "rest-006"],
        "jobCluster": None,
    }


@pytest.fixture
def coldstart_user_request() -> dict:
    return {
        "memberId": "550e8400-e29b-41d4-a716-446655440002",
        "latitude": 37.5665,
        "longitude": 126.9780,
        "requestedAt": "2026-02-26T12:00:00Z",
        "isColdStart": True,
        "feedbackCount": 2,
        "tasteVector": None,
        "onboardingSwipes": [
            {"cardId": "card-korean-001", "category": "한식", "liked": True},
            {"cardId": "card-japanese-001", "category": "일식", "liked": True},
            {"cardId": "card-fastfood-001", "category": "패스트푸드", "liked": False},
        ],
        "allergenFilter": [],
        "dietType": "일반",
        "weather": {
            "condition": "CLEAR",
            "temperatureCelsius": 15.0,
            "description": "맑은 날",
        },
        "recentMealHistory": [],
        "excludeRestaurantIds": [],
        "jobCluster": "IT_OFFICE_WORKER",
    }


@pytest.fixture
def reason_request() -> dict:
    return {
        "recommendationId": "rec-550e8400-e29b-41d4-a716-446655440010",
        "restaurantId": "rest-001",
        "restaurantName": "광화문 된장마을",
        "category": "한식",
        "representativeMenu": "된장찌개 정식",
        "memberId": "550e8400-e29b-41d4-a716-446655440001",
        "tasteVector": {"한식": 0.85, "일식": 0.70, "중식": 0.40},
        "weather": {
            "condition": "RAINY",
            "temperatureCelsius": 8.5,
            "description": "비 오는 날",
        },
        "recentMealHistory": [
            {
                "restaurantId": "rest-007",
                "category": "양식",
                "mealDate": "2026-02-25",
                "satisfaction": "GOOD",
            }
        ],
        "confidenceScore": 87,
    }
