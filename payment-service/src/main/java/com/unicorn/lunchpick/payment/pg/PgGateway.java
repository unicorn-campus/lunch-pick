package com.unicorn.lunchpick.payment.pg;

import com.unicorn.lunchpick.payment.dto.request.PaymentMethodDto;

/**
 * PG(결제 대행사) 게이트웨이 인터페이스
 *
 * <p>실제 PG 연동 또는 Mock 구현체로 교체 가능한 추상화 계층입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface PgGateway {

    /**
     * 카드 결제 승인 요청
     *
     * <p>카드 유효성 검증 후 결제를 처리합니다.
     * 실패 시 {@link com.unicorn.lunchpick.payment.exception.PaymentException}을 던집니다.</p>
     *
     * @param paymentMethod 결제 수단 정보
     * @param amount        결제 금액 (원)
     * @param orderId       주문 식별자
     * @return PG 트랜잭션 ID
     */
    String approve(PaymentMethodDto paymentMethod, int amount, String orderId);
}
