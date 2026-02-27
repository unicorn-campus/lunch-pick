"""
LangChain init_chat_model 기반 LLM 클라이언트.

- primary_model: Claude 3.5 Haiku (일반 추천)
- coldstart_model: Claude 3.5 Sonnet (콜드스타트 추천)
- reason_model: Claude 3.5 Haiku (이유 생성)
- fallback_model: Claude 3.5 Sonnet (Haiku 장애 시 자동 전환)

Retry 전략:
  - 재시도 대상: 503, 408, 일시적 429
  - 최대 2회 재시도 (총 3회 시도)
  - 지수 백오프: 500ms → 1초 → 2초 + 랜덤 지터(0~100ms)

Circuit Breaker:
  - 연속 5회 실패 → Open (60초 대기)
  - 장애 시 fallback_model(Sonnet)로 자동 전환
"""

import asyncio
import logging
import random
from typing import Any

from langchain.chat_models import init_chat_model
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from config import settings
from llm.circuit_breaker import CircuitBreaker

logger = logging.getLogger(__name__)

# Retry 설정
MAX_RETRIES = 2          # 최대 재시도 횟수 (총 3회 시도)
BASE_DELAY_MS = 500      # 초기 대기 시간 (ms)
RETRYABLE_STATUS_CODES = {503, 408, 429}


class LLMInvokeError(Exception):
    """LLM 호출 실패 (재시도 불가)."""


class RetryableError(Exception):
    """LLM 호출 실패 (재시도 가능)."""


