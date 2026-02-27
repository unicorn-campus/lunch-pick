"""인사이트 분석 프롬프트 빌더."""

import hashlib
import logging

from model.insight_request import AiInsightRequest
from prompt.loader import ACTIVE_INSIGHT_TEMPLATE, load_system_prompt

logger = logging.getLogger(__name__)


class InsightPromptBuilder:
    """AI 인사이트 분석 프롬프트 빌더."""

    def build(self, request: AiInsightRequest) -> tuple[str, str, str]:
        """(system_prompt, user_prompt, prompt_hash) 반환."""
        system_prompt = load_system_prompt(ACTIVE_INSIGHT_TEMPLATE)
        user_prompt = self._build_user_prompt(request)
        prompt_hash = hashlib.sha256(
            (system_prompt + user_prompt).encode("utf-8")
        ).hexdigest()[:12]
        return system_prompt, user_prompt, prompt_hash

    def _build_user_prompt(self, request: AiInsightRequest) -> str:
        """사용자 컨텍스트를 프롬프트 텍스트로 조립."""
        # 카테고리 분포 텍스트
        dist_lines = []
        for cat, ratio in sorted(
            request.category_distribution.items(), key=lambda x: -x[1]
        ):
            dist_lines.append(f"- {cat}: {ratio * 100:.1f}%")
        category_text = "\n".join(dist_lines) if dist_lines else "- 데이터 없음"

        # 최근 7일 상세 기록
        recent_7d = request.recent_meals[:7]
        meal_lines = []
        for meal in recent_7d:
            sat_str = f" (만족도: {meal.satisfaction})" if meal.satisfaction else ""
            kw_str = f" [키워드: {meal.keyword}]" if meal.keyword else ""
            meal_lines.append(
                f"- {meal.date} | {meal.restaurant_name} | {meal.category} | {meal.menu_name}{sat_str}{kw_str}"
            )
        meals_text = "\n".join(meal_lines) if meal_lines else "- 기록 없음"

        # 피드백 통계
        good_count = sum(1 for m in request.recent_meals if m.satisfaction == "GOOD")
        bad_count = sum(1 for m in request.recent_meals if m.satisfaction == "BAD")
        neutral_count = sum(
            1 for m in request.recent_meals if m.satisfaction == "NEUTRAL"
        )
        taste_count = sum(1 for m in request.recent_meals if m.keyword == "TASTE")
        price_count = sum(1 for m in request.recent_meals if m.keyword == "PRICE")
        kindness_count = sum(
            1 for m in request.recent_meals if m.keyword == "KINDNESS"
        )

        return (
            f"## 회원 식사 기록 요약\n"
            f"- 분석 기간: 최근 {request.period_days}일\n"
            f"- 총 식사 수: {request.total_meal_count}끼\n\n"
            f"## 카테고리 분포\n"
            f"{category_text}\n\n"
            f"## 최근 7일 상세 기록\n"
            f"{meals_text}\n\n"
            f"## 피드백 통계\n"
            f"- GOOD: {good_count}건, BAD: {bad_count}건, NEUTRAL: {neutral_count}건\n"
            f"- 만족 키워드: TASTE {taste_count}, PRICE {price_count}, KINDNESS {kindness_count}\n\n"
            f"위 데이터를 분석하여 JSON 형식으로 응답해주세요."
        )
