package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오늘의 추천 목록 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record TodayRecommendationsResponse(
        List<RecommendationCardDto> recommendations,
        boolean isColdStart,
        String coldStartMessage,
        boolean isFallback,
        String fallbackMessage,
        LocalDateTime generatedAt
) {}
