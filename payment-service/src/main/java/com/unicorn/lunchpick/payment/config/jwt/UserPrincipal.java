package com.unicorn.lunchpick.payment.config.jwt;

import lombok.Builder;
import lombok.Getter;

import java.security.Principal;

/**
 * JWT 인증 사용자 정보 VO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Getter
@Builder
public class UserPrincipal implements Principal {

    /** 회원 도메인 식별자 (UUID) */
    private final String userId;

    /** 사용자명 (memberId와 동일) */
    private final String username;

    /** 권한 */
    private final String authority;

    @Override
    public String getName() {
        return username;
    }
}
