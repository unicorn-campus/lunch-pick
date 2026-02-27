package com.unicorn.lunchpick.common.exception;

/**
 * 유효성 검증 예외
 *
 * <p>입력값이 비즈니스 규칙을 위반할 때 발생합니다. HTTP 400 Bad Request에 매핑됩니다.</p>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * if (cards.size() &lt; 7) {
 *     throw new ValidationException("취향 퀴즈는 최소 7장을 완료해야 합니다.");
 * }
 * </pre>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see BusinessException
 */
public class ValidationException extends BusinessException {

    private static final String ERROR_CODE = "VALIDATION_FAILED";

    /**
     * 유효성 검증 예외 생성
     *
     * @param message 검증 실패 메시지
     */
    public ValidationException(String message) {
        super(ERROR_CODE, message);
    }

    /**
     * 커스텀 에러코드와 메시지로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     */
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
