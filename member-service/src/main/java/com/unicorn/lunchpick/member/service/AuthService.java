package com.unicorn.lunchpick.member.service;

import com.unicorn.lunchpick.member.dto.request.KakaoLoginRequest;
import com.unicorn.lunchpick.member.dto.response.AuthResponse;

/**
 * 인증 서비스 인터페이스
 *
 * <p>카카오 소셜 로그인을 통한 회원 인증 및 자체 JWT 발급을 담당합니다.</p>
 *
 * <p><b>로그인 플로우:</b></p>
 * <ol>
 *   <li>클라이언트 인증코드 수신</li>
 *   <li>카카오 액세스 토큰 교환 ({@code kauth.kakao.com})</li>
 *   <li>카카오 사용자 정보 조회 ({@code kapi.kakao.com})</li>
 *   <li>회원 조회 또는 신규 등록</li>
 *   <li>자체 JWT 발급 및 반환</li>
 * </ol>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface AuthService {

    /**
     * 카카오 소셜 로그인 처리
     *
     * <p>기존 회원이면 200 OK ({@code isNewUser: false}),
     * 신규 회원이면 201 Created ({@code isNewUser: true})를 반환합니다.</p>
     *
     * @param request 카카오 인증코드 요청
     * @return JWT 액세스 토큰 및 회원 정보
     */
    AuthResponse kakaoLogin(KakaoLoginRequest request);
}
