package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 식사 이력 타임라인 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record MealHistoryResponse(
        List<MealHistoryItemDto> meals,
        int totalCount,
        String message
) {}
