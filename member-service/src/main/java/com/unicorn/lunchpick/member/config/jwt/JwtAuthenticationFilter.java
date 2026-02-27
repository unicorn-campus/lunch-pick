package com.unicorn.lunchpick.member.config.jwt;

import com.unicorn.lunchpick.common.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 *
 * <p>HTTP 요청의 Authorization 헤더에서 JWT Bearer 토큰을 추출하여 인증을 수행합니다.</p>
 *
 * <p><b>처리 과정:</b></p>
 * <ol>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
 *   <li>{@link JwtTokenProvider}로 토큰 유효성 검증</li>
 *   <li>토큰 클레임에서 memberId, role 추출</li>
 *   <li>SecurityContextHolder에 인증 정보 설정</li>
 * </ol>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see JwtTokenProvider
 * @see UserPrincipal
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * JWT 토큰 검증 및 SecurityContext 인증 정보 설정
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException      IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                String memberId = claims.getSubject();
                String role = claims.get("role", String.class);

                if (StringUtils.hasText(memberId)) {
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                            .userId(memberId)
                            .username(memberId)
                            .authority(role != null ? role : "USER")
                            .build();

                    String authority = role != null ? role : "USER";
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 완료 — memberId: {}", memberId);
                }
            } catch (Exception e) {
                log.debug("JWT 파싱 중 오류: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 Bearer 토큰 추출
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 또는 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 필터 적용 제외 경로 설정
     *
     * <p>Actuator, Swagger, 인증 엔드포인트는 JWT 검증에서 제외합니다.</p>
     *
     * @param request HTTP 요청
     * @return 필터 적용 제외 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/health") ||
               path.equals("/api/v1/auth/kakao");
    }
}
