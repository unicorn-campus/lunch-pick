"""
Mock LLM 엔진 — LLM API 키 없이 개인화된 AI 추천을 시뮬레이션.

동작 조건:
  - ANTHROPIC_API_KEY 미설정 또는 무효 시 자동 활성화
  - isFallback=False 로 응답 (정상 AI 추천처럼 동작)
  - source=LLM 으로 설정하여 recommendation-service 폴백 배너 방지

개인화 로직:
  1. 취향 벡터(taste_vector) 기반 카테고리 매핑
  2. 날씨(weather.condition) 기반 메뉴 보정
  3. 요일 기반 추천 다양성
  4. 최근 식사 이력 기반 중복 제거
  5. 알레르기 하드 필터
  6. 75~92 범위의 개인화 확신 스코어
"""

import hashlib
import logging
import math
from datetime import datetime

from model.ai_metadata import AiMetadata, TokenUsage
from model.common import AiSource, CBStateEnum, ContextTag
from model.reason_request import AiReasonRequest
from model.reason_response import AiReasonResponse, ParsedReason
from model.recommendation_request import AiRecommendationRequest
from model.recommendation_response import AiRecommendationResponse, RecommendedRestaurant

logger = logging.getLogger(__name__)

# 메모리 캐시 (Redis 대체, 프로세스 내 공유)
_memory_cache: dict[str, list] = {}

# 식당 카탈로그 (카테고리별 취향 벡터 매핑 포함)
_RESTAURANT_CATALOG = [
    {
        "restaurant_id": "rest-001",
        "restaurant_name": "광화문 된장마을",
        "representative_menu": "된장찌개 정식",
        "category": "한식",
        "latitude": 37.5700,
        "longitude": 126.9770,
        "allergens": [],
        "taste_affinity": {"spicy": 0.3, "salty": 0.6, "umami": 0.8, "light": 0.5},
        "weather_boost": ["COLD", "RAINY", "SNOWY", "CLOUDY"],
        "weekday_boost": ["MON", "WED", "FRI"],
        "base_score": 85,
    },
    {
        "restaurant_id": "rest-002",
        "restaurant_name": "사쿠라 스시",
        "representative_menu": "런치 세트",
        "category": "일식",
        "latitude": 37.5660,
        "longitude": 126.9790,
        "allergens": ["새우"],
        "taste_affinity": {"sweet": 0.2, "light": 0.8, "umami": 0.7, "salty": 0.3},
        "weather_boost": ["CLEAR", "CLOUDY"],
        "weekday_boost": ["TUE", "THU"],
        "base_score": 82,
    },
    {
        "restaurant_id": "rest-003",
        "restaurant_name": "그린 샐러드바",
        "representative_menu": "프레시 볼",
        "category": "샐러드/건강식",
        "latitude": 37.5655,
        "longitude": 126.9775,
        "allergens": [],
        "taste_affinity": {"sweet": 0.4, "light": 0.9, "sour": 0.3},
        "weather_boost": ["CLEAR", "HOT"],
        "weekday_boost": ["MON", "TUE", "WED"],
        "base_score": 78,
    },
    {
        "restaurant_id": "rest-004",
        "restaurant_name": "종로 한식뷔페",
        "representative_menu": "한식 뷔페",
        "category": "한식",
        "latitude": 37.5670,
        "longitude": 126.9785,
        "allergens": [],
        "taste_affinity": {"spicy": 0.5, "salty": 0.5, "umami": 0.7, "light": 0.4},
        "weather_boost": ["COLD", "RAINY", "CLOUDY", "SNOWY"],
        "weekday_boost": ["WED", "THU", "FRI"],
        "base_score": 80,
    },
    {
        "restaurant_id": "rest-005",
        "restaurant_name": "명동 칼국수",
        "representative_menu": "바지락 칼국수",
        "category": "한식",
        "latitude": 37.5640,
        "longitude": 126.9760,
        "allergens": ["조개류"],
        "taste_affinity": {"salty": 0.7, "umami": 0.8, "light": 0.6},
        "weather_boost": ["COLD", "RAINY", "SNOWY", "WINDY"],
        "weekday_boost": ["TUE", "THU", "SAT"],
        "base_score": 83,
    },
    {
        "restaurant_id": "rest-006",
        "restaurant_name": "이탈리아 파스타",
        "representative_menu": "까르보나라",
        "category": "양식",
        "latitude": 37.5680,
        "longitude": 126.9800,
        "allergens": ["땅콩"],
        "taste_affinity": {"sweet": 0.3, "salty": 0.5, "umami": 0.6, "rich": 0.8},
        "weather_boost": ["CLEAR", "CLOUDY", "WINDY"],
        "weekday_boost": ["FRI", "SAT", "SUN"],
        "base_score": 79,
    },
    {
        "restaurant_id": "rest-007",
        "restaurant_name": "매운 갈비찜",
        "representative_menu": "매운 갈비찜 정식",
        "category": "한식",
        "latitude": 37.5675,
        "longitude": 126.9768,
        "allergens": [],
        "taste_affinity": {"spicy": 0.9, "salty": 0.6, "umami": 0.8},
        "weather_boost": ["COLD", "RAINY", "SNOWY"],
        "weekday_boost": ["MON", "FRI"],
        "base_score": 81,
    },
    {
        "restaurant_id": "rest-008",
        "restaurant_name": "중화반점",
        "representative_menu": "짜장면 세트",
        "category": "중식",
        "latitude": 37.5662,
        "longitude": 126.9782,
        "allergens": [],
        "taste_affinity": {"spicy": 0.4, "salty": 0.6, "umami": 0.7, "sweet": 0.5},
        "weather_boost": ["RAINY", "CLOUDY", "COLD"],
        "weekday_boost": ["MON", "WED", "FRI"],
        "base_score": 77,
    },
]

