package com.unicorn.lunchpick.member.service;

import com.unicorn.lunchpick.member.config.oauth2.KakaoOAuthClient;
import com.unicorn.lunchpick.member.config.oauth2.KakaoProfile;
import com.unicorn.lunchpick.member.dto.request.KakaoLoginRequest;
import com.unicorn.lunchpick.member.dto.response.AuthResponse;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import com.unicorn.lunchpick.member.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * AuthService 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenValiditySeconds", 1800L);
    }

    @Test
    @DisplayName("기존 회원 로그인 시 isNewUser=false로 200 응답을 반환한다")
    void kakaoLogin_existingMember_returnsIsNewUserFalse() {
        // given
        KakaoLoginRequest request = new KakaoLoginRequest("auth-code-123");
        KakaoProfile profile = new KakaoProfile("kakao-id-1", "test@example.com", "테스터");
        MemberEntity existingMember = MemberEntity.builder()
                .memberId("member-uuid-1")
                .kakaoId("kakao-id-1")
                .email("test@example.com")
                .nickname("테스터")
                .onboardingCompleted(true)
                .locationEnabled(false)
                .recommendationAlert(true)
                .feedbackReminder(true)
                .build();

        given(kakaoOAuthClient.getAccessToken("auth-code-123")).willReturn("kakao-access-token");
        given(kakaoOAuthClient.getUserProfile("kakao-access-token")).willReturn(profile);
        given(memberRepository.findByKakaoId("kakao-id-1")).willReturn(Optional.of(existingMember));
        given(tokenService.generateAccessToken("member-uuid-1", "ROLE_USER")).willReturn("jwt-token");

        // when
        AuthResponse response = authService.kakaoLogin(request);

        // then
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.memberId()).isEqualTo("member-uuid-1");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.onboardingCompleted()).isTrue();
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("신규 회원 가입 시 isNewUser=true로 회원을 DB에 저장한다")
    void kakaoLogin_newMember_savesAndReturnsIsNewUserTrue() {
        // given
        KakaoLoginRequest request = new KakaoLoginRequest("auth-code-456");
        KakaoProfile profile = new KakaoProfile("kakao-id-2", "new@example.com", "새유저");

        given(kakaoOAuthClient.getAccessToken("auth-code-456")).willReturn("kakao-access-token-2");
        given(kakaoOAuthClient.getUserProfile("kakao-access-token-2")).willReturn(profile);
        given(memberRepository.findByKakaoId("kakao-id-2")).willReturn(Optional.empty());
        given(memberRepository.save(any(MemberEntity.class))).willAnswer(inv -> inv.getArgument(0));
        given(tokenService.generateAccessToken(anyString(), anyString())).willReturn("jwt-token-new");

        // when
        AuthResponse response = authService.kakaoLogin(request);

        // then
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.accessToken()).isEqualTo("jwt-token-new");
        then(memberRepository).should().save(any(MemberEntity.class));
    }

    @Test
    @DisplayName("신규 회원의 닉네임이 null이면 기본값 '런치픽유저'로 저장된다")
    void kakaoLogin_newMemberWithNullNickname_usesDefaultNickname() {
        // given
        KakaoLoginRequest request = new KakaoLoginRequest("auth-code-789");
        KakaoProfile profile = new KakaoProfile("kakao-id-3", "noname@example.com", null);

        given(kakaoOAuthClient.getAccessToken("auth-code-789")).willReturn("kakao-access-token-3");
        given(kakaoOAuthClient.getUserProfile("kakao-access-token-3")).willReturn(profile);
        given(memberRepository.findByKakaoId("kakao-id-3")).willReturn(Optional.empty());
        given(memberRepository.save(any(MemberEntity.class))).willAnswer(inv -> {
            MemberEntity saved = inv.getArgument(0);
            assertThat(saved.getNickname()).isEqualTo("런치픽유저");
            return saved;
        });
        given(tokenService.generateAccessToken(anyString(), anyString())).willReturn("jwt-token-3");

        // when
        AuthResponse response = authService.kakaoLogin(request);

        // then
        assertThat(response.isNewUser()).isTrue();
    }
}
