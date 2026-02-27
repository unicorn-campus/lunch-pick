package com.unicorn.lunchpick.member.exception;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.common.dto.ErrorResponse;
import com.unicorn.lunchpick.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 *
 * <p>회원 서비스 전역에서 발생하는 예외를 일관된 {@link ApiResponse} 포맷으로 변환합니다.</p>
 *
 * <p><b>처리 예외 목록:</b></p>
 * <ul>
 *   <li>{@link BusinessException} 및 하위 클래스 — 400/401/404/409</li>
 *   <li>{@link MethodArgumentNotValidException} — 400 유효성 검증 오류</li>
 *   <li>{@link Exception} — 500 서버 내부 오류</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     *
     * <p>에러 코드에 따라 HTTP 상태 코드를 결정합니다.</p>
     *
     * @param ex 비즈니스 예외
     * @return 실패 ApiResponse (400/401/404)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("비즈니스 예외 발생 — errorCode: {}, message: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ErrorResponse.of(ex.getErrorCode(), ex.getMessage())));
    }

    /**
     * Bean Validation 실패 예외 처리
     *
     * <p>복수 필드 오류를 첫 번째 오류 메시지로 요약합니다.</p>
     *
     * @param ex 유효성 검증 예외
     * @return 실패 ApiResponse (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("유효성 검증 실패 — {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorResponse.of("VALIDATION_FAILED", message)));
    }

    /**
     * 예상하지 못한 서버 내부 오류 처리
     *
     * @param ex 런타임 예외
     * @return 실패 ApiResponse (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("서버 내부 오류 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.")));
    }

    /**
     * 에러 코드 기반 HTTP 상태 코드 결정
     *
     * @param errorCode 에러 코드 문자열
     * @return 매핑된 HttpStatus
     */
    private HttpStatus resolveHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "KAKAO_AUTH_FAILED", "INSUFFICIENT_SWIPES",
                 "HEALTH_INFO_CONSENT_REQUIRED", "INVALID_NICKNAME",
                 "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            case "KAKAO_SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "MEMBER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
