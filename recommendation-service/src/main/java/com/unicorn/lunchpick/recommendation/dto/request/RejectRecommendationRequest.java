package com.unicorn.lunchpick.recommendation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 추천 거절 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record RejectRecommendationRequest(

        /**
         * 거절 사유 (MOOD_NOT_MATCH / TOO_FAR / RECENTLY_VISITED / OTHER)
         */
        @NotBlank(message = "거절 사유는 필수입니다.")
        @Pattern(regexp = "MOOD_NOT_MATCH|TOO_FAR|RECENTLY_VISITED|OTHER",
                message = "거절 사유는 MOOD_NOT_MATCH, TOO_FAR, RECENTLY_VISITED, OTHER 중 하나여야 합니다.")
        String rejectReason
) {}
