"""
추천 이유 생성 서비스 오케스트레이터.

처리 순서:
  1. Redis 캐시 조회 (reason:{recommendationId}) → 히트 시 즉시 반환
  2. Circuit Breaker 상태 확인
     - Open → 기본 이유(폴백) 반환 (isReasonReady: false)
  3. ReasonPromptBuilder로 프롬프트 조립
  4. LLMClient 호출 (reason_model: Claude 3.5 Haiku)
  5. ReasonResponseParser로 파싱
  6. Redis 캐시 저장
  7. 응답 반환
"""

import logging
import time

from cache.cache_manager import CacheManager, get_cache_manager, get_cached_until_iso
from config import settings
from llm.llm_client import LLMClient, LLMInvokeError, get_llm_client
from model.ai_metadata import AiMetadata, TokenUsage
from model.common import AiSource, CBStateEnum
from model.reason_request import AiReasonRequest
from model.reason_response import AiReasonResponse, ParsedReason
from parser.reason_parser import ReasonParsingError, ReasonResponseParser
from prompt.reason_prompt import ReasonPromptBuilder
from service.mock_llm_engine import MockLLMEngine, get_mock_llm_engine

logger = logging.getLogger(__name__)

FALLBACK_REASON_MESSAGE = "추천 이유를 준비 중이에요."


class ReasonService:
    """추천 이유 생성 서비스 오케스트레이터."""

    def __init__(
        self,
        cache_manager: CacheManager | None = None,
        llm_client: LLMClient | None = None,
        reason_parser: ReasonResponseParser | None = None,
        prompt_builder: ReasonPromptBuilder | None = None,
        mock_engine: MockLLMEngine | None = None,
    ) -> None:
        self._cache = cache_manager or get_cache_manager()
        self._llm = llm_client or get_llm_client()
        self._parser = reason_parser or ReasonResponseParser()
        self._prompt_builder = prompt_builder or ReasonPromptBuilder()
        self._mock = mock_engine or get_mock_llm_engine()

    @property
    def _use_mock(self) -> bool:
        """런타임 probe 결과 기반으로 Mock 모드 여부를 동적으로 반환."""
        return not settings.is_llm_available

    async def generate_reason(self, request: AiReasonRequest) -> AiReasonResponse:
        """추천 이유 생성 메인 메서드."""
        start_time = time.time()
        recommendation_id = request.recommendation_id
        confidence_score = request.confidence_score or 70

        # Mock 모드: LLM API 키 없을 때 규칙 기반 이유 생성 (isReasonReady=True 유지)
        if self._use_mock:
            logger.info("Mock LLM 엔진 경로 (recommendation_id=%s)", recommendation_id)
            return self._mock.generate_reason(request)

        # 1. Redis 캐시 조회
        cached = await self._cache.get_reason(recommendation_id)
        if cached is not None:
            logger.info("이유 캐시 히트 (recommendation_id=%s)", recommendation_id)
            return self._build_from_cache(cached, recommendation_id)

        # 2. Circuit Breaker 상태 확인
        cb = self._llm.circuit_breaker
        if cb.is_open():
            logger.warning(
                "Circuit Breaker OPEN — 이유 폴백 반환 (recommendation_id=%s)",
                recommendation_id,
            )
            latency_ms = int((time.time() - start_time) * 1000)
            return self._build_fallback_response(
                recommendation_id, confidence_score, latency_ms, CBStateEnum.OPEN
            )

        # 3. LLM 호출
        try:
            return await self._generate_with_llm(request, start_time)
        except (LLMInvokeError, ReasonParsingError) as exc:
            logger.error(
                "이유 생성 LLM 실패, 폴백 반환 (recommendation_id=%s): %s",
                recommendation_id,
                exc,
            )
            latency_ms = int((time.time() - start_time) * 1000)
            cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)
            return self._build_fallback_response(
                recommendation_id, confidence_score, latency_ms, cb_state
            )

    async def _generate_with_llm(
        self, request: AiReasonRequest, start_time: float
    ) -> AiReasonResponse:
        """LLM으로 추천 이유 생성."""
        recommendation_id = request.recommendation_id
        confidence_score = request.confidence_score or 70

        system_prompt, user_prompt, _ = self._prompt_builder.build(request)

        raw_response, token_usage = await self._llm.generate_reason(system_prompt, user_prompt)

        parsed: ParsedReason = self._parser.parse(raw_response, confidence_score)
        latency_ms = int((time.time() - start_time) * 1000)

        # 캐시 저장
        cache_dict = {
            "recommendation_id": recommendation_id,
            "natural_language_reason": parsed.natural_language_reason,
            "confidence_score": parsed.confidence_score,
            "context_tags": parsed.context_tags,
            "is_reason_ready": True,
            "fallback_reason": None,
        }
        await self._cache.set_reason(recommendation_id, cache_dict)

        cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)
        try:
            model_used = str(self._llm._reason_model.model_name)
        except Exception:
            model_used = None

        metadata = AiMetadata(
            source=AiSource.LLM,
            model_used=model_used,
            latency_ms=latency_ms,
            token_usage=TokenUsage(**token_usage),
            circuit_breaker_state=cb_state,
        )

        return AiReasonResponse(
            recommendationId=recommendation_id,
            naturalLanguageReason=parsed.natural_language_reason,
            confidenceScore=parsed.confidence_score,
            contextTags=parsed.context_tags,
            isReasonReady=True,
            fallbackReason=None,
            cachedUntil=get_cached_until_iso(),
            metadata=metadata,
        )

    def _build_from_cache(self, cached: dict, recommendation_id: str) -> AiReasonResponse:
        """캐시 데이터로 응답 생성."""
        cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)
        metadata = AiMetadata(
            source=AiSource.CACHE,
            model_used=None,
            latency_ms=0,
            token_usage=TokenUsage(),
            circuit_breaker_state=cb_state,
        )
        return AiReasonResponse(
            recommendationId=recommendation_id,
            naturalLanguageReason=cached.get("natural_language_reason", FALLBACK_REASON_MESSAGE),
            confidenceScore=cached.get("confidence_score", 70),
            contextTags=cached.get("context_tags", []),
            isReasonReady=cached.get("is_reason_ready", True),
            fallbackReason=cached.get("fallback_reason"),
            cachedUntil=get_cached_until_iso(),
            metadata=metadata,
        )

    def _build_fallback_response(
        self,
        recommendation_id: str,
        confidence_score: int,
        latency_ms: int,
        cb_state: CBStateEnum,
    ) -> AiReasonResponse:
        """LLM 장애 시 기본 이유 반환 응답 생성."""
        metadata = AiMetadata(
            source=AiSource.FALLBACK_RULE_BASED,
            model_used=None,
            latency_ms=latency_ms,
            token_usage=TokenUsage(),
            circuit_breaker_state=cb_state,
        )
        return AiReasonResponse(
            recommendationId=recommendation_id,
            naturalLanguageReason="주변 인기 식당이에요",
            confidenceScore=confidence_score,
            contextTags=[],
            isReasonReady=False,
            fallbackReason=FALLBACK_REASON_MESSAGE,
            cachedUntil=None,
            metadata=metadata,
        )
