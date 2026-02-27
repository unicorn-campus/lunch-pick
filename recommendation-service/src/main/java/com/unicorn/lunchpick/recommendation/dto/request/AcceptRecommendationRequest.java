package com.unicorn.lunchpick.recommendation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * 추천 수락 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record AcceptRecommendationRequest(

        /** 수락 시각 */
        @NotNull(message = "수락 시각은 필수입니다.")
        LocalDateTime acceptedAt,

        /** 반응 시간 (ms) */
        @NotNull(message = "반응 시간은 필수입니다.")
        @PositiveOrZero(message = "반응 시간은 0 이상이어야 합니다.")
        Integer reactionTimeMs
) {}
