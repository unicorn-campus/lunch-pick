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

import java.time.LocalDateTime;

/**
 * 학습 완료 메시지 JPA 엔티티
 *
 * <p>취향 학습 완료 시 사용자에게 노출할 메시지를 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_recommendation.learning_message</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "learning_message",
        indexes = {
                @Index(name = "idx_learning_message_member_generated", columnList = "member_id, generated_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 회원 식별자 (논리적 연결) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 학습 완료 메시지 */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** 메시지 생성 일시 */
    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    /**
     * 빌더를 통한 엔티티 생성
     */
    @Builder
    public LearningMessageEntity(String memberId, String message) {
        this.memberId = memberId;
        this.message = message;
    }
}
