package com.unicorn.lunchpick.common.dto;

import java.time.Instant;

/**
 * 공통 에러 응답
 *
 * <p>API 오류 발생 시 {@link ApiResponse#fail(ErrorResponse)}와 함께 사용되는 표준 에러 포맷입니다.</p>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * ErrorResponse error = ErrorResponse.of("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.");
 * return ApiResponse.fail(error);
 * </pre>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public class ErrorResponse {

    /** 에러 코드 (예: "MEMBER_NOT_FOUND", "VALIDATION_FAILED") */
    private final String error;

    /** 사용자 친화적 에러 메시지 */
    private final String message;

    /** 에러 발생 시각 (ISO 8601, UTC) */
    private final Instant timestamp;

    private ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    /**
     * 에러 응답 생성
     *
     * @param error   에러 코드
     * @param message 에러 메시지
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
