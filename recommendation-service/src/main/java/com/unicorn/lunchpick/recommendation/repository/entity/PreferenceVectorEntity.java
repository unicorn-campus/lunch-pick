package com.unicorn.lunchpick.recommendation.repository.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 취향 벡터 스냅샷 JPA 엔티티
 *
 * <p>피드백 학습 시점의 취향 벡터 상태를 스냅샷으로 저장합니다.
 * 학습 이력 추적 및 롤백에 활용됩니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_recommendation.preference_vector</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "preference_vector",
        indexes = {
                @Index(name = "idx_preference_vector_member_calculated", columnList = "member_id, calculated_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceVectorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 회원 식별자 (논리적 연결) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 취향 벡터 JSON (JSONB) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vector_json", nullable = false, columnDefinition = "jsonb")
    private String vectorJson;

    /** 벡터 계산 시점 피드백 수 */
    @Column(name = "feedback_count", nullable = false)
    private int feedbackCount = 0;

    /** 콜드스타트 여부 */
    @Column(name = "is_cold_start", nullable = false)
    private boolean isColdStart = true;

    /** 계산 일시 */
    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    /**
     * 빌더를 통한 엔티티 생성
     */
    @Builder
    public PreferenceVectorEntity(String memberId, String vectorJson,
                                   int feedbackCount, boolean isColdStart) {
        this.memberId = memberId;
        this.vectorJson = vectorJson;
        this.feedbackCount = feedbackCount;
        this.isColdStart = isColdStart;
    }
}
