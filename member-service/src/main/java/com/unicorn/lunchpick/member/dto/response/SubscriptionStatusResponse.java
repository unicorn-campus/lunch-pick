package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 구독 상태 조회 응답 DTO
 *
 * <p>현재 회원의 구독 플랜과 만료일을 반환합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record SubscriptionStatusResponse(

        /**
         * 구독 플랜 (FREE / PREMIUM)
         */
        String plan,

        /**
         * 이력 조회 제한 일수 (무료: 30, 프리미엄: null = 무제한)
         */
        Integer historyLimitDays,

        /**
         * 구독 만료일 (무료이면 null)
         */
        LocalDateTime expiresAt
) {
}
