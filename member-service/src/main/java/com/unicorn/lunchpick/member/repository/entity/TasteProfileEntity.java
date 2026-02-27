package com.unicorn.lunchpick.member.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 취향 프로파일 JPA 엔티티
 *
 * <p>회원의 취향 벡터와 콜드스타트 상태를 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_member.taste_profile</p>
 *
 * <p><b>주의사항:</b></p>
 * <ul>
 *   <li>{@code memberId}는 FK 없이 논리적 연결만 사용합니다.</li>
 *   <li>{@code tasteVector}는 JSONB 타입으로 카테고리별 가중치 맵을 저장합니다.</li>
 *   <li>피드백 5건 미만 시 {@code isColdStart}가 true입니다.</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "taste_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TasteProfileEntity {

    /**
     * 내부 PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 회원 식별자 (논리적 연결, FK 없음)
     */
    @Column(name = "member_id", nullable = false, unique = true, length = 36)
    private String memberId;

    /**
     * 취향 벡터 JSON (카테고리별 가중치, JSONB)
     *
     * <p>예시: {"한식": 0.85, "일식": 0.70, "중식": 0.40}</p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "taste_vector", columnDefinition = "jsonb")
    private String tasteVector;

    /**
     * 누적 피드백 수 (5건 미만 시 콜드스타트)
     */
    @Column(name = "feedback_count", nullable = false)
    private int feedbackCount = 0;

    /**
     * 콜드스타트 여부 (feedbackCount < 5)
     */
    @Column(name = "is_cold_start", nullable = false)
    private boolean isColdStart = true;

    /**
     * 갱신일시 (취향 벡터 갱신 시 자동 업데이트)
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 빌더를 통한 엔티티 생성
     *
     * @param memberId      회원 식별자
     * @param tasteVector   취향 벡터 JSON 문자열
     * @param feedbackCount 누적 피드백 수
     * @param isColdStart   콜드스타트 여부
     */
    @Builder
    public TasteProfileEntity(String memberId, String tasteVector, int feedbackCount, boolean isColdStart) {
        this.memberId = memberId;
        this.tasteVector = tasteVector;
        this.feedbackCount = feedbackCount;
        this.isColdStart = isColdStart;
    }

    /**
     * 취향 벡터 갱신
     *
     * @param tasteVector   새 취향 벡터 JSON
     * @param feedbackCount 갱신된 피드백 수
     */
    public void updateTasteVector(String tasteVector, int feedbackCount) {
        this.tasteVector = tasteVector;
        this.feedbackCount = feedbackCount;
        this.isColdStart = feedbackCount < 5;
    }

    /**
     * 피드백 수 증가
     */
    public void incrementFeedbackCount() {
        this.feedbackCount++;
        if (this.feedbackCount >= 5) {
            this.isColdStart = false;
        }
    }
}
