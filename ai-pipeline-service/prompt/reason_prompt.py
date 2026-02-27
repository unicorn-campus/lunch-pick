"""
추천 이유 생성 프롬프트 빌더.

reason-system-v1.0.txt 사용.
"""

import hashlib
import logging

from model.reason_request import AiReasonRequest
from prompt.loader import ACTIVE_REASON_TEMPLATE, load_system_prompt

logger = logging.getLogger(__name__)

WEATHER_DESCRIPTION_MAP = {
    "CLEAR": "맑은 날",
    "CLOUDY": "흐린 날",
    "RAINY": "비 오는 날",
    "SNOWY": "눈 오는 날",
    "WINDY": "바람 부는 날",
    "HOT": "더운 날",
    "COLD": "추운 날",
}


class ReasonPromptBuilder:
    """추천 이유 생성 프롬프트 빌더."""

    def build(self, request: AiReasonRequest) -> tuple[str, str, str]:
        """(system_prompt, user_prompt, prompt_hash) 반환."""
        system_prompt = load_system_prompt(ACTIVE_REASON_TEMPLATE)
        user_prompt = self._build_user_prompt(request)
        prompt_hash = hashlib.sha256(
            (system_prompt + user_prompt).encode("utf-8")
        ).hexdigest()[:12]
        return system_prompt, user_prompt, prompt_hash

    def _build_user_prompt(self, request: AiReasonRequest) -> str:
        # 취향 요약
        taste_summary = self._format_taste_summary(request.taste_vector)

        # 날씨
        weather_description = "알 수 없음"
        temperature_celsius = 20.0
        if request.weather:
            weather_description = request.weather.description or WEATHER_DESCRIPTION_MAP.get(
                request.weather.condition, request.weather.condition
            )
            temperature_celsius = request.weather.temperature_celsius

        # 최근 식사 이력
        recent_meal_history_text = self._format_meal_history(request.recent_meal_history)

        # 확신 스코어
        confidence_score = request.confidence_score or 70

        return (
            f"## 추천된 식당 정보\n"
            f"- 식당명: {request.restaurant_name}\n"
            f"- 카테고리: {request.category}\n"
            f"- 대표 메뉴: {request.representative_menu or '정보 없음'}\n\n"
            f"## 사용자 컨텍스트\n"
            f"- 취향: {taste_summary}\n"
            f"- 오늘 날씨: {weather_description} ({temperature_celsius}°C)\n"
            f"- 최근 식사 이력: {recent_meal_history_text}\n"
            f"- 확신 스코어: {confidence_score}\n\n"
            f"위 정보를 바탕으로 이 사용자에게 {request.restaurant_name}을 추천한 이유를 한 문장으로 설명해주세요.\n"
            f"반드시 JSON 형식으로만 응답하세요."
        )

    def _format_taste_summary(self, taste_vector: dict | None) -> str:
        if not taste_vector:
            return "취향 데이터 없음"
        # 상위 2개 카테고리만 추출
        sorted_items = sorted(taste_vector.items(), key=lambda x: x[1], reverse=True)[:2]
        parts = [f"{cat} 선호 {int(score * 100)}%" for cat, score in sorted_items]
        return ", ".join(parts)

    def _format_meal_history(self, history: list | None) -> str:
        if not history:
            return "이력 없음"
        lines = []
        for i, meal in enumerate(history[:2]):
            day_label = ["어제", "2일 전"][i] if i < 2 else f"{i+1}일 전"
            category = getattr(meal, "category", "알 수 없음")
            lines.append(f"{day_label} {category}")
        return ", ".join(lines)
