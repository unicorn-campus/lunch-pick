package com.unicorn.lunchpick.payment.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 7일 무료 연장 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record ExtendTrialResponse(

        /** 응답 메시지 */
        String message,

        /** 연장된 구독 만료일 */
        LocalDateTime newExpiresAt
) {
}
