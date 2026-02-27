package com.unicorn.lunchpick.payment.exception;

import com.unicorn.lunchpick.common.exception.BusinessException;

/**
 * 결제 서비스 도메인 예외
 *
 * <p>팩토리 메서드 패턴으로 예외 생성을 중앙화합니다.</p>
 *
 * <p><b>주요 에러 코드:</b></p>
 * <ul>
 *   <li>{@code SUBSCRIPTION_NOT_FOUND} — 구독 정보 없음</li>
 *   <li>{@code SUBSCRIPTION_ALREADY_ACTIVE} — 이미 활성 구독 존재</li>
 *   <li>{@code PAYMENT_FAILED} — PG 결제 실패</li>
 *   <li>{@code INVALID_PAYMENT_INFO} — 결제 정보 유효성 오류</li>
 *   <li>{@code TRIAL_EXTENSION_ALREADY_USED} — 무료 연장 이미 사용</li>
 *   <li>{@code DUPLICATE_PAYMENT_LOCK} — 중복 결제 방지 Lock</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public class PaymentException extends BusinessException {

    public PaymentException(String errorCode, String message) {
        super(errorCode, message);
    }

    /** 구독 정보를 찾을 수 없음 */
    public static PaymentException subscriptionNotFound() {
        return new PaymentException("SUBSCRIPTION_NOT_FOUND", "활성 구독 정보를 찾을 수 없습니다.");
    }

    /** 이미 활성 구독 존재 */
    public static PaymentException subscriptionAlreadyActive() {
        return new PaymentException("SUBSCRIPTION_ALREADY_ACTIVE", "이미 프리미엄 구독이 활성화되어 있어요.");
    }

    /** PG 결제 실패 */
    public static PaymentException paymentFailed() {
        return new PaymentException("PAYMENT_FAILED", "결제가 실패했어요. 다른 결제 수단을 시도해주세요.");
    }

    /** 결제 정보 유효성 오류 */
    public static PaymentException invalidPaymentInfo() {
        return new PaymentException("INVALID_PAYMENT_INFO", "카드 정보를 다시 확인해주세요.");
    }

    /** 무료 연장 이미 사용 */
    public static PaymentException trialExtensionAlreadyUsed() {
        return new PaymentException("TRIAL_EXTENSION_ALREADY_USED", "무료 연장은 1회만 사용 가능해요.");
    }

    /** 중복 결제 방지 Lock 획득 실패 */
    public static PaymentException duplicatePaymentLock() {
        return new PaymentException("DUPLICATE_PAYMENT_LOCK", "결제가 이미 진행 중이에요. 잠시 후 다시 시도해주세요.");
    }
}
