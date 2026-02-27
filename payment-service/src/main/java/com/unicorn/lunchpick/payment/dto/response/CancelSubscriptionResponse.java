package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 구독 해지 예약 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record CancelSubscriptionResponse(

        /** 구독 도메인 식별자 */
        String subscriptionId,

        /** 구독 상태 (PENDING_CANCEL) */
        String status,

        /** 현재 구독 기간 종료일 (이 날까지 프리미엄 유지) */
        LocalDateTime currentPeriodEndsAt,

        /** 무료 플랜 전환 예정일 */
        LocalDateTime freePlanStartsAt,

        /** 응답 메시지 */
        String message,

        /** 무료 전환 후 이력 접근 제한 안내 */
        String dataWarningMessage
) {
}
