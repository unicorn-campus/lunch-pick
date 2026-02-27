package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

/**
 * 위치 정보 동의 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record LocationConsentResponse(

        /**
         * 위치 기반 추천 활성화 여부
         */
        boolean locationEnabled,

        /**
         * 안내 메시지
         */
        String message
) {
}
