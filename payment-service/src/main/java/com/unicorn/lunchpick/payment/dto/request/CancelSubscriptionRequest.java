package com.unicorn.lunchpick.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 구독 해지 요청 DTO
 *
 * @param cancelReason       해지 사유 (COST / NOT_USING / QUALITY / OTHER)
 * @param cancelReasonDetail 해지 사유 상세 (선택)
 */
public record CancelSubscriptionRequest(

        @NotBlank(message = "해지 사유는 필수입니다.")
        String cancelReason,

        String cancelReasonDetail
) {
}
