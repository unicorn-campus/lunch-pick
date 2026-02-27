package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 위치 정보 동의 요청 DTO
 *
 * <p>위치정보법 준수: 수집 목적 고지, 보유 기간(6개월) 고지 후 동의 여부를 기록합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record LocationConsentRequest(

        /**
         * 위치 정보 수집 동의 여부
         */
        @NotNull(message = "위치 동의 여부는 필수입니다.")
        Boolean consented,

        /**
         * 동의 시각 (클라이언트 기준, 선택)
         */
        LocalDateTime consentedAt
) {
}
