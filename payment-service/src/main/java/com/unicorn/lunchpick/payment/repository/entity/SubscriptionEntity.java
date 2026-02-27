package com.unicorn.lunchpick.payment.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 구독 정보 엔티티
 *
 * <p>회원의 현재 구독 상태를 관리합니다.
 * 구독 취소는 즉시 해지가 아닌 기간 종료 후 무료 전환(PENDING_CANCEL) 방식입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(
        name = "subscription",
        schema = "lunchpick_payment",
        indexes = {
                @Index(name = "idx_subscription_member_status", columnList = "member_id, status"),
                @Index(name = "idx_subscription_status_billing", columnList = "status, next_billing_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 도메인 식별자 (UUID) */
    @Column(name = "subscription_id", nullable = false, unique = true, length = 36)
    private String subscriptionId;

    /** 회원 식별자 (논리적 연결, FK 없음) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 구독 플랜 (FREE / PREMIUM_MONTHLY / PREMIUM_ANNUAL) */
    @Column(name = "plan_id", nullable = false, length = 30)
    private String planId;

    /** 구독 상태 (ACTIVE / PENDING_CANCEL / CANCELLED / EXPIRED) */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 구독 시작일시 */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** 다음 청구일시 */
    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    /** 현재 구독 기간 종료일 */
    @Column(name = "current_period_ends_at")
    private LocalDateTime currentPeriodEndsAt;

    /** 체험 연장 사용 여부 (회원당 1회 한정) */
    @Column(name = "trial_extension_used", nullable = false)
    @Builder.Default
    private boolean trialExtensionUsed = false;

    /** 취소 사유 (COST / NOT_USING / QUALITY / OTHER) */
    @Column(name = "cancel_reason", length = 20)
    private String cancelReason;

    /** 취소 사유 상세 */
    @Column(name = "cancel_reason_detail", length = 500)
    private String cancelReasonDetail;

    /** 취소 일시 */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 구독 해지 예약 처리
     *
     * <p>즉시 해지가 아닌 PENDING_CANCEL 상태로 전환하여 현재 기간 종료 후 무료 전환합니다.</p>
     *
     * @param cancelReason       취소 사유
     * @param cancelReasonDetail 취소 사유 상세
     */
    public void cancel(String cancelReason, String cancelReasonDetail) {
        this.status = "PENDING_CANCEL";
        this.cancelReason = cancelReason;
        this.cancelReasonDetail = cancelReasonDetail;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 체험 연장 처리 (7일)
     *
     * @param newPeriodEndsAt 연장된 기간 종료일
     */
    public void extendTrial(LocalDateTime newPeriodEndsAt) {
        this.trialExtensionUsed = true;
        this.currentPeriodEndsAt = newPeriodEndsAt;
        if (this.nextBillingAt != null) {
            this.nextBillingAt = newPeriodEndsAt.plusDays(1);
        }
    }
}
