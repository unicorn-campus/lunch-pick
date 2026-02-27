package com.unicorn.lunchpick.member.service;

import com.unicorn.lunchpick.member.dto.request.OnboardingProgressRequest;
import com.unicorn.lunchpick.member.dto.request.OnboardingRequest;
import com.unicorn.lunchpick.member.dto.response.OnboardingProgressSaveResponse;
import com.unicorn.lunchpick.member.dto.response.OnboardingResponse;

/**
 * 취향 온보딩 서비스 인터페이스
 *
 * <p>음식 카드 스와이프 기반 취향 온보딩 퀴즈 제출 및 진행 상태 저장을 담당합니다.</p>
 *
 * <p><b>온보딩 완료 조건:</b></p>
 * <ul>
 *   <li>스와이프 결과 7장 이상</li>
 *   <li>건강 정보 수집 동의 완료</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface OnboardingService {

    /**
     * 취향 온보딩 퀴즈 제출 (완료)
     *
     * <p>스와이프 결과가 7장 미만이면 {@code INSUFFICIENT_SWIPES} 예외가 발생합니다.
     * 성공 시 취향 벡터를 생성하고 회원의 온보딩 완료 상태를 갱신합니다.</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @param request  온보딩 퀴즈 제출 요청
     * @return 온보딩 완료 응답 (topCategories 포함)
     */
    OnboardingResponse completeOnboarding(String memberId, OnboardingRequest request);

    /**
     * 온보딩 진행 상태 임시 저장
     *
     * <p>Redis에 중간 진행 결과를 저장합니다. TTL: 24시간</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @param request  온보딩 진행 상태 요청
     * @return 임시 저장 결과 응답
     */
    OnboardingProgressSaveResponse saveOnboardingProgress(String memberId, OnboardingProgressRequest request);
}
