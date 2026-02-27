package com.unicorn.lunchpick.recommendation.repository.entity;

import com.unicorn.lunchpick.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 결과 JPA 엔티티
 *
 * <p>AI Pipeline이 생성한 추천 카드 1개를 1행으로 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_recommendation.recommendation</p>
 *
 * <p><b>상태 전이:</b> PENDING → ACCEPTED | REJECTED</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "recommendation",
        indexes = {
                @Index(name = "idx_recommendation_member_created", columnList = "member_id, created_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 도메인 식별자 (UUID) */
    @Column(name = "recommendation_id", nullable = false, unique = true, length = 36)
    private String recommendationId;

    /** 회원 식별자 (논리적 연결) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 식당 식별자 */
    @Column(name = "restaurant_id", nullable = false, length = 36)
    private String restaurantId;

    /** 식당명 */
    @Column(name = "restaurant_name", nullable = false, length = 200)
    private String restaurantName;

    /** 대표 메뉴 */
    @Column(name = "representative_menu", nullable = false, length = 200)
    private String representativeMenu;

    /** 추천 이유 요약 */
    @Column(name = "reason_summary", length = 500)
    private String reasonSummary;

    /** 신뢰도 점수 (0~100) */
    @Column(name = "confidence_score", nullable = false)
    private int confidenceScore = 0;

    /** 거리 (미터) */
    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters = 0;

    /** 도보 예상 소요 시간 (분) */
    @Column(name = "estimated_walk_minutes", nullable = false)
    private int estimatedWalkMinutes = 0;

    /** 음식 카테고리 */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 폴백 추천 여부 */
    @Column(name = "is_fallback", nullable = false)
    private boolean isFallback = false;

    /** 추천 상태 (PENDING / ACCEPTED / REJECTED) */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /** 사용자 반응 시간 (ms) */
    @Column(name = "reaction_time_ms")
    private Integer reactionTimeMs;

    /** 거절 사유 */
    @Column(name = "reject_reason", length = 30)
    private String rejectReason;

    /**
     * 빌더를 통한 엔티티 생성
     */
    @Builder
    public RecommendationEntity(String recommendationId, String memberId, String restaurantId,
                                 String restaurantName, String representativeMenu, String reasonSummary,
                                 int confidenceScore, int distanceMeters, int estimatedWalkMinutes,
                                 String category, boolean isFallback) {
        this.recommendationId = recommendationId;
        this.memberId = memberId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.representativeMenu = representativeMenu;
        this.reasonSummary = reasonSummary;
        this.confidenceScore = confidenceScore;
        this.distanceMeters = distanceMeters;
        this.estimatedWalkMinutes = estimatedWalkMinutes;
        this.category = category;
        this.isFallback = isFallback;
        this.status = "PENDING";
    }

    /**
     * 추천 수락 처리
     *
     * @param reactionTimeMs 수락까지 걸린 반응 시간 (ms)
     */
    public void accept(int reactionTimeMs) {
        this.status = "ACCEPTED";
        this.reactionTimeMs = reactionTimeMs;
    }

    /**
     * 추천 거절 처리
     *
     * @param rejectReason 거절 사유
     */
    public void reject(String rejectReason) {
        this.status = "REJECTED";
        this.rejectReason = rejectReason;
    }

    /**
     * 추천 이유 갱신
     *
     * @param reasonSummary 새 추천 이유 요약
     */
    public void updateReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }
}
