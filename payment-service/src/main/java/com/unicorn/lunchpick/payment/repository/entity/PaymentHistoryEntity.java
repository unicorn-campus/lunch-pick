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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 결제 이력 엔티티 — INSERT ONLY
 *
 * <p>전자상거래법에 따라 결제 이력은 5년간 보존됩니다.
 * 애플리케이션 레벨에서 UPDATE/DELETE를 금지합니다.
 * update() 및 delete() 메서드를 제공하지 않습니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(
        name = "payment_history",
        schema = "lunchpick_payment",
        indexes = {
                @Index(name = "idx_payment_member_requested", columnList = "member_id, requested_at DESC"),
                @Index(name = "idx_payment_subscription", columnList = "subscription_id"),
                @Index(name = "idx_payment_status_requested", columnList = "status, requested_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 도메인 식별자 (UUID) */
    @Column(name = "payment_id", nullable = false, unique = true, length = 36)
    private String paymentId;

    /** 회원 식별자 (논리적 연결, FK 없음) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 구독 식별자 (논리적 연결, FK 없음) */
    @Column(name = "subscription_id", nullable = false, length = 36)
    private String subscriptionId;

    /** 구독 플랜 식별자 */
    @Column(name = "plan_id", nullable = false, length = 30)
    private String planId;

    /** 결제 금액 (원, 0 이상) */
    @Column(name = "amount", nullable = false)
    private int amount;

    /** 결제 상태 (PENDING / SUCCESS / FAILED / CANCELLED) */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** PG사 거래 ID */
    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    /** 오류 코드 (결제 실패 시) */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /** 자동 갱신 동의 (전자상거래법 준수) */
    @Column(name = "auto_renewal_agreed", nullable = false)
    @Builder.Default
    private boolean autoRenewalAgreed = false;

    /** 청약 철회 권리 인지 확인 (전자상거래법 준수) */
    @Column(name = "withdrawal_right_acknowledged", nullable = false)
    @Builder.Default
    private boolean withdrawalRightAcknowledged = false;

    /** 청약 철회 기한 (결제일 + 7일) */
    @Column(name = "withdrawal_deadline")
    private LocalDateTime withdrawalDeadline;

    /** 결제 요청 일시 */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** 결제 승인 일시 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // INSERT ONLY: update(), delete() 메서드 제공하지 않음
}
