"""
추천 이유 생성 LLM 응답 파서.

LLM JSON 응답 파싱 + Pydantic 스키마 검증.
파싱 실패 시 ReasonParsingError 발생 → 서비스 계층에서 폴백 처리.
"""

import json
import logging
import re

from pydantic import ValidationError

from model.reason_response import ParsedReason

logger = logging.getLogger(__name__)

VALID_CONTEXT_TAGS = {"날씨", "이력", "취향", "요일", "시간"}


class ReasonParsingError(Exception):
    """이유 응답 파싱 실패."""


class ReasonResponseParser:
    """LLM 추천 이유 JSON 응답 파서."""

    def parse(self, raw_response: str, confidence_score: int = 70) -> ParsedReason:
        """LLM raw 응답을 파싱하여 ParsedReason 반환.

        Args:
            raw_response: LLM이 반환한 원시 문자열
            confidence_score: 요청에서 전달된 확신 스코어 (LLM 응답값 검증용)

        Returns:
            ParsedReason

        Raises:
            ReasonParsingError: JSON 파싱 또는 스키마 검증 실패
        """
        cleaned = self._extract_json(raw_response)
        parsed_dict = self._parse_json(cleaned)
        validated = self._validate(parsed_dict)
        # confidence_score는 요청에서 전달된 값을 우선 사용
        if confidence_score is not None:
            validated = ParsedReason(
                natural_language_reason=validated.natural_language_reason,
                confidence_score=confidence_score,
                context_tags=validated.context_tags,
            )
        return validated

    def _extract_json(self, raw: str) -> str:
        """markdown 코드 블록 제거 후 JSON 문자열 추출."""
        text = raw.strip()
        code_block = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
        if code_block:
            text = code_block.group(1).strip()
        return text

    def _parse_json(self, text: str) -> dict:
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            logger.warning("이유 JSON 파싱 실패: %s | raw=%s", exc, text[:200])
            raise ReasonParsingError(f"JSON 파싱 실패: {exc}") from exc

    def _validate(self, data: dict) -> ParsedReason:
        try:
            parsed = ParsedReason(**data)
            # context_tags 유효값 필터링
            parsed = ParsedReason(
                natural_language_reason=parsed.natural_language_reason,
                confidence_score=parsed.confidence_score,
                context_tags=[
                    tag for tag in parsed.context_tags if tag in VALID_CONTEXT_TAGS
                ],
            )
            return parsed
        except (ValidationError, TypeError) as exc:
            logger.warning("이유 스키마 검증 실패: %s | data=%s", exc, str(data)[:200])
            raise ReasonParsingError(f"스키마 검증 실패: {exc}") from exc
