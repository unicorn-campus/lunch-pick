package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.util.JwtTokenProvider;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 테스트 전용 인증 컨트롤러 (dev 프로파일에서만 활성화)
 *
 * <p>OAuth2 소셜 로그인 없이 JWT 토큰을 직접 발급하여 API 테스트를 수행할 수 있도록 합니다.
 * 테스트 회원이 없으면 자동 생성합니다.</p>
 */
@Profile("dev")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    /**
     * 테스트용 JWT 토큰 발급 (회원 자동 생성)
     *
     * @param request {"nickname": "테스트유저"} — nickname만 있으면 회원 자동생성 후 토큰 발급
     * @return {"accessToken": "...", "memberId": "..."}
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> testLogin(
            @RequestBody Map<String, String> request) {
        String nickname = request.getOrDefault("nickname", "테스트유저");

        // 닉네임으로 기존 회원 조회, 없으면 자동 생성
        MemberEntity member = memberRepository.findByNickname(nickname)
                .orElseGet(() -> {
                    MemberEntity newMember = MemberEntity.builder()
                            .memberId(UUID.randomUUID().toString())
                            .kakaoId("test-kakao-" + System.currentTimeMillis())
                            .nickname(nickname)
                            .email(nickname.replaceAll("\\s+", "") + "@test.com")
                            .onboardingCompleted(false)
                            .build();
                    return memberRepository.save(newMember);
                });

        String token = jwtTokenProvider.generateAccessToken(member.getMemberId(), "ROLE_USER");
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "memberId", member.getMemberId()
        ));
    }
}
