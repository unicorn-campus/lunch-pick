"""
Circuit Breaker 상태 관리.

상태 전이:
  Closed (정상) → 연속 5회 실패 → Open (장애, 60초 대기)
  Open → 60초 경과 → Half-Open (복구 시도, 1회 허용)
  Half-Open → 성공 → Closed
  Half-Open → 실패 → Open (대기 시간 재시작)
"""

import logging
import time
from enum import Enum

logger = logging.getLogger(__name__)


class CBState(Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreaker:
    """LLM API 호출에 대한 Circuit Breaker.

    Args:
        failure_threshold: Open 전환 전 허용 연속 실패 횟수 (기본 5회)
        recovery_timeout: Open 상태 유지 시간(초) (기본 60초)
    """

    def __init__(
        self,
        failure_threshold: int = 5,
        recovery_timeout: float = 60.0,
    ) -> None:
        self._state = CBState.CLOSED
        self._failure_count: int = 0
        self._failure_threshold = failure_threshold
        self._last_failure_time: float = 0.0
        self._recovery_timeout = recovery_timeout

    @property
    def state(self) -> CBState:
        return self._state

    @property
    def failure_count(self) -> int:
        return self._failure_count

    def is_open(self) -> bool:
        """현재 호출을 차단해야 하면 True.

        Open 상태에서 recovery_timeout이 경과하면 Half-Open으로 전환하고
        1회 호출을 허용한다 (False 반환).
        """
        if self._state == CBState.OPEN:
            elapsed = time.time() - self._last_failure_time
            if elapsed >= self._recovery_timeout:
                self._transition_to_half_open()
                return False  # Half-Open: 1회 허용
            return True
        return False

    def record_success(self) -> None:
        """성공 기록. Half-Open → Closed 전환."""
        prev = self._failure_count
        self._failure_count = 0
        if self._state != CBState.CLOSED:
            logger.info(
                "Circuit Breaker: %s → CLOSED (이전 실패 횟수=%d)", self._state.value, prev
            )
        self._state = CBState.CLOSED

    def record_failure(self) -> None:
        """실패 기록. 임계값 초과 시 Open 전환."""
        self._failure_count += 1
        self._last_failure_time = time.time()
        if self._failure_count >= self._failure_threshold:
            self._transition_to_open()

    def _transition_to_open(self) -> None:
        if self._state != CBState.OPEN:
            logger.warning(
                "Circuit Breaker: %s → OPEN (연속 실패 %d회)",
                self._state.value,
                self._failure_count,
            )
        self._state = CBState.OPEN

    def _transition_to_half_open(self) -> None:
        logger.info("Circuit Breaker: OPEN → HALF_OPEN (복구 시도)")
        self._state = CBState.HALF_OPEN

    def reset(self) -> None:
        """테스트용 강제 초기화."""
        self._state = CBState.CLOSED
        self._failure_count = 0
        self._last_failure_time = 0.0
