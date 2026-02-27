package com.unicorn.lunchpick.member.service.impl;

import com.unicorn.lunchpick.common.util.JwtTokenProvider;
import com.unicorn.lunchpick.member.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * JWT 토큰 관리 서비스 구현체
 *
 * <p>JWT 생성 및 Redis 블랙리스트를 통한 토큰 무효화를 구현합니다.</p>
 *
 * <p><b>Redis 키 패턴:</b> {@code jwt:blacklist:{token}} (TTL = 잔여 만료 시간)</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateAccessToken(String memberId, String role) {
        return jwtTokenProvider.generateAccessToken(memberId, role);
    }

    /**
     * {@inheritDoc}
     *
     * <p>잔여 만료 시간을 TTL로 설정하여 블랙리스트에 등록합니다.
     * 이미 만료된 토큰은 블랙리스트 등록을 건너뜁니다.</p>
     */
    @Override
    public void invalidateToken(String token) {
        try {
            Instant expiration = jwtTokenProvider.getExpiration(token);
            Duration ttl = Duration.between(Instant.now(), expiration);
            if (!ttl.isNegative() && !ttl.isZero()) {
                String key = BLACKLIST_KEY_PREFIX + token;
                stringRedisTemplate.opsForValue().set(key, "blacklisted", ttl);
                log.debug("JWT 블랙리스트 등록 완료 — TTL: {}초", ttl.getSeconds());
            }
        } catch (Exception ex) {
            log.warn("JWT 블랙리스트 등록 실패 (이미 만료된 토큰으로 추정)", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
