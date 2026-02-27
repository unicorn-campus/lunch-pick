package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 *
 * <p>닉네임과 알림 설정을 수정합니다.
 * 닉네임은 2~20자, 한글/영문/숫자/공백만 허용합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record UpdateProfileRequest(

        /**
         * 닉네임 (2~20자, 특수문자 제한)
         */
        @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]+$", message = "닉네임은 특수문자 없이 입력해주세요.")
        String nickname,

        /**
         * 알림 설정
         */
        NotificationSettingsRequest notificationSettings
) {

    /**
     * 알림 설정 내부 레코드
     */
    public record NotificationSettingsRequest(

            /**
             * 추천 알림 ON/OFF
             */
            Boolean recommendationAlert,

            /**
             * 피드백 리마인더 ON/OFF
             */
            Boolean feedbackReminder
    ) {
    }
}
