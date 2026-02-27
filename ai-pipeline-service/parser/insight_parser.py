"""인사이트 LLM 응답 파서."""

import json
import logging
import re

logger = logging.getLogger(__name__)


class InsightParsingError(Exception):
    """인사이트 응답 파싱 실패."""


class InsightResponseParser:
    """LLM 인사이트 JSON 응답 파서."""

    def parse(self, raw_response: str) -> dict:
        """LLM raw 응답을 파싱하여 인사이트 dict 반환.

        Returns:
            {
              "weeklySummary": str,
              "mealBalance": {"diversityScore": int, "diagnosis": str, "coachingComment": str},
              "satisfactionAnalysis": {"satisfactionRate": int, "patterns": list, "patternComment": str}
            }

        Raises:
            InsightParsingError: JSON 파싱 또는 필수 필드 누락
        """
        cleaned = self._extract_json(raw_response)
        parsed = self._parse_json(cleaned)
        self._validate_fields(parsed)
        return parsed

    def _extract_json(self, raw: str) -> str:
        """markdown 코드 블록 제거 후 JSON 문자열 추출."""
        text = raw.strip()
        code_block = re.search(r"```(?:json)?\s*([\s\S]*?)```", text)
        if code_block:
            text = code_block.group(1).strip()
        return text

    def _parse_json(self, text: str) -> dict:
        """JSON 파싱."""
        try:
            return json.loads(text)
        except json.JSONDecodeError as exc:
            logger.warning("인사이트 JSON 파싱 실패: %s | raw=%s", exc, text[:200])
            raise InsightParsingError(f"JSON 파싱 실패: {exc}") from exc

    def _validate_fields(self, data: dict) -> None:
        """필수 필드 존재 여부 검증 (관대한 파싱)."""
        if "weeklySummary" not in data:
            raise InsightParsingError("weeklySummary 필드 누락")

        balance = data.get("mealBalance")
        if not isinstance(balance, dict):
            raise InsightParsingError("mealBalance 필드 누락 또는 잘못된 형식")

        for field in ("diversityScore", "diagnosis", "coachingComment"):
            if field not in balance:
                raise InsightParsingError(f"mealBalance.{field} 필드 누락")

        analysis = data.get("satisfactionAnalysis")
        if not isinstance(analysis, dict):
            raise InsightParsingError("satisfactionAnalysis 필드 누락 또는 잘못된 형식")

        for field in ("satisfactionRate", "patterns", "patternComment"):
            if field not in analysis:
                raise InsightParsingError(f"satisfactionAnalysis.{field} 필드 누락")
