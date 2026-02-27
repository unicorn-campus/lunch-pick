package com.unicorn.lunchpick.common.exception;

/**
 * 충돌 예외
 *
 * <p>리소스 중복 생성 또는 상태 충돌 시 발생합니다. HTTP 409 Conflict에 매핑됩니다.</p>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * // 식사 기록 중복 시
 * throw new ConflictException("DUPLICATE_MEAL_RECORD", "이미 식사 기록이 존재합니다.");
 *
 * // 중복 결제 방지
 * throw new ConflictException("DUPLICATE_PAYMENT", "이미 처리 중인 결제가 있습니다.");
 * </pre>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see BusinessException
 */
public class ConflictException extends BusinessException {

    /**
     * 충돌 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     */
    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
