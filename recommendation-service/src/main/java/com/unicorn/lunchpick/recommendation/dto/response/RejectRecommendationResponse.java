package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

/**
 * 추천 거절 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record RejectRecommendationResponse(
        boolean rejected,
        RecommendationCardDto alternativeRecommendation,
        boolean hasAlternative,
        String message,
        String noAlternativeMessage
) {}
