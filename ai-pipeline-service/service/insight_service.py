"""
인사이트 분석 메인 오케스트레이터.

처리 순서:
  1. Mock 모드 확인 → MockLLMEngine.generate_insights()
  2. LLM 호출 → InsightResponseParser 파싱
  3. 실패 시 폴백 인사이트 반환
"""

import logging
import time

from config import settings
from llm.llm_client import LLMClient, LLMInvokeError, get_llm_client
from model.ai_metadata import AiMetadata, TokenUsage
from model.common import AiSource, CBStateEnum
from model.insight_request import AiInsightRequest
from model.insight_response import AiInsightResponse, MealBalance, SatisfactionAnalysis
from parser.insight_parser import InsightParsingError, InsightResponseParser
from prompt.insight_prompt import InsightPromptBuilder
from service.mock_llm_engine import MockLLMEngine, get_mock_llm_engine

logger = logging.getLogger(__name__)


class InsightService:
    """AI 인사이트 분석 서비스 오케스트레이터."""

    def __init__(
        self,
        llm_client: LLMClient | None = None,
        insight_parser: InsightResponseParser | None = None,
        prompt_builder: InsightPromptBuilder | None = None,
        mock_engine: MockLLMEngine | None = None,
    ) -> None:
        self._llm = llm_client or get_llm_client()
        self._parser = insight_parser or InsightResponseParser()
        self._prompt_builder = prompt_builder or InsightPromptBuilder()
        self._mock = mock_engine or get_mock_llm_engine()

    @property
    def _use_mock(self) -> bool:
        return not settings.is_llm_available

    async def analyze(self, request: AiInsightRequest) -> AiInsightResponse:
        """인사이트 분석 메인 메서드."""
        start_time = time.time()

        # Mock 모드
        if self._use_mock:
            logger.info("Mock LLM 인사이트 경로 (member=%s)", request.member_id)
            return self._mock.generate_insights(request)

        # LLM 호출 경로
        try:
            return await self._generate_with_llm(request, start_time)
        except (LLMInvokeError, InsightParsingError) as exc:
            logger.error(
                "인사이트 LLM 호출/파싱 실패, 폴백 전환 (member=%s): %s",
                request.member_id,
                exc,
            )
            return self._build_fallback_response(request, start_time)

    async def _generate_with_llm(
        self, request: AiInsightRequest, start_time: float
    ) -> AiInsightResponse:
        """LLM 기반 인사이트 생성."""
        system_prompt, user_prompt, _ = self._prompt_builder.build(request)

        raw_response, token_usage = await self._llm.generate_reason(
            system_prompt, user_prompt
        )

        parsed = self._parser.parse(raw_response)
        latency_ms = int((time.time() - start_time) * 1000)

        cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)

        metadata = AiMetadata(
            source=AiSource.LLM,
            model_used=settings.reason_model_id,
            latency_ms=latency_ms,
            token_usage=TokenUsage(**token_usage),
            circuit_breaker_state=cb_state,
        )

        return AiInsightResponse(
            weeklySummary=parsed["weeklySummary"],
            mealBalance=MealBalance(
                diversityScore=parsed["mealBalance"]["diversityScore"],
                diagnosis=parsed["mealBalance"]["diagnosis"],
                coachingComment=parsed["mealBalance"]["coachingComment"],
            ),
            satisfactionAnalysis=SatisfactionAnalysis(
                satisfactionRate=parsed["satisfactionAnalysis"]["satisfactionRate"],
                patterns=parsed["satisfactionAnalysis"]["patterns"],
                patternComment=parsed["satisfactionAnalysis"]["patternComment"],
            ),
            metadata=metadata,
        )

    def _build_fallback_response(
        self, request: AiInsightRequest, start_time: float
    ) -> AiInsightResponse:
        """폴백 인사이트 생성 (규칙 기반)."""
        mock_result = self._mock.generate_insights(request)
        latency_ms = int((time.time() - start_time) * 1000)
        mock_result.metadata.latency_ms = latency_ms
        mock_result.metadata.source = AiSource.FALLBACK_RULE_BASED
        return mock_result
