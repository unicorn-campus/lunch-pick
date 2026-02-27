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
 * 피드백 JPA 엔티티
 *
 * <p>식사 기록에 대한 만족도 및 키워드 피드백을 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_recommendation.feedback</p>
 *
 * <p><b>주의사항:</b> meal_id는 UNIQUE — 식사 1건당 피드백 1건</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "feedback",
        indexes = {
                @Index(name = "idx_feedback_member_created", columnList = "member_id, created_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 도메인 식별자 (UUID) */
    @Column(name = "feedback_id", nullable = false, unique = true, length = 36)
    private String feedbackId;

    /** 회원 식별자 (논리적 연결) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 식사 기록 식별자 (1:1, UNIQUE) */
    @Column(name = "meal_id", nullable = false, unique = true, length = 36)
    private String mealId;

    /** 만족도 (GOOD / BAD / NEUTRAL) */
    @Column(name = "satisfaction", nullable = false, length = 10)
    private String satisfaction;

    /** 피드백 키워드 (TASTE / PRICE / KINDNESS, nullable) */
    @Column(name = "keyword", length = 20)
    private String keyword;

    /** 피드백 스킵 여부 */
    @Column(name = "skipped", nullable = false)
    private boolean skipped = false;

    /**
     * 빌더를 통한 엔티티 생성
     */
    @Builder
    public FeedbackEntity(String feedbackId, String memberId, String mealId,
                           String satisfaction, String keyword, boolean skipped) {
        this.feedbackId = feedbackId;
        this.memberId = memberId;
        this.mealId = mealId;
        this.satisfaction = satisfaction;
        this.keyword = keyword;
        this.skipped = skipped;
    }

    /** 피드백 수정 */
    public void update(String satisfaction, String keyword, boolean skipped) {
        this.satisfaction = satisfaction;
        this.keyword = keyword;
        this.skipped = skipped;
    }
}
