package com.unicorn.lunchpick.recommendation.client.dto;

import lombok.Builder;

/**
 * 카카오맵 Place API 응답에서 파싱된 주변 식당 정보
 *
 * @param restaurantId       카카오 place_id
 * @param restaurantName     식당명 (place_name)
 * @param representativeMenu 대표 메뉴 (category_name에서 소분류 추출)
 * @param category           카테고리 대분류 (한식/중식/일식 등)
 * @param distanceMeters     거리 (미터)
 * @param estimatedWalkMinutes 예상 도보 시간 (분)
 * @param address            도로명 주소
 * @param phone              전화번호
 * @param longitude          경도 (x)
 * @param latitude           위도 (y)
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Builder
public record NearbyRestaurant(
        String restaurantId,
        String restaurantName,
        String representativeMenu,
        String category,
        int distanceMeters,
        int estimatedWalkMinutes,
        String address,
        String phone,
        double longitude,
        double latitude
) {}
