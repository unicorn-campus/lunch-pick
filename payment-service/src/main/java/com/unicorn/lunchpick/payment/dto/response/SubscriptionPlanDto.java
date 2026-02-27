package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 구독 플랜 정보 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record SubscriptionPlanDto(

        /** 플랜 식별자 (FREE / PREMIUM_MONTHLY / PREMIUM_ANNUAL) */
        String planId,

        /** 플랜명 */
        String planName,

        /** 월 결제 금액 (원) */
        int pricePerMonth,

        /** 총 결제 금액 (원, 연간은 연 결제 금액) */
        int totalPrice,

        /** 결제 주기 (MONTHLY / ANNUAL) */
        String billingCycle,

        /** 할인율 (%) */
        double discountRate,

        /** 플랜 제공 기능 목록 */
        List<String> features
) {
}
