package com.unicorn.lunchpick.member.service;

import com.unicorn.lunchpick.member.dto.request.DietaryRestrictionsRequest;
import com.unicorn.lunchpick.member.dto.request.LocationConsentRequest;
import com.unicorn.lunchpick.member.dto.request.UpdateProfileRequest;
import com.unicorn.lunchpick.member.dto.response.DietaryRestrictionsResponse;
import com.unicorn.lunchpick.member.dto.response.LocationConsentResponse;
import com.unicorn.lunchpick.member.dto.response.MemberProfileResponse;
import com.unicorn.lunchpick.member.dto.response.SubscriptionStatusResponse;
import com.unicorn.lunchpick.member.dto.response.TasteProfileResponse;

/**
 * 회원 프로필 관리 서비스 인터페이스
 *
 * <p>회원 프로필 조회/수정, 위치 동의, 식이 제한 설정, 구독 상태 조회를 담당합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface MemberService {

    /**
     * 회원 프로필 조회
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @return 회원 프로필 응답 DTO
     */
    MemberProfileResponse getProfile(String memberId);

    /**
     * 회원 프로필 수정 (닉네임, 알림 설정)
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @param request  프로필 수정 요청
     * @return 수정된 회원 프로필 응답 DTO
     */
    MemberProfileResponse updateProfile(String memberId, UpdateProfileRequest request);

    /**
     * 위치 정보 동의 처리
     *
     * <p>동의 이력은 INSERT 전용으로 관리되며, 회원의 {@code locationEnabled} 상태도 함께 갱신됩니다.</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @param request  위치 동의 요청
     * @return 위치 동의 처리 결과
     */
    LocationConsentResponse updateLocationConsent(String memberId, LocationConsentRequest request);

    /**
     * 식이 제한 정보 설정
     *
     * <p>민감 정보이므로 {@code healthInfoConsentGiven}이 반드시 true여야 합니다.</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @param request  식이 제한 설정 요청
     * @return 식이 제한 설정 결과
     */
    DietaryRestrictionsResponse updateDietaryRestrictions(String memberId, DietaryRestrictionsRequest request);

    /**
     * 구독 상태 조회
     *
     * <p>Redis 캐시에서 구독 정보를 우선 조회하며, 캐시 미스 시 기본값(FREE 플랜)을 반환합니다.</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @return 구독 상태 응답 DTO
     */
    SubscriptionStatusResponse getSubscriptionStatus(String memberId);

    /**
     * 내부 API: 취향 프로파일 조회 (추천 서비스용)
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @return 취향 프로파일 응답 DTO
     */
    TasteProfileResponse getTasteProfile(String memberId);

    /**
     * 구독 상태 캐시 갱신 (MQ 이벤트 수신 시 호출)
     *
     * <p>expiresAt은 Redis에 ISO 8601 문자열로 저장됩니다.</p>
     *
     * @param memberId  회원 도메인 식별자 (UUID)
     * @param plan      구독 플랜 이름 (예: "PREMIUM")
     * @param expiresAt 구독 만료 일시 문자열 (ISO 8601, nullable)
     */
    void updateSubscriptionCache(String memberId, String plan, String expiresAt);
}
