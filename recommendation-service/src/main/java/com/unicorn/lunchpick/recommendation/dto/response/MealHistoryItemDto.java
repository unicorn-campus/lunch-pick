package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 식사 이력 항목 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record MealHistoryItemDto(
        String mealId,
        LocalDate date,
        String restaurantName,
        String menuName,
        String category,
        String categoryColor,
        String satisfaction,
        LocalDateTime recordedAt
) {}
