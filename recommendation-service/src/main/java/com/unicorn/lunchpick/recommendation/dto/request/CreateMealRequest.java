package com.unicorn.lunchpick.recommendation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 식사 기록 생성 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record CreateMealRequest(

        /** 연결된 추천 ID (직접 기록 시 null) */
        String recommendationId,

        /** 식당 ID */
        @NotBlank(message = "식당 ID는 필수입니다.")
        String restaurantId,

        /** 메뉴명 (선택) */
        String menuName,

        /** 식사 기록 시각 */
        @NotNull(message = "식사 기록 시각은 필수입니다.")
        LocalDateTime recordedAt
) {}
