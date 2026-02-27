package com.unicorn.lunchpick.common.exception;

/**
 * 인증 실패 예외
 *
 * <p>JWT 토큰이 유효하지 않거나 인증이 필요한 리소스에 비인증 접근 시 발생합니다.
 * HTTP 401 Unauthorized에 매핑됩니다.</p>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * if (!jwtTokenProvider.validateToken(token)) {
 *     throw new UnauthorizedException("유효하지 않은 토큰입니다.");
 * }
 * </pre>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see BusinessException
 */
public class UnauthorizedException extends BusinessException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    /**
     * 인증 실패 예외 생성
     *
     * @param message 인증 실패 메시지
     */
    public UnauthorizedException(String message) {
        super(ERROR_CODE, message);
    }
}
