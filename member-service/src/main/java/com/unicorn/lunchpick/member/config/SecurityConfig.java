package com.unicorn.lunchpick.member.config;

import com.unicorn.lunchpick.common.util.JwtTokenProvider;
import com.unicorn.lunchpick.member.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 *
 * <p>런치픽 회원 서비스의 JWT 기반 인증 및 API 보안 설정입니다.</p>
 *
 * <p><b>공개 경로:</b></p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/kakao} — 카카오 소셜 로그인 (인증 불필요)</li>
 *   <li>{@code /actuator/**} — 헬스체크 및 메트릭</li>
 *   <li>{@code /swagger-ui/**}, {@code /v3/api-docs/**} — Swagger UI</li>
 *   <li>{@code /internal/**} — 서비스 간 내부 API (VPC 내부에서만 접근)</li>
 * </ul>
 *
 * <p><b>CORS:</b> {@code cors.allowed-origins} 환경변수로 허용 도메인 제어</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * CORS 허용 오리진 목록 (환경변수 주입)
     */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Spring Security 필터 체인 설정
     *
     * <p><b>보안 정책:</b></p>
     * <ul>
     *   <li>CSRF 비활성화 (JWT Stateless 방식)</li>
     *   <li>세션 비활성화 (SessionCreationPolicy.STATELESS)</li>
     *   <li>JWT 인증 필터 등록</li>
     * </ul>
     *
     * @param http HttpSecurity 빌더
     * @return SecurityFilterChain
     * @throws Exception 보안 설정 오류
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/kakao").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CORS 설정 소스 빈 등록
     *
     * <p>허용 오리진은 {@code cors.allowed-origins} 환경변수로 주입됩니다.
     * 운영 환경에서는 {@code CORS_ALLOWED_ORIGINS} 환경변수로 제어합니다.</p>
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
