"""Circuit Breaker 상태 전이 단위 테스트."""

import time

import pytest

from llm.circuit_breaker import CBState, CircuitBreaker


def test_initial_state_is_closed():
    cb = CircuitBreaker()
    assert cb.state == CBState.CLOSED
    assert cb.failure_count == 0
    assert cb.is_open() is False


def test_open_after_failure_threshold():
    cb = CircuitBreaker(failure_threshold=5)
    for _ in range(4):
        cb.record_failure()
        assert cb.state == CBState.CLOSED
    cb.record_failure()  # 5번째 실패
    assert cb.state == CBState.OPEN


def test_is_open_returns_true_when_open():
    cb = CircuitBreaker(failure_threshold=2)
    cb.record_failure()
    cb.record_failure()
    assert cb.state == CBState.OPEN
    assert cb.is_open() is True


def test_transitions_to_half_open_after_recovery_timeout():
    cb = CircuitBreaker(failure_threshold=2, recovery_timeout=0.1)
    cb.record_failure()
    cb.record_failure()
    assert cb.state == CBState.OPEN

    time.sleep(0.15)
    # is_open() 호출 시 Half-Open 전환
    result = cb.is_open()
    assert result is False  # Half-Open에서 1회 허용
    assert cb.state == CBState.HALF_OPEN


def test_closed_after_success_in_half_open():
    cb = CircuitBreaker(failure_threshold=2, recovery_timeout=0.1)
    cb.record_failure()
    cb.record_failure()
    time.sleep(0.15)
    cb.is_open()  # Half-Open 전환
    assert cb.state == CBState.HALF_OPEN

    cb.record_success()
    assert cb.state == CBState.CLOSED
    assert cb.failure_count == 0


def test_open_again_after_failure_in_half_open():
    cb = CircuitBreaker(failure_threshold=2, recovery_timeout=0.1)
    cb.record_failure()
    cb.record_failure()
    time.sleep(0.15)
    cb.is_open()  # Half-Open 전환

    cb.record_failure()
    assert cb.state == CBState.OPEN


def test_success_resets_failure_count():
    cb = CircuitBreaker(failure_threshold=5)
    cb.record_failure()
    cb.record_failure()
    cb.record_success()
    assert cb.failure_count == 0
    assert cb.state == CBState.CLOSED


def test_reset():
    cb = CircuitBreaker(failure_threshold=2)
    cb.record_failure()
    cb.record_failure()
    assert cb.state == CBState.OPEN
    cb.reset()
    assert cb.state == CBState.CLOSED
    assert cb.failure_count == 0
