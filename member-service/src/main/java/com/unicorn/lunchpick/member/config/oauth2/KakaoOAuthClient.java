package com.unicorn.lunchpick.member.config.oauth2;

import com.unicorn.lunchpick.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 카카오 OAuth2 API 클라이언트
 *
 * <p>카카오 인증 코드를 액세스 토큰으로 교환하고,
 * 카카오 사용자 프로파일을 조회하는 HTTP 클라이언트입니다.</p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>인증 코드 → 카카오 액세스 토큰 교환</li>
 *   <li>카카오 액세스 토큰으로 사용자 프로파일 조회</li>
 * </ul>
 *
 * <p><b>오류 처리:</b></p>
 * <ul>
 *   <li>카카오 인증 실패 시 {@link MemberException#kakaoAuthFailed()}</li>
 *   <li>카카오 서버 연결 실패 시 {@link MemberException#kakaoServiceUnavailable()}</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see KakaoOAuthConfig
 * @see KakaoProfile
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final KakaoOAuthConfig kakaoOAuthConfig;
    private final RestTemplate restTemplate;

    /**
     * 인증 코드로 카카오 액세스 토큰 교환
     *
     * <p><b>처리 과정:</b></p>
     * <ol>
     *   <li>카카오 토큰 발급 API 호출 (POST kauth.kakao.com/oauth/token)</li>
     *   <li>응답에서 액세스 토큰 추출</li>
     * </ol>
     *
     * @param authorizationCode 카카오 OAuth 2.0 인증 코드
     * @return 카카오 액세스 토큰
     * @throws MemberException 카카오 인증 실패 또는 서버 연결 실패 시
     */
    @SuppressWarnings("unchecked")
    public String getAccessToken(String authorizationCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", kakaoOAuthConfig.getClientId());
            params.add("client_secret", kakaoOAuthConfig.getClientSecret());
            params.add("redirect_uri", kakaoOAuthConfig.getRedirectUri());
            params.add("code", authorizationCode);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    KakaoOAuthConfig.TOKEN_URI,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("access_token")) {
                log.warn("카카오 토큰 응답에 access_token 없음: {}", body);
                throw MemberException.kakaoAuthFailed();
            }

            return (String) body.get("access_token");

        } catch (HttpClientErrorException e) {
            log.warn("카카오 토큰 교환 실패 — HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw MemberException.kakaoAuthFailed();
        } catch (ResourceAccessException e) {
            log.error("카카오 서버 연결 실패: {}", e.getMessage());
            throw MemberException.kakaoServiceUnavailable();
        } catch (MemberException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 토큰 교환 중 예기치 않은 오류: {}", e.getMessage(), e);
            throw MemberException.kakaoAuthFailed();
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 프로파일 조회
     *
     * <p><b>처리 과정:</b></p>
     * <ol>
     *   <li>카카오 사용자 정보 API 호출 (GET kapi.kakao.com/v2/user/me)</li>
     *   <li>응답에서 카카오 ID, 이메일, 닉네임 추출</li>
     * </ol>
     *
     * @param kakaoAccessToken 카카오 액세스 토큰
     * @return 카카오 사용자 프로파일
     * @throws MemberException 프로파일 조회 실패 시
     */
    @SuppressWarnings("unchecked")
    public KakaoProfile getUserProfile(String kakaoAccessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(kakaoAccessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    KakaoOAuthConfig.USER_INFO_URI,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("카카오 사용자 정보 응답 본문 없음");
                throw MemberException.kakaoAuthFailed();
            }

            String kakaoId = String.valueOf(body.get("id"));

            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            String email = null;
            String nickname = null;

            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }

            if (nickname == null) {
                nickname = "런치픽 사용자";
            }

            return KakaoProfile.builder()
                    .kakaoId(kakaoId)
                    .email(email)
                    .nickname(nickname)
                    .build();

        } catch (HttpClientErrorException e) {
            log.warn("카카오 사용자 정보 조회 실패 — HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw MemberException.kakaoAuthFailed();
        } catch (ResourceAccessException e) {
            log.error("카카오 서버 연결 실패: {}", e.getMessage());
            throw MemberException.kakaoServiceUnavailable();
        } catch (MemberException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 중 예기치 않은 오류: {}", e.getMessage(), e);
            throw MemberException.kakaoAuthFailed();
        }
    }
}
