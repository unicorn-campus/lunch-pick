"""
헬스체크 엔드포인트.
VPC 내부 전용. Circuit Breaker 상태 + Redis 연결 상태 포함.
"""

import time

from fastapi import APIRouter

from cache.cache_manager import get_cache_manager
from llm.llm_client import get_llm_client

router = APIRouter(tags=["health"])

# 서비스 기동 시각 (uptime 계산용)
_START_TIME = time.time()


@router.get("/health")
async def health_check() -> dict:
    """
    헬스체크 엔드포인트.
    Circuit Breaker 상태 + Redis 연결 상태를 실시간으로 반환한다.
    """
    llm_client = get_llm_client()
    cb = llm_client.circuit_breaker

    cache_manager = get_cache_manager()
    redis_connected = await cache_manager.is_connected()

    return {
        "status": "ok",
        "circuit_breaker": {
            "state": cb.state.value,
            "failure_count": cb.failure_count,
        },
        "redis": {
            "connected": redis_connected,
            "db": 4,
        },
        "llm_provider": "anthropic",
        "uptime_seconds": int(time.time() - _START_TIME),
    }
