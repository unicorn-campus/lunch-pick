package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 추천 이유 상세 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record RecommendationReasonResponse(
        String recommendationId,
        String naturalLanguageReason,
        int confidenceScore,
        List<String> contextTags,
        boolean isReasonReady,
        String fallbackMessage
) {}
