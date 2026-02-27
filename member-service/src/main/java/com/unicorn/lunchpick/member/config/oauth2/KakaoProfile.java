package com.unicorn.lunchpick.member.config.oauth2;

import lombok.Builder;

/**
 * 카카오 사용자 프로파일 DTO
 *
 * <p>카카오 API에서 조회한 사용자 정보를 담는 객체입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record KakaoProfile(

        /**
         * 카카오 회원 고유 ID
         */
        String kakaoId,

        /**
         * 카카오 연동 이메일
         */
        String email,

        /**
         * 카카오 닉네임
         */
        String nickname
) {
}
