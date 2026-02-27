package com.unicorn.lunchpick.member.repository.entity;

import com.unicorn.lunchpick.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 JPA 엔티티
 *
 * <p>런치픽 서비스 회원의 기본 정보를 저장하는 엔티티입니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_member.member</p>
 *
 * <p><b>주요 필드:</b></p>
 * <ul>
 *   <li>{@code memberId} — 도메인 식별자 (UUID 문자열), 외부 공개용</li>
 *   <li>{@code kakaoId} — 카카오 OAuth ID, 유니크</li>
 *   <li>{@code email} — 카카오 연동 이메일</li>
 *   <li>{@code nickname} — 서비스 내 닉네임 (2~20자)</li>
 *   <li>{@code onboardingCompleted} — 취향 온보딩 완료 여부</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see com.unicorn.lunchpick.member.repository.jpa.MemberRepository
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity extends BaseTimeEntity {

    /**
     * 내부 PK (BIGSERIAL, auto increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 도메인 식별자 (UUID 문자열, 외부 공개)
     */
    @Column(name = "member_id", nullable = false, unique = true, length = 36)
    private String memberId;

    /**
     * 카카오 OAuth ID
     */
    @Column(name = "kakao_id", nullable = false, unique = true, length = 50)
    private String kakaoId;

    /**
     * 카카오 연동 이메일
     */
    @Column(name = "email", length = 200)
    private String email;

    /**
     * 서비스 내 닉네임 (2~20자)
     */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /**
     * 취향 온보딩 완료 여부
     */
    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    /**
     * 위치 정보 사용 동의 여부
     */
    @Column(name = "location_enabled", nullable = false)
    private boolean locationEnabled = false;

    /**
     * 추천 알림 수신 여부
     */
    @Column(name = "recommendation_alert", nullable = false)
    private boolean recommendationAlert = true;

    /**
     * 피드백 리마인더 수신 여부
     */
    @Column(name = "feedback_reminder", nullable = false)
    private boolean feedbackReminder = true;

    /**
     * 빌더를 통한 엔티티 생성
     *
     * @param memberId            도메인 식별자 (UUID)
     * @param kakaoId             카카오 OAuth ID
     * @param email               이메일
     * @param nickname            닉네임
     * @param onboardingCompleted 온보딩 완료 여부
     * @param locationEnabled     위치 동의 여부
     * @param recommendationAlert 추천 알림 여부
     * @param feedbackReminder    피드백 리마인더 여부
     */
    @Builder
    public MemberEntity(String memberId, String kakaoId, String email, String nickname,
                        boolean onboardingCompleted, boolean locationEnabled,
                        boolean recommendationAlert, boolean feedbackReminder) {
        this.memberId = memberId;
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
        this.onboardingCompleted = onboardingCompleted;
        this.locationEnabled = locationEnabled;
        this.recommendationAlert = recommendationAlert;
        this.feedbackReminder = feedbackReminder;
    }

    /**
     * 카카오 ID 갱신 (기존 회원 로그인 시 최신 카카오 ID 동기화)
     *
     * @param kakaoId 갱신할 카카오 OAuth ID
     */
    public void updateKakaoId(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    /**
     * 온보딩 완료 처리
     */
    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    /**
     * 위치 동의 상태 갱신
     *
     * @param locationEnabled 위치 동의 여부
     */
    public void updateLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
    }

    /**
     * 프로필 정보 수정
     *
     * @param nickname            닉네임
     * @param recommendationAlert 추천 알림 여부
     * @param feedbackReminder    피드백 리마인더 여부
     */
    public void updateProfile(String nickname, boolean recommendationAlert, boolean feedbackReminder) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        this.recommendationAlert = recommendationAlert;
        this.feedbackReminder = feedbackReminder;
    }
}
