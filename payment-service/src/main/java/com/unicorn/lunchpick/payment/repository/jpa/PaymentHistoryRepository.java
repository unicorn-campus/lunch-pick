package com.unicorn.lunchpick.payment.repository.jpa;

import com.unicorn.lunchpick.payment.repository.entity.PaymentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 결제 이력 JPA 레포지토리 — INSERT ONLY
 *
 * <p>결제 이력은 전자상거래법에 따라 5년간 보존합니다.
 * delete 메서드는 호출하지 않습니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, Long> {

    /**
     * 결제 ID로 결제 이력 조회
     *
     * @param paymentId 결제 도메인 식별자
     * @return 결제 이력
     */
    Optional<PaymentHistoryEntity> findByPaymentId(String paymentId);
}
