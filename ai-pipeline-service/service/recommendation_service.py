"""
추천 생성 메인 오케스트레이터.

처리 순서:
  1. Redis 캐시 조회 → 히트 시 즉시 반환 (source: CACHE)
  2. Circuit Breaker 상태 확인
     - Open → Stale 캐시 조회 → 없으면 규칙 기반 폴백
     - Closed → 콜드스타트 분기 (isColdStart 플래그)
  3. PromptBuilder로 프롬프트 조립
  4. LLMClient 호출 (Retry 포함)
  5. ResponseParser로 파싱 + Pydantic 검증
  6. Redis 캐시 저장
  7. 응답 반환
"""

import logging
import time

from cache.cache_manager import (
    CacheManager,
    calculate_location_grid,
    get_cached_until_iso,
    get_cache_manager,
)
from config import settings
from llm.circuit_breaker import CBState
from llm.llm_client import LLMClient, LLMInvokeError, get_llm_client
from model.ai_metadata import AiMetadata, TokenUsage
from model.common import (
    COLD_START_FEEDBACK_THRESHOLD,
    COLD_START_TAG,
    AiSource,
    CBStateEnum,
)
from model.recommendation_request import AiRecommendationRequest
from model.recommendation_response import AiRecommendationResponse, RecommendedRestaurant
from parser.recommendation_parser import RecommendationParsingError, RecommendationResponseParser
from prompt.recommendation_prompt import ColdStartPromptBuilder, RecommendationPromptBuilder
from service.fallback_engine import FallbackEngine
from service.mock_llm_engine import MockLLMEngine, get_mock_llm_engine

logger = logging.getLogger(__name__)

WEEKDAY_CODE = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]


