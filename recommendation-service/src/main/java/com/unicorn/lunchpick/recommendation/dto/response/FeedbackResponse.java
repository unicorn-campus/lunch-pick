package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

/**
 * 피드백 제출 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record FeedbackResponse(
        String message,
        String reflectionMessage,
        long totalFeedbackCount
) {}
