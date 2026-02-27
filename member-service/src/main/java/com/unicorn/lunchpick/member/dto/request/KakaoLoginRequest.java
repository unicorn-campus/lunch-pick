package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 소셜 로그인 요청 DTO
 *
 * <p>카카오 OAuth 2.0 인증 코드를 포함한 로그인 요청입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record KakaoLoginRequest(

        /**
         * 카카오 OAuth 2.0 인증 코드
         */
        @NotBlank(message = "카카오 인증 코드는 필수입니다.")
        String authorizationCode
) {
}
