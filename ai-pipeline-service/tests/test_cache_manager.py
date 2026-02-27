"""CacheManager 단위 테스트."""

import pytest

from cache.cache_manager import (
    build_recommendation_cache_key,
    build_reason_cache_key,
    build_stale_cache_key,
    calculate_location_grid,
    calculate_ttl_until_1pm,
    get_cached_until_iso,
)


def test_calculate_location_grid():
    grid = calculate_location_grid(37.5665, 126.9780)
    # round(37.5665, 3) = 37.566 또는 37.567 (Python banker's rounding)
    # round(126.9780, 3) = 126.978
    parts = grid.split("_")
    assert len(parts) == 2
    assert parts[1] == "126.978"
    assert parts[0].startswith("37.")


def test_calculate_location_grid_rounding():
    # 소수점 3자리 반올림
    grid = calculate_location_grid(37.56654, 126.97805)
    parts = grid.split("_")
    assert len(parts) == 2
    assert len(parts[0].split(".")[1]) <= 3


def test_calculate_ttl_until_1pm_is_positive():
    ttl = calculate_ttl_until_1pm()
    assert ttl >= 60


def test_calculate_ttl_until_1pm_max():
    # 최대 TTL은 24시간 + 여유를 넘지 않음
    ttl = calculate_ttl_until_1pm()
    assert ttl <= 86400 + 3600


def test_get_cached_until_iso_format():
    result = get_cached_until_iso()
    assert "T" in result
    assert "13:00:00" in result


def test_build_recommendation_cache_key():
    key = build_recommendation_cache_key(
        "member-1", "37.567_126.978", "RAINY", "WED"
    )
    assert key == "rec:member-1:37.567_126.978:RAINY:WED"


def test_build_stale_cache_key():
    key = build_stale_cache_key("member-1", "37.567_126.978", "CLEAR", "MON")
    assert key == "rec:stale:member-1:37.567_126.978:CLEAR:MON"


def test_build_reason_cache_key():
    key = build_reason_cache_key("rec-12345")
    assert key == "reason:rec-12345"


@pytest.mark.asyncio
async def test_cache_manager_get_returns_none_on_miss(mock_redis_empty):
    from cache.cache_manager import CacheManager
    manager = CacheManager()
    result = await manager.get("nonexistent-key")
    assert result is None


@pytest.mark.asyncio
async def test_cache_manager_set_and_get(mock_redis_empty):
    from cache.cache_manager import CacheManager
    manager = CacheManager()
    await manager.set("test-key", {"data": "value"}, ttl=60)
    # mock은 실제 저장 없이 set만 검증
    mock_redis_empty["set"].assert_called_once()


@pytest.mark.asyncio
async def test_cache_manager_is_connected(mock_redis_connected):
    from cache.cache_manager import CacheManager
    manager = CacheManager()
    result = await manager.is_connected()
    assert result is True
