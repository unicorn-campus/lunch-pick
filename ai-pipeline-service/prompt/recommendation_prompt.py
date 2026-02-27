"""
추천 생성 프롬프트 빌더.

일반 추천: recommendation-system-v1.0.txt
콜드스타트: recommendation-coldstart-v1.0.txt
"""

import hashlib
import json
import logging
from datetime import datetime

import pytz

from model.recommendation_request import AiRecommendationRequest
from prompt.loader import (
    ACTIVE_COLDSTART_TEMPLATE,
    ACTIVE_RECOMMENDATION_TEMPLATE,
    load_system_prompt,
    render_user_prompt,
)

logger = logging.getLogger(__name__)

KST = pytz.timezone("Asia/Seoul")

WEEKDAY_KO = {
    0: "월요일",
    1: "화요일",
    2: "수요일",
    3: "목요일",
    4: "금요일",
    5: "토요일",
    6: "일요일",
}

WEATHER_CODE_MAP = {
    "CLEAR": "맑음",
    "CLOUDY": "흐림",
    "RAINY": "비",
    "SNOWY": "눈",
    "WINDY": "바람",
    "HOT": "더움",
    "COLD": "추움",
}

# 가상의 식당 목록 (실제 환경에서는 recommendation-service가 전달)
_DEFAULT_AVAILABLE_RESTAURANTS = [
    {
        "restaurant_id": "rest-001",
        "restaurant_name": "광화문 된장마을",
        "representative_menu": "된장찌개 정식",
        "category": "한식",
        "distance_meters": 320,
        "estimated_walk_minutes": 4,
        "allergens": [],
    },
    {
        "restaurant_id": "rest-002",
        "restaurant_name": "사쿠라 스시",
        "representative_menu": "런치 세트",
        "category": "일식",
        "distance_meters": 480,
        "estimated_walk_minutes": 6,
        "allergens": ["새우"],
    },
    {
        "restaurant_id": "rest-003",
        "restaurant_name": "그린 샐러드바",
        "representative_menu": "프레시 볼",
        "category": "샐러드/건강식",
        "distance_meters": 250,
        "estimated_walk_minutes": 3,
        "allergens": [],
    },
    {
        "restaurant_id": "rest-004",
        "restaurant_name": "종로 한식뷔페",
        "representative_menu": "한식 뷔페",
        "category": "한식",
        "distance_meters": 400,
        "estimated_walk_minutes": 5,
        "allergens": [],
    },
    {
        "restaurant_id": "rest-005",
        "restaurant_name": "이탈리아 파스타",
        "representative_menu": "까르보나라",
        "category": "양식",
        "distance_meters": 350,
        "estimated_walk_minutes": 4,
        "allergens": ["땅콩"],
    },
]


class RecommendationPromptBuilder:
    """일반 추천 프롬프트 빌더."""

    def build(self, request: AiRecommendationRequest) -> tuple[str, str, str]:
        """(system_prompt, user_prompt, prompt_hash) 반환."""
        system_prompt = load_system_prompt(ACTIVE_RECOMMENDATION_TEMPLATE)
        user_prompt = self._build_user_prompt(request)
        prompt_hash = hashlib.sha256(
            (system_prompt + user_prompt).encode("utf-8")
        ).hexdigest()[:12]
        return system_prompt, user_prompt, prompt_hash

    def _build_user_prompt(self, request: AiRecommendationRequest) -> str:
        """사용자 컨텍스트를 프롬프트 텍스트로 조립."""
        requested_at = request.requested_at
        if isinstance(requested_at, str):
            requested_at = datetime.fromisoformat(requested_at.replace("Z", "+00:00"))
        kst_time = requested_at.astimezone(KST)
        weekday_str = WEEKDAY_KO[kst_time.weekday()]
        current_time = kst_time.strftime("%H:%M")

        # 취향 벡터 텍스트 변환
        taste_vector_text = self._format_taste_vector(request.taste_vector)

        # 알레르기 필터
        allergen_filter = ", ".join(request.allergen_filter) if request.allergen_filter else "없음"

        # 식단 유형
        diet_type = request.diet_type or "일반"

        # 날씨
        weather_condition = "CLEAR"
        weather_description = "맑은 날"
        temperature_celsius = 20.0
        if request.weather:
            weather_condition = request.weather.condition
            weather_description = request.weather.description or WEATHER_CODE_MAP.get(
                request.weather.condition, request.weather.condition
            )
            temperature_celsius = request.weather.temperature_celsius

        # 최근 식사 이력 텍스트
        recent_meal_history_text = self._format_meal_history(request.recent_meal_history)

        # 제외 식당
        exclude_ids = ", ".join(request.exclude_restaurant_ids) if request.exclude_restaurant_ids else "없음"

        # 이용 가능한 식당 목록 (allergen 필터 적용)
        available = self._filter_restaurants(
            _DEFAULT_AVAILABLE_RESTAURANTS,
            request.allergen_filter or [],
            request.exclude_restaurant_ids or [],
        )
        available_restaurants_json = json.dumps(available, ensure_ascii=False, indent=2)

        return (
            f"## 사용자 취향 정보\n"
            f"- 취향 벡터: {taste_vector_text}\n"
            f"- 알레르기 필터 (절대 제외): {allergen_filter}\n"
            f"- 식단 유형: {diet_type}\n\n"
            f"## 오늘의 상황\n"
            f"- 요일: {weekday_str}\n"
            f"- 날씨: {weather_condition} ({weather_description}, {temperature_celsius}°C)\n"
            f"- 현재 시각: {current_time}\n\n"
            f"## 최근 식사 이력 (반복 방지)\n"
            f"{recent_meal_history_text}\n\n"
            f"## 제외할 식당 (최근 3일 내 방문)\n"
            f"{exclude_ids}\n\n"
            f"## 추천 가능한 식당 목록 (위치 반경 500m 이내)\n"
            f"{available_restaurants_json}\n\n"
            f"위 정보를 바탕으로 오늘 점심으로 가장 적합한 식당 3곳을 추천해주세요.\n"
            f"반드시 JSON 형식으로만 응답하세요."
        )

    def _format_taste_vector(self, taste_vector: dict | None) -> str:
        if not taste_vector:
            return "데이터 없음"
        parts = []
        for category, score in taste_vector.items():
            percentage = int(score * 100)
            parts.append(f"{category} 선호 {percentage}%")
        return ", ".join(parts)

    def _format_meal_history(self, history: list | None) -> str:
        if not history:
            return "이력 없음"
        lines = []
        for i, meal in enumerate(history[:3]):  # 최근 3건만
            day_label = ["어제", "2일 전", "3일 전"][i] if i < 3 else f"{i+1}일 전"
            satisfaction = ""
            if hasattr(meal, "satisfaction") and meal.satisfaction:
                sat_map = {"GOOD": "양호", "BAD": "별로", "NEUTRAL": "보통"}
                satisfaction = f"({sat_map.get(meal.satisfaction, meal.satisfaction)})"
            category = getattr(meal, "category", "알 수 없음")
            lines.append(f"{day_label}: {category}{satisfaction}")
        return "\n".join(lines)

    def _filter_restaurants(
        self,
        restaurants: list[dict],
        allergen_filter: list[str],
        exclude_ids: list[str],
    ) -> list[dict]:
        """알레르기 하드 필터 + 제외 식당 필터 적용."""
        result = []
        for r in restaurants:
            if r["restaurant_id"] in exclude_ids:
                continue
            allergens = r.get("allergens", [])
            if any(a in allergens for a in allergen_filter):
                continue
            result.append(r)
        return result


