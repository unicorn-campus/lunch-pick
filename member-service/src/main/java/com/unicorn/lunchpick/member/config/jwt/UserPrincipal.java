package com.unicorn.lunchpick.member.config.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증된 사용자 정보 Principal 객체
 *
 * <p>JWT 토큰에서 추출된 사용자 식별 정보를 담습니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Getter
@Builder
@RequiredArgsConstructor
public class UserPrincipal {

    /**
     * 회원 도메인 식별자 (UUID 문자열)
     */
    private final String userId;

    /**
     * 사용자명 (닉네임)
     */
    private final String username;

    /**
     * 권한 (USER / ADMIN)
     */
    private final String authority;

    /**
     * 사용자 ID 반환 (Spring Security 호환)
     *
     * @return 사용자 ID
     */
    public String getName() {
        return userId;
    }

    /**
     * 일반 사용자 여부 확인
     *
     * @return USER 권한이면 true
     */
    public boolean isUser() {
        return "USER".equals(authority) || authority == null;
    }
}
