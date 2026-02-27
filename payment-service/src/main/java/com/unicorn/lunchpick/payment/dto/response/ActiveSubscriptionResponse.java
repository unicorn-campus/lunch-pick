package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 활성 구독 조회 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Builder
public record ActiveSubscriptionResponse(

        /** 구독 도메인 식별자 (UUID) */
        String subscriptionId,

        /** 구독 플랜 */
        String planId,

        /** 구독 상태 */
        String status,

        /** 현재 기간 종료일시 */
        LocalDateTime currentPeriodEndsAt
) {
}