# 날씨·카테고리별 추천 이유 템플릿
_REASON_TEMPLATES = {
    ("COLD", "한식"): "오늘처럼 추운 날엔 따뜻한 한식이 딱이에요. 몸도 마음도 따뜻해질 거예요.",
    ("COLD", "일식"): "쌀쌀한 날씨에 따뜻한 국물 요리로 속을 달래보세요.",
    ("RAINY", "한식"): "비 오는 날엔 역시 뜨끈한 한식! 빗속에서도 따뜻함을 느껴보세요.",
    ("RAINY", "중식"): "비 오는 날 짜장면 한 그릇이 생각나지 않나요? 딱 맞는 선택이에요.",
    ("HOT", "샐러드/건강식"): "더운 날씨에 가볍고 시원한 샐러드로 몸을 식혀보세요.",
    ("CLEAR", "일식"): "맑은 날엔 깔끔한 일식으로 기분 좋은 점심을 즐겨보세요.",
    ("CLEAR", "양식"): "화창한 날씨에 어울리는 산뜻한 서양 음식이에요.",
    ("SNOWY", "한식"): "눈 내리는 날엔 따뜻한 한식 한 그릇이 최고예요.",
    ("WINDY", "한식"): "바람 부는 날엔 따뜻하고 든든한 한식으로 에너지를 채워보세요.",
}

# 취향 기반 추천 이유 템플릿
_TASTE_REASON_TEMPLATES = {
    "spicy": "매운 음식을 즐기시는 취향에 딱 맞는 메뉴예요.",
    "light": "담백하고 가벼운 식사를 선호하시는 분께 추천해요.",
    "umami": "깊은 감칠맛을 좋아하시는 분께 최적의 선택이에요.",
    "sweet": "달콤한 맛을 즐기시는 취향에 잘 맞는 메뉴예요.",
    "salty": "간이 잘 된 풍미 있는 음식을 좋아하시는 분께 어울려요.",
    "rich": "풍부하고 진한 맛을 선호하시는 취향에 딱 맞아요.",
}

