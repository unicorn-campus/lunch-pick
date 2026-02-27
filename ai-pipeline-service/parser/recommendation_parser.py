"""
추천 생성 LLM 응답 파서.

LLM JSON 응답 파싱 + Pydantic 스키마 검증.
파싱 실패 시 ParsingError 발생 → 서비스 계층에서 폴백 처리.
"""

import json
import logging
import re

from pydantic import ValidationError

from model.recommendation_response import (
    LLMRecommendationOutput,
    LLMRecommendationResponse,
    RecommendedRestaurant,
)

logger = logging.getLogger(__name__)


class RecommendationParsingError(Exception):
    """추천 응답 파싱 실패."""


class RecommendationResponseParser:
    """LLM 추천 JSON 응답 파서."""

    def parse(self, raw_response: str) -> list[RecommendedRestaurant]:
        """LLM raw 응답을 파싱하여 RecommendedRestaurant 목록 반환.

        Args:
            raw_response: LLM이 반환한 원시 문자열

        Returns:
            파싱된 RecommendedRestaurant 목록 (1~3개)

        Raises:
            RecommendationParsingError: JSON 파싱 또는 스키마 검증 실패
        """
        cleaned = self._extract_json(raw_response)
        parsed_dict = self._parse_json(cleaned)
        validated = self._validate(parsed_dict)
        return self._to_response_models(validated.recommendations)

    def _extract_json(self, raw: str) -> str:
        """markdown 코드 블록 제거 후 JSON 문자열 추출."""
        text = raw.strip()
        # ```json ... ``` 또는 ``` ... ``` 블록 제거
        code_block = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
        if code_block:
            text = code_block.group(1).strip()
        return text

    def _parse_json(self, text: str) -> dict:
        """JSON 파싱. 실패 시 RecommendationParsingError 발생."""
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            logger.warning("JSON 파싱 실패: %s | raw=%s", exc, text[:200])
            raise RecommendationParsingError(f"JSON 파싱 실패: {exc}") from exc

    def _validate(self, data: dict) -> LLMRecommendationResponse:
        """Pydantic 스키마 검증."""
        try:
            return LLMRecommendationResponse(**data)
        except (ValidationError, TypeError) as exc:
            logger.warning("스키마 검증 실패: %s | data=%s", exc, str(data)[:200])
            raise RecommendationParsingError(f"스키마 검증 실패: {exc}") from exc

    def _to_response_models(
        self, items: list[LLMRecommendationOutput]
    ) -> list[RecommendedRestaurant]:
        """LLMRecommendationOutput → RecommendedRestaurant 변환.

        distanceMeters, estimatedWalkMinutes는 LLM 응답에 없으므로 None 처리.
        실제 값은 recommendation-service가 DB에서 조회 후 보강한다.
        """
        result = []
        for item in items:
            restaurant = RecommendedRestaurant(
                restaurantId=item.restaurant_id,
                restaurantName=item.restaurant_name,
                representativeMenu=item.representative_menu,
                category=item.category,
                reasonSummary=item.reason_summary,
                confidenceScore=item.confidence_score,
                distanceMeters=None,
                estimatedWalkMinutes=None,
            )
            result.append(restaurant)
        return result
