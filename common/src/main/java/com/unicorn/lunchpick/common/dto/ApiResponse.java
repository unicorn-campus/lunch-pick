package com.unicorn.lunchpick.common.dto;

import java.time.Instant;

/**
 * 공통 API 응답 래퍼
 *
 * <p>모든 REST API 응답에 사용되는 표준 응답 포맷입니다.</p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>성공 응답: {@link #ok(Object)} 정적 팩토리 메서드 사용</li>
 *   <li>실패 응답: {@link #fail(ErrorResponse)} 정적 팩토리 메서드 사용</li>
 *   <li>null 필드는 각 서비스의 Jackson ObjectMapper 설정으로 제어</li>
 * </ul>
 *
 * @param <T> 응답 데이터 타입
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public class ApiResponse<T> {

    /** 요청 성공 여부 */
    private final boolean success;

    /** 응답 데이터 (성공 시) */
    private final T data;

    /** 에러 응답 (실패 시) */
    private final ErrorResponse error;

    /** 응답 생성 시각 (ISO 8601, UTC) */
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = Instant.now();
    }

    /**
     * 성공 응답 생성
     *
     * @param data 응답 데이터
     * @param <T>  응답 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 데이터 없는 성공 응답 생성 (204 No Content 등)
     *
     * @param <T> 응답 데이터 타입
     * @return 성공 ApiResponse (data = null)
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 실패 응답 생성
     *
     * @param error 에러 응답 객체
     * @param <T>   응답 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
