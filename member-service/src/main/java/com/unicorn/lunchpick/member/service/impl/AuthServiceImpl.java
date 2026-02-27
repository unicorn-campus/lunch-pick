package com.unicorn.lunchpick.member.service.impl;

import com.unicorn.lunchpick.member.config.oauth2.KakaoOAuthClient;
import com.unicorn.lunchpick.member.config.oauth2.KakaoProfile;
import com.unicorn.lunchpick.member.dto.request.KakaoLoginRequest;
import com.unicorn.lunchpick.member.dto.response.AuthResponse;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import com.unicorn.lunchpick.member.service.AuthService;
import com.unicorn.lunchpick.member.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 인증 서비스 구현체
 *
 * <p>카카오 인증코드를 받아 카카오 사용자 정보를 조회하고,
 * 기존 회원이면 로그인, 신규 회원이면 자동 가입 후 자체 JWT를 발급합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String TOKEN_TYPE = "Bearer";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    @Value("${jwt.access-token-validity:1800}")
    private long accessTokenValiditySeconds;

    /**
     * {@inheritDoc}
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>카카오 인증코드 → 카카오 액세스 토큰 교환</li>
     *   <li>카카오 액세스 토큰 → 카카오 사용자 프로파일 조회</li>
     *   <li>kakaoId로 기존 회원 조회:
     *     <ul>
     *       <li>기존 회원: kakaoId 동기화 → 로그인</li>
     *       <li>신규 회원: 회원 엔티티 생성 → 가입</li>
     *     </ul>
     *   </li>
     *   <li>자체 JWT 발급 및 응답 반환</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthResponse kakaoLogin(KakaoLoginRequest request) {
        String kakaoAccessToken = kakaoOAuthClient.getAccessToken(request.authorizationCode());
        KakaoProfile profile = kakaoOAuthClient.getUserProfile(kakaoAccessToken);

        Optional<MemberEntity> existingMember = memberRepository.findByKakaoId(profile.kakaoId());

        boolean isNewUser;
        MemberEntity member;

        if (existingMember.isPresent()) {
            member = existingMember.get();
            member.updateKakaoId(profile.kakaoId());
            isNewUser = false;
            log.info("기존 회원 로그인 — memberId: {}", member.getMemberId());
        } else {
            member = MemberEntity.builder()
                    .memberId(UUID.randomUUID().toString())
                    .kakaoId(profile.kakaoId())
                    .email(profile.email())
                    .nickname(profile.nickname() != null ? profile.nickname() : "런치픽유저")
                    .onboardingCompleted(false)
                    .locationEnabled(false)
                    .recommendationAlert(true)
                    .feedbackReminder(true)
                    .build();
            memberRepository.save(member);
            isNewUser = true;
            log.info("신규 회원 가입 — memberId: {}", member.getMemberId());
        }

        String accessToken = tokenService.generateAccessToken(member.getMemberId(), ROLE_USER);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(accessTokenValiditySeconds)
                .memberId(member.getMemberId())
                .isNewUser(isNewUser)
                .onboardingCompleted(member.isOnboardingCompleted())
                .build();
    }
}
