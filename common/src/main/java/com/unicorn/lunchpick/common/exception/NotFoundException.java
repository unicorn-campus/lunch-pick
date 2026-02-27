package com.unicorn.lunchpick.common.exception;

/**
 * 리소스 없음 예외
 *
 * <p>요청한 리소스가 존재하지 않을 때 발생합니다. HTTP 404 Not Found에 매핑됩니다.</p>
 *
 * <p><b>사용 예시:</b></p>
 * <pre>
 * throw new NotFoundException("Member", memberId);
 * // 메시지: "Member not found: 123"
 * </pre>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see BusinessException
 */
public class NotFoundException extends BusinessException {

    private static final String ERROR_CODE = "NOT_FOUND";

    /**
     * 리소스명과 식별자로 예외 생성
     *
     * @param resource 리소스명 (예: "Member", "Recommendation")
     * @param id       리소스 식별자
     */
    public NotFoundException(String resource, Object id) {
        super(ERROR_CODE, resource + " not found: " + id);
    }

    /**
     * 커스텀 메시지로 예외 생성
     *
     * @param message 에러 메시지
     */
    public NotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
