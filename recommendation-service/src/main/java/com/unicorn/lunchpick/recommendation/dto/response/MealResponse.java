package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 식사 기록 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record MealResponse(
        String mealId,
        String restaurantName,
        String menuName,
        LocalDateTime recordedAt,
        String message
) {}