class RecommendationService:
    """AI 추천 생성 서비스 오케스트레이터."""

    def __init__(
        self,
        cache_manager: CacheManager | None = None,
        llm_client: LLMClient | None = None,
        fallback_engine: FallbackEngine | None = None,
        recommendation_parser: RecommendationResponseParser | None = None,
        prompt_builder: RecommendationPromptBuilder | None = None,
        coldstart_prompt_builder: ColdStartPromptBuilder | None = None,
        mock_engine: MockLLMEngine | None = None,
    ) -> None:
        self._cache = cache_manager or get_cache_manager()
        self._llm = llm_client or get_llm_client()
        self._fallback = fallback_engine or FallbackEngine()
        self._parser = recommendation_parser or RecommendationResponseParser()
        self._prompt_builder = prompt_builder or RecommendationPromptBuilder()
        self._coldstart_prompt_builder = coldstart_prompt_builder or ColdStartPromptBuilder()
        self._mock = mock_engine or get_mock_llm_engine()

    @property
    def _use_mock(self) -> bool:
        """런타임 probe 결과 기반으로 Mock 모드 여부를 동적으로 반환."""
        return not settings.is_llm_available

    async def generate(self, request: AiRecommendationRequest) -> AiRecommendationResponse:
        """추천 생성 메인 메서드."""
        start_time = time.time()

        # Mock 모드: LLM API 키 없을 때 개인화 규칙 엔진으로 처리 (isFallback=False 유지)
        if self._use_mock:
            logger.info("Mock LLM 엔진 경로 (member=%s)", request.member_id)
            return self._mock.generate_recommendations(request)

        # 캐시 키 구성 요소
        location_grid = calculate_location_grid(request.latitude, request.longitude)
        weather_code = request.weather.condition.value if request.weather else "CLEAR"
        weekday_code = self._get_weekday_code(request.requested_at)
        member_id = request.member_id

        # 1. Redis 캐시 조회 (skipCache=true 시 건너뜀)
        if not request.skip_cache:
            cached = await self._cache.get_recommendation(
                member_id, location_grid, weather_code, weekday_code
            )
            if cached is not None:
                logger.info("캐시 히트 (member=%s)", member_id)
                return self._build_response_from_cache(
                    cached, member_id, location_grid, weather_code, weekday_code
                )
        else:
            logger.info("캐시 스킵 요청 (member=%s)", member_id)

        # 2. Circuit Breaker 상태 확인
        cb = self._llm.circuit_breaker
        if cb.is_open():
            logger.warning("Circuit Breaker OPEN — 폴백 처리 (member=%s)", member_id)
            return await self._handle_open_circuit(
                request, member_id, location_grid, weather_code, weekday_code, start_time
            )

        # 3. LLM 호출 경로 (Closed / Half-Open)
        return await self._handle_llm_path(
            request, member_id, location_grid, weather_code, weekday_code, start_time
        )

    async def _handle_llm_path(
        self,
        request: AiRecommendationRequest,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
        start_time: float,
    ) -> AiRecommendationResponse:
        """LLM 정상 경로 처리 (콜드스타트 분기 포함)."""
        is_cold_start = request.is_cold_start or (
            request.feedback_count is not None
            and request.feedback_count < COLD_START_FEEDBACK_THRESHOLD
        )

        try:
            if is_cold_start:
                return await self._generate_coldstart(
                    request, member_id, location_grid, weather_code, weekday_code, start_time
                )
            else:
                return await self._generate_normal(
                    request, member_id, location_grid, weather_code, weekday_code, start_time
                )
        except (LLMInvokeError, RecommendationParsingError) as exc:
            logger.error("LLM 호출/파싱 실패, 규칙 기반 폴백 전환 (member=%s): %s", member_id, exc)
            return self._build_fallback_response(
                request, member_id, location_grid, weather_code, weekday_code,
                start_time, source=AiSource.FALLBACK_RULE_BASED,
                cb_state=CBStateEnum(self._llm.circuit_breaker.state.value),
            )

    async def _generate_normal(
        self,
        request: AiRecommendationRequest,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
        start_time: float,
    ) -> AiRecommendationResponse:
        """일반 사용자 추천 생성."""
        system_prompt, user_prompt, prompt_hash = self._prompt_builder.build(request)

        # 프롬프트 해시 캐시 확인 (동일 컨텍스트 중복 호출 방지, skipCache 시 건너뜀)
        if not request.skip_cache:
            hash_cached = await self._cache.get_prompt_hash(prompt_hash)
            if hash_cached is not None:
                logger.info("프롬프트 해시 캐시 히트 (hash=%s)", prompt_hash)
                return self._build_response_from_cache(
                    hash_cached, member_id, location_grid, weather_code, weekday_code
                )

        raw_response, token_usage = await self._llm.generate_recommendation(
            system_prompt, user_prompt
        )

        recommendations = self._parser.parse(raw_response)
        latency_ms = int((time.time() - start_time) * 1000)

        cache_key = await self._cache.set_recommendation(
            member_id, location_grid, weather_code, weekday_code,
            self._serialize_recommendations(recommendations),
        )

        response_dict = self._serialize_recommendations(recommendations)
        await self._cache.set_prompt_hash(prompt_hash, response_dict)

        cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)
        try:
            model_name = str(self._llm._primary_model.model_name)
        except Exception:
            model_name = None

        metadata = AiMetadata(
            source=AiSource.LLM,
            model_used=model_name,
            latency_ms=latency_ms,
            token_usage=TokenUsage(**token_usage),
            circuit_breaker_state=cb_state,
        )

        return AiRecommendationResponse(
            recommendations=recommendations,
            isFallback=False,
            isColdStart=False,
            coldStartTag=None,
            cacheKey=cache_key,
            cachedUntil=get_cached_until_iso(),
            metadata=metadata,
        )

    async def _generate_coldstart(
        self,
        request: AiRecommendationRequest,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
        start_time: float,
    ) -> AiRecommendationResponse:
        """콜드스타트 사용자 추천 생성 (Claude 3.5 Sonnet)."""
        system_prompt, user_prompt, prompt_hash = self._coldstart_prompt_builder.build(request)

        raw_response, token_usage = await self._llm.generate_coldstart_recommendation(
            system_prompt, user_prompt
        )

        recommendations = self._parser.parse(raw_response)
        latency_ms = int((time.time() - start_time) * 1000)

        cache_key = await self._cache.set_recommendation(
            member_id, location_grid, weather_code, weekday_code,
            self._serialize_recommendations(recommendations),
        )

        cb_state = CBStateEnum(self._llm.circuit_breaker.state.value)
        try:
            coldstart_model_name = str(self._llm._coldstart_model.model_name)
        except Exception:
            coldstart_model_name = None

        metadata = AiMetadata(
            source=AiSource.COLD_START_LLM,
            model_used=coldstart_model_name,
            latency_ms=latency_ms,
            token_usage=TokenUsage(**token_usage),
            circuit_breaker_state=cb_state,
        )

        return AiRecommendationResponse(
            recommendations=recommendations,
            isFallback=False,
            isColdStart=True,
            coldStartTag=COLD_START_TAG,
            cacheKey=cache_key,
            cachedUntil=get_cached_until_iso(),
            metadata=metadata,
        )

    async def _handle_open_circuit(
        self,
        request: AiRecommendationRequest,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
        start_time: float,
    ) -> AiRecommendationResponse:
        """Circuit Breaker Open 상태 처리.

        Stale 캐시 존재 → STALE_CACHE 반환
        없으면 → FALLBACK_RULE_BASED 반환
        """
        cache_key = f"rec:{member_id}:{location_grid}:{weather_code}:{weekday_code}"
        latency_ms = int((time.time() - start_time) * 1000)

        # Stale 캐시 조회
        stale = await self._cache.get_stale_recommendation(
            member_id, location_grid, weather_code, weekday_code
        )
        if stale is not None:
            logger.info("Stale 캐시 반환 (member=%s)", member_id)
            recommendations = self._deserialize_recommendations(stale)
            metadata = AiMetadata(
                source=AiSource.STALE_CACHE,
                model_used=None,
                latency_ms=latency_ms,
                token_usage=TokenUsage(),
                circuit_breaker_state=CBStateEnum.OPEN,
            )
            return AiRecommendationResponse(
                recommendations=recommendations,
                isFallback=True,
                isColdStart=False,
                coldStartTag=None,
                cacheKey=cache_key,
                cachedUntil=None,
                metadata=metadata,
            )

        # 규칙 기반 폴백
        return self._build_fallback_response(
            request, member_id, location_grid, weather_code, weekday_code,
            start_time, source=AiSource.FALLBACK_RULE_BASED, cb_state=CBStateEnum.OPEN,
        )

    def _build_fallback_response(
        self,
        request: AiRecommendationRequest,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
        start_time: float,
        source: AiSource,
        cb_state: CBStateEnum,
    ) -> AiRecommendationResponse:
        """규칙 기반 폴백 추천 응답 생성."""
        fallback_recs = self._fallback.get_fallback_recommendations(
            latitude=request.latitude,
            longitude=request.longitude,
            allergen_filter=request.allergen_filter or [],
            exclude_restaurant_ids=request.exclude_restaurant_ids or [],
        )
        latency_ms = int((time.time() - start_time) * 1000)
        cache_key = f"rec:{member_id}:{location_grid}:{weather_code}:{weekday_code}"

        metadata = AiMetadata(
            source=source,
            model_used=None,
            latency_ms=latency_ms,
            token_usage=TokenUsage(),
            circuit_breaker_state=cb_state,
        )
        return AiRecommendationResponse(
            recommendations=fallback_recs,
            isFallback=True,
            isColdStart=False,
            coldStartTag=None,
            cacheKey=cache_key,
            cachedUntil=None,
            metadata=metadata,
        )

    def _build_response_from_cache(
        self,
        cached: dict | list,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday_code: str,
    ) -> AiRecommendationResponse:
        """캐시 데이터로 응답 생성."""
        cache_key = f"rec:{member_id}:{location_grid}:{weather_code}:{weekday_code}"
        recommendations = self._deserialize_recommendations(cached)
        metadata = AiMetadata(
            source=AiSource.CACHE,
            model_used=None,
            latency_ms=0,
            token_usage=TokenUsage(),
            circuit_breaker_state=CBStateEnum(self._llm.circuit_breaker.state.value),
        )
        return AiRecommendationResponse(
            recommendations=recommendations,
            isFallback=False,
            isColdStart=False,
            coldStartTag=None,
            cacheKey=cache_key,
            cachedUntil=get_cached_until_iso(),
            metadata=metadata,
        )

    def _serialize_recommendations(self, recommendations: list[RecommendedRestaurant]) -> list:
        """RecommendedRestaurant 목록 → dict 목록 (캐시 저장용)."""
        return [r.model_dump(by_alias=True) for r in recommendations]

    def _deserialize_recommendations(self, data: list | dict) -> list[RecommendedRestaurant]:
        """캐시 데이터 → RecommendedRestaurant 목록."""
        if isinstance(data, dict) and "recommendations" in data:
            items = data["recommendations"]
        elif isinstance(data, list):
            items = data
        else:
            items = [data]
        result = []
        for item in items:
            try:
                result.append(RecommendedRestaurant(**item))
            except Exception as exc:
                logger.warning("캐시 역직렬화 실패 (item=%s): %s", item, exc)
        return result

    def _get_weekday_code(self, requested_at) -> str:
        """요청 시각에서 요일 코드 추출 (KST 기준)."""
        import pytz
        from datetime import datetime
        KST = pytz.timezone("Asia/Seoul")
        if isinstance(requested_at, str):
            requested_at = datetime.fromisoformat(requested_at.replace("Z", "+00:00"))
        kst_time = requested_at.astimezone(KST)
        return WEEKDAY_CODE[kst_time.weekday()]
