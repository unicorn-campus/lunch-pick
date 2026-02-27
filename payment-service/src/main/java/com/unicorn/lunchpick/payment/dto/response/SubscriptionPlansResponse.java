package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 구독 플랜 조회 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record SubscriptionPlansResponse(

        /** 구독 플랜 목록 (FREE / PREMIUM_MONTHLY / PREMIUM_ANNUAL) */
        List<SubscriptionPlanDto> plans,

        /** 현재 사용 중인 플랜 */
        String currentPlan,

        /** 전환 트리거 메시지 (30일 제한 도달 시, nullable) */
        String promotionMessage
) {
}
