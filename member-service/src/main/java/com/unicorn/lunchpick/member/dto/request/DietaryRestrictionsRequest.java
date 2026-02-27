package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 알레르기/식이제한 설정 요청 DTO
 *
 * <p>민감 정보(건강 관련)이므로 {@code healthInfoConsentGiven}이 반드시 true여야 합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record DietaryRestrictionsRequest(

        /**
         * 건강 관련 정보 수집 동의 (민감정보 별도 동의)
         */
        @NotNull(message = "건강 정보 동의 여부는 필수입니다.")
        Boolean healthInfoConsentGiven,

        /**
         * 알레르기 항목 목록 (8대 알레르겐)
         */
        List<String> allergens,

        /**
         * 직접 입력 알레르기 항목 목록
         */
        List<String> customAllergens,

        /**
         * 식이 유형 (일반/채식/비건/할랄/기타)
         */
        String dietType
) {
}
