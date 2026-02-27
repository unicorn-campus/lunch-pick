package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 구독 결제 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record CreateSubscriptionResponse(

        /** 구독 도메인 식별자 (UUID) */
        String subscriptionId,

        /** 구독 플랜 */
        String planId,

        /** 구독 상태 (ACTIVE) */
        String status,

        /** 구독 시작일시 */
        LocalDateTime startedAt,

        /** 다음 결제일시 */
        LocalDateTime nextBillingAt,

        /** 결제 금액 (원) */
        int amount,

        /** PG사 거래 ID */
        String transactionId,

        /** 응답 메시지 */
        String message,

        /** 청약철회 가능 기한 (결제일 + 7일) */
        LocalDateTime withdrawalDeadline
) {
}
