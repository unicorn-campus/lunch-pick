package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 알레르기/식이제한 설정 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record DietaryRestrictionsResponse(

        /**
         * 안내 메시지
         */
        String message,

        /**
         * 적용된 알레르기 목록 (시스템 + 직접 입력 통합)
         */
        List<String> appliedAllergens,

        /**
         * 식이 유형
         */
        String dietType
) {
}
