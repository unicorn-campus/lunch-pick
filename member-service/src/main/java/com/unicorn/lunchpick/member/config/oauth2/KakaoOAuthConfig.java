package com.unicorn.lunchpick.member.config.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 카카오 OAuth2 설정
 *
 * <p>카카오 OAuth2 인증에 필요한 클라이언트 ID, Secret, Redirect URI 설정을 제공합니다.</p>
 *
 * <p><b>환경변수:</b></p>
 * <ul>
 *   <li>{@code KAKAO_CLIENT_ID} — 카카오 앱 클라이언트 ID</li>
 *   <li>{@code KAKAO_CLIENT_SECRET} — 카카오 앱 클라이언트 Secret</li>
 *   <li>{@code KAKAO_REDIRECT_URI} — 인증 후 리다이렉트 URI</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class KakaoOAuthConfig {

    /**
     * 카카오 OAuth2 토큰 발급 URI
     */
    public static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";

    /**
     * 카카오 사용자 정보 조회 URI
     */
    public static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.client-secret}")
    private String clientSecret;

    @Value("${kakao.oauth.redirect-uri}")
    private String redirectUri;

    /**
     * 카카오 앱 클라이언트 ID 반환
     *
     * @return 클라이언트 ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 카카오 앱 클라이언트 Secret 반환
     *
     * @return 클라이언트 Secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * 인증 후 리다이렉트 URI 반환
     *
     * @return 리다이렉트 URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }
}
