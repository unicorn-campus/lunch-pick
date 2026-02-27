package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

/**
 * 추천 수락 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record AcceptRecommendationResponse(
        String acceptanceId,
        String restaurantId,
        String restaurantName,
        String restaurantAddress,
        String message
) {}
