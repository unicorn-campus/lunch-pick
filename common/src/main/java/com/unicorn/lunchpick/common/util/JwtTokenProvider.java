package com.unicorn.lunchpick.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 토큰 생성/파싱/검증 유틸리티
 *
 * <p>모든 서비스에서 동일한 시크릿 키와 설정으로 JWT를 처리합니다.
 * {@code application.yml}의 {@code jwt.secret} 환경변수를 공유하여 서비스 간 토큰 호환성을 보장합니다.</p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>액세스 토큰 생성 ({@link #generateAccessToken(String, String)})</li>
 *   <li>토큰 파싱 및 클레임 추출 ({@link #parseClaims(String)})</li>
 *   <li>토큰 유효성 검증 ({@link #validateToken(String)})</li>
 *   <li>토큰 만료 시각 조회 ({@link #getExpiration(String)})</li>
 * </ul>
 *
 * <p><b>주의사항:</b></p>
 * <ul>
 *   <li>JWT_SECRET 환경변수는 모든 서비스에서 동일한 값을 사용해야 합니다.</li>
 *   <li>시크릿 키는 최소 256비트(32자) 이상이어야 합니다.</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity:1800}") long accessTokenValiditySeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    /**
     * 액세스 토큰 생성
     *
     * @param subject  토큰 주체 (회원 ID 문자열)
     * @param role     회원 권한 (예: "ROLE_USER", "ROLE_PREMIUM")
     * @return 서명된 JWT 액세스 토큰 문자열
     */
    public String generateAccessToken(String subject, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 클레임 파싱
     *
     * <p>만료된 토큰의 경우 {@link ExpiredJwtException}이 발생합니다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return 파싱된 Claims 객체
     * @throws ExpiredJwtException     토큰이 만료된 경우
     * @throws MalformedJwtException   토큰 형식이 잘못된 경우
     * @throws SignatureException       서명 검증 실패 시
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 subject(회원 ID) 추출
     *
     * @param token JWT 토큰 문자열
     * @return subject (회원 ID 문자열)
     */
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 유효성 검증
     *
     * <p>서명 검증 + 만료 여부를 함께 확인합니다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return 유효하면 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException
                 | SignatureException | UnsupportedJwtException
                 | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰 만료 시각 조회
     *
     * @param token JWT 토큰 문자열
     * @return 만료 시각 ({@link Instant})
     */
    public Instant getExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param token JWT 토큰 문자열
     * @return 만료되었으면 {@code true}
     */
    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
