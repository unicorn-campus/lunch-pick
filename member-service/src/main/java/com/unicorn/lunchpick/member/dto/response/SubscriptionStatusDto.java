package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 구독 상태 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record SubscriptionStatusDto(

        /**
         * 구독 플랜 (FREE / PREMIUM)
         */
        String plan,

        /**
         * 이력 조회 제한 일수 (무료: 30, 프리미엄: null)
         */
        Integer historyLimitDays,

        /**
         * 구독 만료일 (무료이면 null)
         */
        LocalDateTime expiresAt
) {
}
