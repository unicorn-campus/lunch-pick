package com.unicorn.lunchpick.recommendation.client;

import com.unicorn.lunchpick.recommendation.client.dto.NearbyRestaurant;

import java.util.List;

/**
 * 카카오맵 Place API 클라이언트 인터페이스
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
public interface KakaoPlaceClient {

    /**
     * 주변 식당 목록 조회
     *
     * @param latitude     위도
     * @param longitude    경도
     * @param radiusMeters 검색 반경 (미터)
     * @return 주변 식당 목록 (실패 시 빈 리스트)
     */
    List<NearbyRestaurant> searchNearbyRestaurants(double latitude, double longitude, int radiusMeters);
}
