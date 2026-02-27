package com.unicorn.lunchpick.payment.pg;

import com.unicorn.lunchpick.payment.dto.request.PaymentMethodDto;
import com.unicorn.lunchpick.payment.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Mock PG 게이트웨이 구현체
 *
 * <p>실제 PG 연동 없이 카드 유효성 검증을 수행하는 테스트용 구현체입니다.</p>
 *
 * <p><b>카드 번호별 처리 규칙:</b></p>
 * <ul>
 *   <li>{@code 0000-0000-0000-0000} → {@code INVALID_PAYMENT_INFO} (유효하지 않은 카드)</li>
 *   <li>{@code 4000-0000-0000-0002} → {@code PAYMENT_FAILED} (결제 거절)</li>
 *   <li>만료된 카드 (expiryYear/expiryMonth 기준) → {@code INVALID_PAYMENT_INFO} (만료 카드)</li>
 *   <li>나머지 → 결제 승인 성공</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Component
public class MockPgGateway implements PgGateway {

    private static final String INVALID_CARD_NUMBER = "0000-0000-0000-0000";
    private static final String DECLINED_CARD_NUMBER = "4000-0000-0000-0002";

    /**
     * {@inheritDoc}
     *
     * <p>카드 번호 패턴 및 유효기간을 검증한 후 Mock 트랜잭션 ID를 반환합니다.</p>
     */
    @Override
    public String approve(PaymentMethodDto paymentMethod, int amount, String orderId) {
        String cardNumber = paymentMethod.cardNumber();

        // 유효하지 않은 카드 번호
        if (INVALID_CARD_NUMBER.equals(cardNumber)) {
            log.warn("Mock PG: 유효하지 않은 카드 번호 — orderId: {}", orderId);
            throw PaymentException.invalidPaymentInfo();
        }

        // 만료 카드 검증
        if (isCardExpired(paymentMethod.expiryYear(), paymentMethod.expiryMonth())) {
            log.warn("Mock PG: 만료된 카드 — orderId: {}, expiryYear: {}, expiryMonth: {}",
                    orderId, paymentMethod.expiryYear(), paymentMethod.expiryMonth());
            throw PaymentException.invalidPaymentInfo();
        }

        // 결제 거절 테스트 카드
        if (DECLINED_CARD_NUMBER.equals(cardNumber)) {
            log.warn("Mock PG: 결제 거절 카드 — orderId: {}", orderId);
            throw PaymentException.paymentFailed();
        }

        String pgTransactionId = "pg-txn-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Mock PG: 결제 승인 완료 — orderId: {}, amount: {}, pgTransactionId: {}",
                orderId, amount, pgTransactionId);
        return pgTransactionId;
    }

    /**
     * 카드 만료 여부 확인
     *
     * @param expiryYear  유효기간 연도
     * @param expiryMonth 유효기간 월
     * @return 만료된 경우 true
     */
    private boolean isCardExpired(Integer expiryYear, Integer expiryMonth) {
        if (expiryYear == null || expiryMonth == null) {
            return true;
        }
        YearMonth cardExpiry = YearMonth.of(expiryYear, expiryMonth);
        YearMonth now = YearMonth.now();
        return cardExpiry.isBefore(now);
    }
}
