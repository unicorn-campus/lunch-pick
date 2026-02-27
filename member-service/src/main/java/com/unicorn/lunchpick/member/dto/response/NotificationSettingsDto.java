package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

/**
 * 알림 설정 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record NotificationSettingsDto(

        /**
         * 추천 알림 ON/OFF
         */
        boolean recommendationAlert,

        /**
         * 피드백 리마인더 ON/OFF
         */
        boolean feedbackReminder
) {
}
