package com.unicorn.lunchpick.recommendation.exception;

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
 * 추천 서비스 전역 예외 처리 핸들러
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("비즈니스 예외 발생 — errorCode: {}, message: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ErrorResponse.of(ex.getErrorCode(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("유효성 검증 실패 — {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorResponse.of("VALIDATION_FAILED", message)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("서버 내부 오류 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.")));
    }

    private HttpStatus resolveHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "RECOMMENDATION_NOT_FOUND", "MEAL_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_MEAL_RECORD" -> HttpStatus.CONFLICT;
            case "CANCEL_TIMEOUT" -> HttpStatus.CONFLICT;
            case "PREMIUM_REQUIRED" -> HttpStatus.FORBIDDEN;
            case "INVALID_MEAL_TIME", "LOCATION_REQUIRED", "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
