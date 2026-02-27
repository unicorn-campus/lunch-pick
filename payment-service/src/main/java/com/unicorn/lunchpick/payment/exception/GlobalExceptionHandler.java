package com.unicorn.lunchpick.payment.exception;

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
 * 결제 서비스 전역 예외 처리기
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리 — errorCode에 따라 HTTP 상태 결정
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("비즈니스 예외 발생 — errorCode: {}, message: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ErrorResponse.of(ex.getErrorCode(), ex.getMessage())));
    }

    /**
     * 입력값 유효성 검사 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("입력값 유효성 오류: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorResponse.of("VALIDATION_FAILED", message)));
    }

    /**
     * 알 수 없는 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("예기치 않은 오류 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorResponse.of("INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했어요. 잠시 후 다시 시도해주세요.")));
    }

    private HttpStatus resolveHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "SUBSCRIPTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SUBSCRIPTION_ALREADY_ACTIVE",
                 "TRIAL_EXTENSION_ALREADY_USED",
                 "DUPLICATE_PAYMENT_LOCK" -> HttpStatus.CONFLICT;
            case "PAYMENT_FAILED" -> HttpStatus.PAYMENT_REQUIRED;
            case "INVALID_PAYMENT_INFO",
                 "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