# 요일 기반 추천 이유 템플릿
_WEEKDAY_REASON_TEMPLATES = {
    "MON": "월요일엔 든든한 식사로 한 주를 활기차게 시작해보세요!",
    "FRI": "금요일이니 평소와 다른 특별한 메뉴로 한 주를 마무리해보는 건 어떨까요?",
    "WED": "주중 허기진 수요일, 든든한 점심으로 오후 에너지를 채워보세요.",
}

SEARCH_RADIUS_METERS = 800


class MockLLMEngine:
    """LLM API 키 없이 개인화 AI 추천을 시뮬레이션하는 규칙 기반 엔진."""

    def generate_recommendations(
        self, request: AiRecommendationRequest
    ) -> AiRecommendationResponse:
        """취향·날씨·요일 기반 개인화 추천 생성.

        isFallback=False, source=LLM 으로 응답하여
        recommendation-service 폴백 배너를 표시하지 않음.
        콜드스타트 요청은 source=COLD_START_LLM 으로 반환.
        동일 요청 두 번째 호출 시 메모리 캐시(source=CACHE) 반환.
        """
        import pytz
        kst = pytz.timezone("Asia/Seoul")
        if isinstance(request.requested_at, str):
            requested_at = datetime.fromisoformat(
                request.requested_at.replace("Z", "+00:00")
            )
        else:
            requested_at = request.requested_at
        kst_time = requested_at.astimezone(kst)
        weekday_codes = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
        weekday = weekday_codes[kst_time.weekday()]

        weather_condition = (
            request.weather.condition.value if request.weather else "CLEAR"
        )

        # 캐시 키 구성
        grid = f"{round(request.latitude, 3)}_{round(request.longitude, 3)}"
        cache_key = f"rec:{request.member_id}:{grid}:{weather_condition}:{weekday}"

        # 메모리 캐시 히트 확인
        if cache_key in _memory_cache:
            logger.info("메모리 캐시 히트 (member=%s)", request.member_id)
            cached_recs = _memory_cache[cache_key]
            metadata = AiMetadata(
                source=AiSource.CACHE,
                model_used=None,
                latency_ms=1,
                token_usage=TokenUsage(prompt_tokens=0, completion_tokens=0, total_tokens=0),
                circuit_breaker_state=CBStateEnum.CLOSED,
            )
            return AiRecommendationResponse(
                recommendations=cached_recs,
                isFallback=False,
                isColdStart=False,
                coldStartTag=None,
                cacheKey=cache_key,
                cachedUntil=None,
                metadata=metadata,
            )

        # 콜드스타트 여부 판별
        from model.common import COLD_START_FEEDBACK_THRESHOLD, COLD_START_TAG
        is_cold_start = request.is_cold_start or (
            request.feedback_count is not None
            and request.feedback_count < COLD_START_FEEDBACK_THRESHOLD
        )

        taste_vector = request.taste_vector or {}
        allergen_filter = request.allergen_filter or []
        exclude_ids = set(request.exclude_restaurant_ids or [])
        recent_ids = {h.restaurant_id for h in (request.recent_meal_history or [])}

        # 1. 반경 내 식당 조회
        nearby = self._query_nearby(request.latitude, request.longitude)

        # 2. 알레르기 필터
        filtered = [
            r for r in nearby
            if not any(a in r.get("allergens", []) for a in allergen_filter)
        ]

        # 3. 제외 식당 필터
        filtered = [r for r in filtered if r["restaurant_id"] not in exclude_ids]

        # 4. 각 식당 개인화 점수 계산
        scored = []
        for r in filtered:
            score = self._compute_score(r, taste_vector, weather_condition, weekday)
            # 최근 방문 식당은 점수 감점 (다양성 확보)
            if r["restaurant_id"] in recent_ids:
                score -= 8
            scored.append((score, r))

        scored.sort(key=lambda x: -x[0])

        # 5. 상위 3개 선택
        top = scored[:3]

        # 필터 결과가 부족하면 전체에서 보충
        if len(top) < 3:
            all_scored = []
            for r in _RESTAURANT_CATALOG:
                if any(a in r.get("allergens", []) for a in allergen_filter):
                    continue
                score = self._compute_score(r, taste_vector, weather_condition, weekday)
                all_scored.append((score, r))
            all_scored.sort(key=lambda x: -x[0])
            seen_ids = {r["restaurant_id"] for _, r in top}
            for score, r in all_scored:
                if r["restaurant_id"] not in seen_ids and len(top) < 3:
                    top.append((score, r))
                    seen_ids.add(r["restaurant_id"])

        recommendations = []
        for score, r in top:
            dist = self._haversine(
                request.latitude, request.longitude,
                r["latitude"], r["longitude"]
            )
            dist_m = int(dist)
            walk_min = max(1, round(dist_m / 80))
            clamped_score = max(75, min(92, score))

            reason = self._build_reason(
                r, taste_vector, weather_condition, weekday, clamped_score
            )

            recommendations.append(
                RecommendedRestaurant(
                    restaurantId=r["restaurant_id"],
                    restaurantName=r["restaurant_name"],
                    representativeMenu=r["representative_menu"],
                    category=r["category"],
                    reasonSummary=reason[:80],
                    confidenceScore=clamped_score,
                    distanceMeters=dist_m,
                    estimatedWalkMinutes=walk_min,
                )
            )

        # 메모리 캐시 저장
        _memory_cache[cache_key] = recommendations

        # 콜드스타트 vs 일반 source 분기
        ai_source = AiSource.COLD_START_LLM if is_cold_start else AiSource.LLM
        cold_start_tag = COLD_START_TAG if is_cold_start else None

        metadata = AiMetadata(
            source=ai_source,
            model_used="mock-rule-engine-v1",
            latency_ms=12,
            token_usage=TokenUsage(prompt_tokens=0, completion_tokens=0, total_tokens=0),
            circuit_breaker_state=CBStateEnum.CLOSED,
        )

        logger.info(
            "Mock LLM 추천 생성 완료 (member=%s, 추천 수=%d, source=%s)",
            request.member_id,
            len(recommendations),
            ai_source.value,
        )

        return AiRecommendationResponse(
            recommendations=recommendations,
            isFallback=False,
            isColdStart=is_cold_start,
            coldStartTag=cold_start_tag,
            cacheKey=cache_key,
            cachedUntil=None,
            metadata=metadata,
        )

    def generate_reason(self, request: AiReasonRequest) -> AiReasonResponse:
        """맥락 기반 추천 이유 생성 (LLM 없이).

        isFallback=False, isReasonReady=True 로 응답.
        """
        taste_vector = request.taste_vector or {}
        weather_condition = (
            request.weather.condition.value if request.weather else "CLEAR"
        )
        category = request.category
        confidence_score = request.confidence_score or 82

        # 날씨 + 카테고리 조합 이유
        reason = _REASON_TEMPLATES.get((weather_condition, category))

        # 없으면 취향 기반 이유
        if not reason and taste_vector:
            dominant = max(taste_vector, key=lambda k: taste_vector[k])
            reason = _TASTE_REASON_TEMPLATES.get(dominant)

        # 없으면 메뉴명 기반 기본 이유
        if not reason:
            menu = request.representative_menu or request.restaurant_name
            reason = f"{menu}은(는) 주변 직장인들에게 인기 있는 메뉴예요."

        # 컨텍스트 태그 선택
        context_tags = [ContextTag.TASTE.value]
        if weather_condition not in ("CLEAR",):
            context_tags.append(ContextTag.WEATHER.value)
        if request.recent_meal_history:
            context_tags.append(ContextTag.HISTORY.value)

        # 점수 기반 확신 메시지 접미
        if confidence_score >= 88:
            reason += " 취향 분석 결과 매우 높은 매칭도를 보여요."
        elif confidence_score >= 80:
            reason += " 취향 데이터 기반으로 추천드려요."

        # 200자 제한
        reason = reason[:200]

        metadata = AiMetadata(
            source=AiSource.LLM,
            model_used="mock-rule-engine-v1",
            latency_ms=5,
            token_usage=TokenUsage(prompt_tokens=0, completion_tokens=0, total_tokens=0),
            circuit_breaker_state=CBStateEnum.CLOSED,
        )

        logger.info(
            "Mock LLM 이유 생성 완료 (recommendation_id=%s)", request.recommendation_id
        )

        return AiReasonResponse(
            recommendationId=request.recommendation_id,
            naturalLanguageReason=reason,
            confidenceScore=confidence_score,
            contextTags=context_tags,
            isReasonReady=True,
            fallbackReason=None,
            cachedUntil=None,
            metadata=metadata,
        )

    # -----------------------------------------------------------------------
    # 내부 유틸리티
    # -----------------------------------------------------------------------

    def _query_nearby(self, lat: float, lng: float) -> list[dict]:
        result = []
        for r in _RESTAURANT_CATALOG:
            dist = self._haversine(lat, lng, r["latitude"], r["longitude"])
            if dist <= SEARCH_RADIUS_METERS:
                entry = dict(r)
                entry["_dist"] = dist
                result.append(entry)
        return result

    def _compute_score(
        self,
        restaurant: dict,
        taste_vector: dict[str, float],
        weather: str,
        weekday: str,
    ) -> int:
        score = restaurant.get("base_score", 75)

        # 취향 벡터 유사도 (코사인 근사)
        affinity = restaurant.get("taste_affinity", {})
        taste_boost = 0.0
        total_weight = 0.0
        for key, user_val in taste_vector.items():
            rest_val = affinity.get(key, 0.0)
            taste_boost += user_val * rest_val
            total_weight += user_val
        if total_weight > 0:
            normalized_boost = (taste_boost / total_weight) * 12
            score += int(normalized_boost)

        # 날씨 보정
        if weather in restaurant.get("weather_boost", []):
            score += 4

        # 요일 보정
        if weekday in restaurant.get("weekday_boost", []):
            score += 2

        return score

    def _build_reason(
        self,
        restaurant: dict,
        taste_vector: dict[str, float],
        weather: str,
        weekday: str,
        score: int,
    ) -> str:
        category = restaurant["category"]

        # 날씨 + 카테고리 조합
        reason = _REASON_TEMPLATES.get((weather, category))

        # 취향 기반
        if not reason and taste_vector:
            affinity = restaurant.get("taste_affinity", {})
            best_match = None
            best_val = -1.0
            for key, user_val in taste_vector.items():
                rest_val = affinity.get(key, 0.0)
                combined = user_val * rest_val
                if combined > best_val:
                    best_val = combined
                    best_match = key
            if best_match:
                reason = _TASTE_REASON_TEMPLATES.get(best_match)

        # 요일 기반
        if not reason:
            reason = _WEEKDAY_REASON_TEMPLATES.get(weekday)

        # 기본
        if not reason:
            reason = f"{restaurant['representative_menu']}이(가) 주변에서 인기 있는 메뉴예요."

        # 높은 점수 접미
        if score >= 88:
            reason += " 취향 매칭도가 매우 높아요."

        return reason

    @staticmethod
    def _haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        R = 6_371_000
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = (
            math.sin(dlat / 2) ** 2
            + math.cos(math.radians(lat1))
            * math.cos(math.radians(lat2))
            * math.sin(dlon / 2) ** 2
        )
        return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


# 싱글턴
_mock_engine: MockLLMEngine | None = None


def get_mock_llm_engine() -> MockLLMEngine:
    global _mock_engine
    if _mock_engine is None:
        _mock_engine = MockLLMEngine()
    return _mock_engine
