package com.unicorn.lunchpick.common.exception;

/**
 * 비즈니스 예외 기반 클래스
 *
 * <p>도메인 비즈니스 규칙 위반 시 발생하는 예외의 최상위 타입입니다.
 * 모든 서비스별 비즈니스 예외는 이 클래스를 상속합니다.</p>
 *
 * <p><b>하위 예외:</b></p>
 * <ul>
 *   <li>{@link NotFoundException} — 리소스를 찾을 수 없을 때</li>
 *   <li>{@link ValidationException} — 유효성 검증 실패 시</li>
 *   <li>{@link ConflictException} — 충돌(중복) 발생 시</li>
 *   <li>{@link UnauthorizedException} — 인증 실패 시</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public class BusinessException extends RuntimeException {

    /** 에러 코드 (예: "MEMBER_NOT_FOUND", "DUPLICATE_PAYMENT") */
    private final String errorCode;

    /**
     * 비즈니스 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 원인 예외를 포함한 비즈니스 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @param cause     원인 예외
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
