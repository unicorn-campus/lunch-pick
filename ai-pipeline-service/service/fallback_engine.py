"""
규칙 기반 폴백 추천 엔진.

LLM 장애 시 사용되는 규칙 기반 추천 엔진.
LLM 없이 위치 반경 내 인기 식당 + 알레르기 필터 + 거리 정렬로 추천 생성.

AI-06: 규칙 기반 폴백 추천 (LLM 미사용)
"""

import logging
import math

from model.recommendation_response import RecommendedRestaurant

logger = logging.getLogger(__name__)

# 내장 식당 데이터 (실제 환경에서는 DB 조회 또는 recommendation-service에서 전달)
_POPULAR_RESTAURANTS = [
    {
        "restaurant_id": "rest-001",
        "restaurant_name": "광화문 된장마을",
        "representative_menu": "된장찌개 정식",
        "category": "한식",
        "latitude": 37.5700,
        "longitude": 126.9770,
        "allergens": [],
        "popularity_rank": 1,
    },
    {
        "restaurant_id": "rest-002",
        "restaurant_name": "사쿠라 스시",
        "representative_menu": "런치 세트",
        "category": "일식",
        "latitude": 37.5660,
        "longitude": 126.9790,
        "allergens": ["새우"],
        "popularity_rank": 2,
    },
    {
        "restaurant_id": "rest-003",
        "restaurant_name": "그린 샐러드바",
        "representative_menu": "프레시 볼",
        "category": "샐러드/건강식",
        "latitude": 37.5655,
        "longitude": 126.9775,
        "allergens": [],
        "popularity_rank": 3,
    },
    {
        "restaurant_id": "rest-004",
        "restaurant_name": "종로 한식뷔페",
        "representative_menu": "한식 뷔페",
        "category": "한식",
        "latitude": 37.5670,
        "longitude": 126.9785,
        "allergens": [],
        "popularity_rank": 4,
    },
    {
        "restaurant_id": "rest-005",
        "restaurant_name": "명동 칼국수",
        "representative_menu": "바지락 칼국수",
        "category": "한식",
        "latitude": 37.5640,
        "longitude": 126.9760,
        "allergens": ["조개류"],
        "popularity_rank": 5,
    },
    {
        "restaurant_id": "rest-006",
        "restaurant_name": "이탈리아 파스타",
        "representative_menu": "까르보나라",
        "category": "양식",
        "latitude": 37.5680,
        "longitude": 126.9800,
        "allergens": ["땅콩"],
        "popularity_rank": 6,
    },
]

FALLBACK_CONFIDENCE_SCORE = 60
FALLBACK_REASON_SUMMARY = "주변 인기 식당이에요"
SEARCH_RADIUS_METERS = 500


