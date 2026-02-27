package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

/**
 * 온보딩 진행 상태 임시 저장 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record OnboardingProgressSaveResponse(

        /**
         * 안내 메시지
         */
        String message,

        /**
         * 현재까지 저장된 스와이프 수
         */
        int savedCount
) {
}
