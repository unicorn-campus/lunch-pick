package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

/**
 * 카카오 소셜 로그인 응답 DTO
 *
 * <p>로그인/회원가입 성공 시 JWT 액세스 토큰과 사용자 정보를 반환합니다.</p>
 *
 * <p><b>HTTP 상태 코드:</b></p>
 * <ul>
 *   <li>200 OK — 기존 회원 로그인 ({@code isNewUser: false})</li>
 *   <li>201 Created — 신규 회원 가입 ({@code isNewUser: true})</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record AuthResponse(

        /**
         * JWT 액세스 토큰 (만료 1시간)
         */
        String accessToken,

        /**
         * 토큰 타입 ("Bearer")
         */
        String tokenType,

        /**
         * 액세스 토큰 만료 시간 (초)
         */
        long expiresIn,

        /**
         * 회원 도메인 식별자 (UUID)
         */
        String memberId,

        /**
         * 신규 가입 여부 (true이면 온보딩 화면으로 전환)
         */
        boolean isNewUser,

        /**
         * 온보딩 완료 여부
         */
        boolean onboardingCompleted,

        /**
         * 회원 닉네임
         */
        String nickname
) {
}