class FallbackEngine:
    """LLM 장애 시 규칙 기반 추천 엔진."""

    def get_fallback_recommendations(
        self,
        latitude: float,
        longitude: float,
        allergen_filter: list[str],
        exclude_restaurant_ids: list[str] | None = None,
        max_results: int = 3,
        available_restaurants=None,
    ) -> list[RecommendedRestaurant]:
        """규칙 기반 추천 생성.

        1. 위치 반경 500m 내 식당 조회
        2. 알레르기 하드 필터 적용
        3. 제외 식당 필터 적용
        4. 거리순 + 인기 순위 복합 정렬
        5. 상위 max_results개 선택

        Args:
            latitude: 현재 위치 위도
            longitude: 현재 위치 경도
            allergen_filter: 알레르기 제외 목록 (하드 필터)
            exclude_restaurant_ids: 제외할 식당 ID 목록
            max_results: 최대 추천 수

        Returns:
            RecommendedRestaurant 목록 (최소 1개)
        """
        exclude_ids = set(exclude_restaurant_ids or [])

        # 1. 위치 반경 내 식당 조회 (전달된 목록이 있으면 사용)
        if available_restaurants:
            nearby = [
                {
                    "restaurant_id": r.restaurant_id,
                    "restaurant_name": r.restaurant_name,
                    "representative_menu": r.representative_menu,
                    "category": r.category,
                    "latitude": latitude,
                    "longitude": longitude,
                    "allergens": r.allergens,
                    "popularity_rank": idx + 1,
                    "_distance_meters": r.distance_meters,
                }
                for idx, r in enumerate(available_restaurants)
            ]
        else:
            nearby = self._query_nearby_restaurants(latitude, longitude, SEARCH_RADIUS_METERS)

        # 2. 알레르기 하드 필터
        filtered = self._apply_allergen_filter(nearby, allergen_filter)

        # 3. 제외 식당 필터
        filtered = [r for r in filtered if r["restaurant_id"] not in exclude_ids]

        # 4. 거리 계산 후 정렬 (거리 1순위, 인기 2순위)
        filtered = self._sort_by_distance_and_popularity(filtered, latitude, longitude)

        # 5. 상위 max_results개
        top = filtered[:max_results]

        if not top:
            # 필터 결과가 없으면 알레르기 필터만 유지한 상태로 재시도 (제외 식당 무시)
            logger.warning(
                "폴백 추천: 필터 결과 없음. 제외 식당 무시하고 재시도 (lat=%.4f, lon=%.4f)",
                latitude,
                longitude,
            )
            filtered_no_exclude = self._apply_allergen_filter(nearby, allergen_filter)
            filtered_no_exclude = self._sort_by_distance_and_popularity(
                filtered_no_exclude, latitude, longitude
            )
            top = filtered_no_exclude[:max_results]

        if not top:
            # 반경 내 식당이 없으면 전체 인기 식당에서 폴백
            logger.warning("폴백 추천: 반경 내 식당 없음. 전체 인기 식당에서 선택")
            all_filtered = self._apply_allergen_filter(_POPULAR_RESTAURANTS, allergen_filter)
            top = all_filtered[:max_results]

        return self._to_response_models(top, latitude, longitude)

    def _query_nearby_restaurants(
        self, latitude: float, longitude: float, radius_meters: float
    ) -> list[dict]:
        """반경 내 식당 조회 (내장 데이터 기반)."""
        result = []
        for restaurant in _POPULAR_RESTAURANTS:
            distance = self._haversine_distance(
                latitude, longitude,
                restaurant["latitude"], restaurant["longitude"],
            )
            if distance <= radius_meters:
                restaurant_with_distance = dict(restaurant)
                restaurant_with_distance["_distance_meters"] = int(distance)
                result.append(restaurant_with_distance)
        return result

    def _apply_allergen_filter(
        self, restaurants: list[dict], allergen_filter: list[str]
    ) -> list[dict]:
        """알레르기 하드 필터 적용. 알레르기 성분이 하나라도 있으면 제외."""
        if not allergen_filter:
            return restaurants
        return [
            r for r in restaurants
            if not any(allergen in r.get("allergens", []) for allergen in allergen_filter)
        ]

    def _sort_by_distance_and_popularity(
        self, restaurants: list[dict], latitude: float, longitude: float
    ) -> list[dict]:
        """거리 우선, 동거리 시 인기 순위 정렬."""
        for r in restaurants:
            if "_distance_meters" not in r:
                r["_distance_meters"] = int(
                    self._haversine_distance(
                        latitude, longitude, r["latitude"], r["longitude"]
                    )
                )
        return sorted(
            restaurants,
            key=lambda r: (r["_distance_meters"], r.get("popularity_rank", 999)),
        )

    def _haversine_distance(
        self,
        lat1: float, lon1: float,
        lat2: float, lon2: float,
    ) -> float:
        """두 좌표 간 거리(미터) 계산 (Haversine 공식)."""
        earth_radius_m = 6_371_000
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = (
            math.sin(dlat / 2) ** 2
            + math.cos(math.radians(lat1))
            * math.cos(math.radians(lat2))
            * math.sin(dlon / 2) ** 2
        )
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return earth_radius_m * c

    def _to_response_models(
        self, restaurants: list[dict], latitude: float, longitude: float
    ) -> list[RecommendedRestaurant]:
        """dict → RecommendedRestaurant 변환."""
        result = []
        for r in restaurants:
            distance_meters = r.get("_distance_meters") or int(
                self._haversine_distance(
                    latitude, longitude, r["latitude"], r["longitude"]
                )
            )
            estimated_walk_minutes = max(1, round(distance_meters / 80))  # 도보 80m/분

            restaurant = RecommendedRestaurant(
                restaurantId=r["restaurant_id"],
                restaurantName=r["restaurant_name"],
                representativeMenu=r["representative_menu"],
                category=r["category"],
                reasonSummary=FALLBACK_REASON_SUMMARY,
                confidenceScore=FALLBACK_CONFIDENCE_SCORE,
                distanceMeters=distance_meters,
                estimatedWalkMinutes=estimated_walk_minutes,
            )
            result.append(restaurant)
        return result
