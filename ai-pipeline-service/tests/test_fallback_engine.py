"""FallbackEngine 단위 테스트."""

import pytest

from service.fallback_engine import FallbackEngine


@pytest.fixture
def engine():
    return FallbackEngine()


def test_returns_at_most_3_recommendations(engine):
    results = engine.get_fallback_recommendations(
        latitude=37.5665,
        longitude=126.9780,
        allergen_filter=[],
    )
    assert 1 <= len(results) <= 3


def test_allergen_filter_removes_restaurants(engine):
    # "새우" 알레르기 → rest-002(사쿠라 스시) 제외
    results = engine.get_fallback_recommendations(
        latitude=37.5665,
        longitude=126.9780,
        allergen_filter=["새우"],
    )
    restaurant_ids = [r.restaurant_id for r in results]
    assert "rest-002" not in restaurant_ids


def test_all_allergens_filtered_still_returns_results(engine):
    # 일부 알레르기만 필터 → 나머지 식당 반환
    results = engine.get_fallback_recommendations(
        latitude=37.5665,
        longitude=126.9780,
        allergen_filter=["새우", "땅콩"],
    )
    assert len(results) >= 1


def test_exclude_restaurant_ids(engine):
    results = engine.get_fallback_recommendations(
        latitude=37.5665,
        longitude=126.9780,
        allergen_filter=[],
        exclude_restaurant_ids=["rest-001", "rest-003"],
    )
    restaurant_ids = [r.restaurant_id for r in results]
    assert "rest-001" not in restaurant_ids
    assert "rest-003" not in restaurant_ids


def test_confidence_score_is_fixed(engine):
    results = engine.get_fallback_recommendations(37.5665, 126.9780, [])
    for r in results:
        assert r.confidence_score == 60


def test_reason_summary_is_fallback_message(engine):
    results = engine.get_fallback_recommendations(37.5665, 126.9780, [])
    for r in results:
        assert r.reason_summary == "주변 인기 식당이에요"


def test_distance_and_walk_minutes_populated(engine):
    results = engine.get_fallback_recommendations(37.5665, 126.9780, [])
    for r in results:
        assert r.distance_meters is not None
        assert r.distance_meters >= 0
        assert r.estimated_walk_minutes is not None
        assert r.estimated_walk_minutes >= 1


def test_haversine_distance(engine):
    # 같은 좌표는 거리 0
    dist = engine._haversine_distance(37.5665, 126.9780, 37.5665, 126.9780)
    assert dist == pytest.approx(0.0, abs=1.0)


def test_haversine_distance_known_points(engine):
    # 광화문 ~ 시청역 약 700m
    dist = engine._haversine_distance(37.5759, 126.9769, 37.5663, 126.9779)
    assert 500 < dist < 1500
