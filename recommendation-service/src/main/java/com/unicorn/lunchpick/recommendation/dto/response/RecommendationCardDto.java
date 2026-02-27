package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

/**
 * 추천 카드 DTO
 *
 * <p>오늘의 추천 1개 카드 정보를 담습니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record RecommendationCardDto(
        String recommendationId,
        String restaurantId,
        String restaurantName,
        String representativeMenu,
        String reasonSummary,
        int confidenceScore,
        int distanceMeters,
        int estimatedWalkMinutes,
        String category,
        boolean isFallback
) {}
