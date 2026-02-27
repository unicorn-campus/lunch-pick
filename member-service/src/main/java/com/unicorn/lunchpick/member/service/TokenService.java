package com.unicorn.lunchpick.member.service;

/**
 * JWT 토큰 관리 서비스 인터페이스
 *
 * <p>액세스 토큰 생성 및 Redis 기반 JWT 블랙리스트(로그아웃) 관리를 담당합니다.</p>
 *
 * <p><b>Redis DB 0 키 패턴:</b></p>
 * <ul>
 *   <li>{@code jwt:blacklist:{token}} — 로그아웃된 JWT 블랙리스트 (TTL = 토큰 잔여 만료 시간)</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface TokenService {

    /**
     * 회원 ID 기반 액세스 토큰 생성
     *
     * @param memberId 회원 도메인 식별자 (UUID 문자열)
     * @param role     권한 (예: "ROLE_USER")
     * @return 서명된 JWT 액세스 토큰
     */
    String generateAccessToken(String memberId, String role);

    /**
     * 토큰을 블랙리스트에 등록하여 무효화 (로그아웃)
     *
     * <p>잔여 만료 시간을 TTL로 설정하여 Redis에 저장합니다.</p>
     *
     * @param token 무효화할 JWT 토큰
     */
    void invalidateToken(String token);

    /**
     * 토큰 블랙리스트 여부 확인
     *
     * @param token 검사할 JWT 토큰
     * @return 블랙리스트에 존재하면 {@code true}
     */
    boolean isTokenBlacklisted(String token);
}
