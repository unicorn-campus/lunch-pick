package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 프로필 응답 DTO
 *
 * <p>현재 로그인한 회원의 프로필 정보를 반환합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record MemberProfileResponse(

        /**
         * 회원 도메인 식별자 (UUID)
         */
        String memberId,

        /**
         * 닉네임
         */
        String nickname,

        /**
         * 카카오 연동 이메일
         */
        String email,

        /**
         * 식이 유형
         */
        String dietType,

        /**
         * 알레르기 목록 (시스템 + 직접 입력 통합)
         */
        List<String> allergens,

        /**
         * 위치 기반 추천 활성화 여부
         */
        boolean locationEnabled,

        /**
         * 알림 설정
         */
        NotificationSettingsDto notificationSettings,

        /**
         * 구독 상태
         */
        SubscriptionStatusDto subscription,

        /**
         * 온보딩 완료 여부
         */
        boolean onboardingCompleted,

        /**
         * 가입 일시
         */
        LocalDateTime createdAt
) {
}
