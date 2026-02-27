package com.unicorn.lunchpick.recommendation.dto.request;

import java.time.LocalDateTime;

/**
 * 식사 기록 수정 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record UpdateMealRequest(
        String restaurantId,
        String menuName,
        LocalDateTime recordedAt
) {}