class ColdStartPromptBuilder:
    """콜드스타트 추천 프롬프트 빌더."""

    def build(self, request: AiRecommendationRequest) -> tuple[str, str, str]:
        """(system_prompt, user_prompt, prompt_hash) 반환."""
        system_prompt = load_system_prompt(ACTIVE_COLDSTART_TEMPLATE)
        user_prompt = self._build_user_prompt(request)
        prompt_hash = hashlib.sha256(
            (system_prompt + user_prompt).encode("utf-8")
        ).hexdigest()[:12]
        return system_prompt, user_prompt, prompt_hash

    def _build_user_prompt(self, request: AiRecommendationRequest) -> str:
        requested_at = request.requested_at
        if isinstance(requested_at, str):
            requested_at = datetime.fromisoformat(requested_at.replace("Z", "+00:00"))
        kst_time = requested_at.astimezone(KST)
        weekday_str = WEEKDAY_KO[kst_time.weekday()]

        # 온보딩 스와이프 텍스트
        onboarding_text = self._format_onboarding_swipes(request.onboarding_swipes)

        # 직군 클러스터
        job_cluster = request.job_cluster or "GENERAL_OFFICE"

        # 날씨
        weather_condition = "CLEAR"
        weather_description = "맑은 날"
        temperature_celsius = 20.0
        if request.weather:
            weather_condition = request.weather.condition
            weather_description = request.weather.description or weather_condition
            temperature_celsius = request.weather.temperature_celsius

        # 알레르기
        allergen_filter = ", ".join(request.allergen_filter) if request.allergen_filter else "없음"

        # 이용 가능한 식당 목록
        available = _DEFAULT_AVAILABLE_RESTAURANTS
        if request.allergen_filter:
            available = [
                r for r in available
                if not any(a in r.get("allergens", []) for a in request.allergen_filter)
            ]
        available_restaurants_json = json.dumps(available, ensure_ascii=False, indent=2)

        return (
            f"## 온보딩 카드 선택 결과\n"
            f"{onboarding_text}\n\n"
            f"## 직군 클러스터\n"
            f"{job_cluster}\n\n"
            f"## 오늘의 상황\n"
            f"- 날씨: {weather_condition} ({weather_description}, {temperature_celsius}°C)\n"
            f"- 요일: {weekday_str}\n\n"
            f"## 알레르기 필터 (절대 제외)\n"
            f"{allergen_filter}\n\n"
            f"## 추천 가능한 식당 목록 (위치 반경 500m 이내)\n"
            f"{available_restaurants_json}\n\n"
            f"위 온보딩 결과와 직군 정보를 바탕으로 이 사용자에게 적합한 식당 3곳을 추천해주세요.\n"
            f"확신 스코어는 40~65 범위로 설정하세요 (취향 데이터 부족으로 인한 불확실성 반영).\n"
            f"반드시 JSON 형식으로만 응답하세요."
        )

    def _format_onboarding_swipes(self, swipes: list | None) -> str:
        if not swipes:
            return "온보딩 데이터 없음"
        lines = []
        for swipe in swipes:
            liked = getattr(swipe, "liked", False)
            category = getattr(swipe, "category", "알 수 없음")
            reaction = "좋아요" if liked else "싫어요"
            lines.append(f"- {category}: {reaction}")
        return "\n".join(lines)
