"""
Redis Cache-Aside 패턴 구현.

캐시 키 설계:
  추천 결과 (유효): rec:{member_id}:{location_grid}:{weather_code}:{weekday}
  추천 결과 (Stale): rec:stale:{member_id}:{location_grid}:{weather_code}:{weekday}
  이유 생성 결과:   reason:{recommendation_id}
  프롬프트 해시:    ai:response:{prompt_hash_12}

TTL:
  추천/이유 캐시: 당일 13:00까지 남은 초
  Stale 캐시:    24시간
  프롬프트 해시:  4시간
"""

import json
import logging
from datetime import datetime, timedelta

import pytz
import redis.asyncio as aioredis

from config import settings

logger = logging.getLogger(__name__)

KST = pytz.timezone("Asia/Seoul")

# TTL 상수
STALE_TTL_SECONDS = 86400          # 24시간
PROMPT_HASH_TTL_SECONDS = 14400    # 4시간
REASON_CACHE_TTL_SECONDS = 3600    # 1시간 (기본값, 13:00까지보다 짧으면 13:00 사용)


def calculate_location_grid(latitude: float, longitude: float) -> str:
    """위도/경도를 약 200m 격자로 양자화.

    소수점 3자리 반올림 = 약 111m 단위.
    """
    grid_lat = round(latitude, 3)
    grid_lon = round(longitude, 3)
    return f"{grid_lat:.3f}_{grid_lon:.3f}"


def calculate_ttl_until_1pm() -> int:
    """현재 KST 기준 당일 13:00까지 남은 초.

    13:00 이후이면 다음날 13:00까지 남은 초를 반환한다.
    최소값은 60초.
    """
    now_kst = datetime.now(KST)
    target = now_kst.replace(hour=13, minute=0, second=0, microsecond=0)
    if now_kst >= target:
        target = target + timedelta(days=1)
    ttl = int((target - now_kst).total_seconds())
    return max(ttl, 60)


def get_cached_until_iso() -> str:
    """당일 13:00 KST를 ISO 8601 형식으로 반환."""
    now_kst = datetime.now(KST)
    target = now_kst.replace(hour=13, minute=0, second=0, microsecond=0)
    if now_kst >= target:
        target = target + timedelta(days=1)
    return target.isoformat()


def build_recommendation_cache_key(
    member_id: str,
    location_grid: str,
    weather_code: str,
    weekday: str,
) -> str:
    """추천 결과 캐시 키 생성."""
    return f"rec:{member_id}:{location_grid}:{weather_code}:{weekday}"


def build_stale_cache_key(
    member_id: str,
    location_grid: str,
    weather_code: str,
    weekday: str,
) -> str:
    """Stale 캐시 키 생성."""
    return f"rec:stale:{member_id}:{location_grid}:{weather_code}:{weekday}"


def build_reason_cache_key(recommendation_id: str) -> str:
    """추천 이유 캐시 키 생성."""
    return f"reason:{recommendation_id}"


def build_prompt_hash_key(prompt_hash: str) -> str:
    """프롬프트 해시 캐시 키 생성."""
    return f"ai:response:{prompt_hash[:12]}"


class CacheManager:
    """Redis Cache-Aside 패턴 구현체."""

    def __init__(self) -> None:
        self._redis: aioredis.Redis | None = None

    async def _get_redis(self) -> aioredis.Redis:
        if self._redis is None:
            self._redis = aioredis.from_url(
                f"redis://{settings.redis_host}:{settings.redis_port}/{settings.redis_db}",
                decode_responses=True,
                socket_connect_timeout=3,
                socket_timeout=3,
            )
        return self._redis

    async def is_connected(self) -> bool:
        """Redis 연결 상태 확인."""
        try:
            r = await self._get_redis()
            await r.ping()
            return True
        except Exception:
            return False

    async def get(self, key: str) -> dict | None:
        """캐시 조회. 존재하지 않으면 None 반환."""
        try:
            r = await self._get_redis()
            raw = await r.get(key)
            if raw is None:
                return None
            return json.loads(raw)
        except Exception as exc:
            logger.warning("캐시 조회 실패 (key=%s): %s", key, exc)
            return None

    async def set(self, key: str, value: dict, ttl: int) -> None:
        """캐시 저장. TTL(초) 설정."""
        try:
            r = await self._get_redis()
            await r.setex(key, ttl, json.dumps(value, ensure_ascii=False))
        except Exception as exc:
            logger.warning("캐시 저장 실패 (key=%s): %s", key, exc)

    async def get_recommendation(
        self,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday: str,
    ) -> dict | None:
        """추천 결과 캐시 조회."""
        key = build_recommendation_cache_key(member_id, location_grid, weather_code, weekday)
        return await self.get(key)

    async def set_recommendation(
        self,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday: str,
        value: dict,
    ) -> str:
        """추천 결과 캐시 저장. 동시에 Stale 캐시도 저장. 캐시 키 반환."""
        key = build_recommendation_cache_key(member_id, location_grid, weather_code, weekday)
        ttl = calculate_ttl_until_1pm()
        await self.set(key, value, ttl)

        # Stale 캐시 저장 (24시간)
        stale_key = build_stale_cache_key(member_id, location_grid, weather_code, weekday)
        await self.set(stale_key, value, STALE_TTL_SECONDS)

        logger.debug("추천 캐시 저장 (key=%s, ttl=%ds)", key, ttl)
        return key

    async def get_stale_recommendation(
        self,
        member_id: str,
        location_grid: str,
        weather_code: str,
        weekday: str,
    ) -> dict | None:
        """Stale 캐시 조회 (Circuit Breaker Open 시 사용)."""
        stale_key = build_stale_cache_key(member_id, location_grid, weather_code, weekday)
        return await self.get(stale_key)

    async def get_reason(self, recommendation_id: str) -> dict | None:
        """추천 이유 캐시 조회."""
        key = build_reason_cache_key(recommendation_id)
        return await self.get(key)

    async def set_reason(self, recommendation_id: str, value: dict) -> None:
        """추천 이유 캐시 저장 (당일 13:00까지)."""
        key = build_reason_cache_key(recommendation_id)
        ttl = calculate_ttl_until_1pm()
        await self.set(key, value, ttl)
        logger.debug("이유 캐시 저장 (key=%s, ttl=%ds)", key, ttl)

    async def get_prompt_hash(self, prompt_hash: str) -> dict | None:
        """프롬프트 해시 캐시 조회 (동일 컨텍스트 LLM 호출 중복 제거)."""
        key = build_prompt_hash_key(prompt_hash)
        return await self.get(key)

    async def set_prompt_hash(self, prompt_hash: str, value: dict) -> None:
        """프롬프트 해시 캐시 저장 (TTL 4시간)."""
        key = build_prompt_hash_key(prompt_hash)
        await self.set(key, value, PROMPT_HASH_TTL_SECONDS)

    async def close(self) -> None:
        """Redis 연결 종료."""
        if self._redis is not None:
            await self._redis.aclose()
            self._redis = None


# 싱글턴 인스턴스
_cache_manager: CacheManager | None = None


def get_cache_manager() -> CacheManager:
    global _cache_manager
    if _cache_manager is None:
        _cache_manager = CacheManager()
    return _cache_manager