class LLMClient:
    """LangChain 기반 다중 LLM 모델 클라이언트."""

    def __init__(self) -> None:
        self._circuit_breaker = CircuitBreaker(
            failure_threshold=settings.cb_failure_threshold,
            recovery_timeout=settings.cb_recovery_timeout,
        )

        # 기본 모델: Claude 3.5 Haiku (일반 추천·이유 생성)
        self._primary_model = init_chat_model(
            model=settings.primary_model_id,
            model_provider=settings.primary_model_provider,
            temperature=settings.primary_model_temperature,
            max_tokens=settings.primary_model_max_tokens,
        )

        # 콜드스타트 모델: Claude 3.5 Sonnet
        self._coldstart_model = init_chat_model(
            model=settings.coldstart_model_id,
            model_provider=settings.coldstart_model_provider,
            temperature=settings.coldstart_model_temperature,
            max_tokens=settings.coldstart_model_max_tokens,
        )

        # 이유 생성 모델: Claude 3.5 Haiku (max_tokens 절감)
        self._reason_model = init_chat_model(
            model=settings.reason_model_id,
            model_provider=settings.reason_model_provider,
            temperature=settings.reason_model_temperature,
            max_tokens=settings.reason_model_max_tokens,
        )

        # Fallback 모델: Claude 3.5 Sonnet (Haiku 장애 시 자동 전환)
        self._fallback_model = init_chat_model(
            model=settings.coldstart_model_id,
            model_provider=settings.coldstart_model_provider,
            temperature=settings.primary_model_temperature,
            max_tokens=settings.primary_model_max_tokens,
        )

    @property
    def circuit_breaker(self) -> CircuitBreaker:
        return self._circuit_breaker

    def _build_messages(self, system_prompt: str, user_prompt: str) -> list:
        return [
            SystemMessage(content=system_prompt),
            HumanMessage(content=user_prompt),
        ]

    def _exponential_backoff(self, attempt: int) -> float:
        """500ms → 1초 → 2초 + 랜덤 지터(0~100ms). 초 단위 반환."""
        base = BASE_DELAY_MS * (2 ** attempt) / 1000
        jitter = random.uniform(0, 0.1)
        return base + jitter

    def _is_retryable(self, exc: Exception) -> bool:
        """재시도 가능한 예외 여부 판별."""
        exc_str = str(exc).lower()
        # HTTP 상태 코드 기반 판별
        for code in RETRYABLE_STATUS_CODES:
            if str(code) in exc_str:
                return True
        # 타임아웃·연결 오류는 재시도
        if any(
            keyword in exc_str
            for keyword in ["timeout", "connection", "rate_limit", "overloaded", "503", "408"]
        ):
            return True
        # 401 인증 실패는 재시도 불가
        if "401" in exc_str or "authentication" in exc_str or "api_key" in exc_str:
            return False
        return False

    async def _invoke_with_retry(
        self, model: Any, messages: list, model_name: str
    ) -> AIMessage:
        """지수 백오프 재시도 포함 LLM 호출."""
        last_exc: Exception | None = None

        for attempt in range(MAX_RETRIES + 1):
            try:
                response = await model.ainvoke(messages)
                return response
            except Exception as exc:
                last_exc = exc
                if attempt < MAX_RETRIES and self._is_retryable(exc):
                    delay = self._exponential_backoff(attempt)
                    logger.warning(
                        "LLM 호출 실패 (모델=%s, 시도=%d/%d, 대기=%.2fs): %s",
                        model_name, attempt + 1, MAX_RETRIES + 1, delay, exc,
                    )
                    await asyncio.sleep(delay)
                else:
                    logger.error(
                        "LLM 호출 최종 실패 (모델=%s, 시도=%d/%d): %s",
                        model_name, attempt + 1, MAX_RETRIES + 1, exc,
                    )
                    break

        raise LLMInvokeError(f"LLM 호출 실패: {last_exc}") from last_exc

    async def generate_recommendation(
        self, system_prompt: str, user_prompt: str
    ) -> tuple[str, dict]:
        """일반 추천 생성. (raw_content, token_usage) 튜플 반환.

        Circuit Breaker Open 시 LLMInvokeError 발생.
        Haiku 실패 시 Sonnet fallback 시도.
        """
        if self._circuit_breaker.is_open():
            raise LLMInvokeError("Circuit Breaker OPEN: LLM 호출 차단됨")

        messages = self._build_messages(system_prompt, user_prompt)

        try:
            response = await self._invoke_with_retry(
                self._primary_model, messages, settings.primary_model_id
            )
            self._circuit_breaker.record_success()
            return response.content, self._extract_token_usage(response)
        except LLMInvokeError:
            # Primary 실패 → Fallback(Sonnet) 시도
            logger.warning("Primary(Haiku) 실패, Fallback(Sonnet)으로 전환")
            try:
                response = await self._invoke_with_retry(
                    self._fallback_model, messages, settings.coldstart_model_id
                )
                self._circuit_breaker.record_success()
                return response.content, self._extract_token_usage(response)
            except LLMInvokeError as exc:
                self._circuit_breaker.record_failure()
                raise exc

    async def generate_coldstart_recommendation(
        self, system_prompt: str, user_prompt: str
    ) -> tuple[str, dict]:
        """콜드스타트 추천 생성 (Sonnet 사용). (raw_content, token_usage) 반환."""
        if self._circuit_breaker.is_open():
            raise LLMInvokeError("Circuit Breaker OPEN: LLM 호출 차단됨")

        messages = self._build_messages(system_prompt, user_prompt)

        try:
            response = await self._invoke_with_retry(
                self._coldstart_model, messages, settings.coldstart_model_id
            )
            self._circuit_breaker.record_success()
            return response.content, self._extract_token_usage(response)
        except LLMInvokeError as exc:
            self._circuit_breaker.record_failure()
            raise exc

    async def generate_reason(
        self, system_prompt: str, user_prompt: str
    ) -> tuple[str, dict]:
        """추천 이유 생성 (Haiku 사용). (raw_content, token_usage) 반환."""
        if self._circuit_breaker.is_open():
            raise LLMInvokeError("Circuit Breaker OPEN: LLM 호출 차단됨")

        messages = self._build_messages(system_prompt, user_prompt)

        try:
            response = await self._invoke_with_retry(
                self._reason_model, messages, settings.reason_model_id
            )
            self._circuit_breaker.record_success()
            return response.content, self._extract_token_usage(response)
        except LLMInvokeError:
            # Reason 실패 → Fallback(Sonnet) 시도
            logger.warning("Reason(Haiku) 실패, Fallback(Sonnet)으로 전환")
            try:
                response = await self._invoke_with_retry(
                    self._fallback_model, messages, settings.coldstart_model_id
                )
                self._circuit_breaker.record_success()
                return response.content, self._extract_token_usage(response)
            except LLMInvokeError as exc:
                self._circuit_breaker.record_failure()
                raise exc

    def _extract_token_usage(self, response: AIMessage) -> dict:
        """LLM 응답에서 토큰 사용량 추출."""
        usage = getattr(response, "usage_metadata", None)
        if usage is None:
            # LangChain AIMessage usage_metadata 구조 대응
            usage = getattr(response, "response_metadata", {}).get("usage", {})
            prompt_tokens = usage.get("input_tokens", 0)
            completion_tokens = usage.get("output_tokens", 0)
        else:
            prompt_tokens = getattr(usage, "input_tokens", 0)
            completion_tokens = getattr(usage, "output_tokens", 0)

        return {
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "total_tokens": prompt_tokens + completion_tokens,
        }


# 싱글턴 인스턴스
_llm_client: LLMClient | None = None


def get_llm_client() -> LLMClient:
    global _llm_client
    if _llm_client is None:
        _llm_client = LLMClient()
    return _llm_client
