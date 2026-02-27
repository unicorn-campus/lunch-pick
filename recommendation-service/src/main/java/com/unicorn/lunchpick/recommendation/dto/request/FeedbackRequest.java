package com.unicorn.lunchpick.recommendation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 피드백 제출 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record FeedbackRequest(

        /**
         * 만족도 (GOOD / BAD / NEUTRAL)
         */
        @NotBlank(message = "만족도는 필수입니다.")
        @Pattern(regexp = "GOOD|BAD|NEUTRAL",
                message = "만족도는 GOOD, BAD, NEUTRAL 중 하나여야 합니다.")
        String satisfaction,

        /**
         * 피드백 키워드 (TASTE / PRICE / KINDNESS, nullable)
         */
        @Pattern(regexp = "TASTE|PRICE|KINDNESS",
                message = "키워드는 TASTE, PRICE, KINDNESS 중 하나여야 합니다.")
        String keyword
) {}
