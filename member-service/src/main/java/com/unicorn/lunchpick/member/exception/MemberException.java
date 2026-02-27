package com.unicorn.lunchpick.member.exception;

import com.unicorn.lunchpick.common.exception.BusinessException;

/**
 * 회원 서비스 전용 비즈니스 예외
 *
 * <p>회원 도메인의 비즈니스 규칙 위반 시 발생하는 예외입니다.</p>
 *
 * <p><b>주요 에러 코드:</b></p>
 * <ul>
 *   <li>{@code KAKAO_AUTH_FAILED} — 카카오 인증 실패</li>
 *   <li>{@code KAKAO_SERVICE_UNAVAILABLE} — 카카오 서버 연결 실패</li>
 *   <li>{@code MEMBER_NOT_FOUND} — 회원 없음</li>
 *   <li>{@code INSUFFICIENT_SWIPES} — 온보딩 스와이프 수 부족</li>
 *   <li>{@code HEALTH_INFO_CONSENT_REQUIRED} — 건강 정보 동의 필요</li>
 *   <li>{@code INVALID_NICKNAME} — 닉네임 형식 오류</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see BusinessException
 */
public class MemberException extends BusinessException {

    /**
     * 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     */
    public MemberException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param message   에러 메시지
     * @param cause     원인 예외
     */
    public MemberException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /** 카카오 인증 실패 예외 생성 팩토리 메서드 */
    public static MemberException kakaoAuthFailed() {
        return new MemberException("KAKAO_AUTH_FAILED", "인증에 실패했어요. 다시 시도해주세요.");
    }

    /** 카카오 서비스 불가 예외 생성 팩토리 메서드 */
    public static MemberException kakaoServiceUnavailable() {
        return new MemberException("KAKAO_SERVICE_UNAVAILABLE", "인터넷 연결을 확인해주세요.");
    }

    /** 회원 없음 예외 생성 팩토리 메서드 */
    public static MemberException memberNotFound() {
        return new MemberException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다.");
    }

    /** 온보딩 스와이프 수 부족 예외 생성 팩토리 메서드 */
    public static MemberException insufficientSwipes() {
        return new MemberException("INSUFFICIENT_SWIPES", "조금만 더! 7장 이상 스와이프해야 취향을 파악할 수 있어요.");
    }

    /** 건강 정보 동의 필요 예외 생성 팩토리 메서드 */
    public static MemberException healthInfoConsentRequired() {
        return new MemberException("HEALTH_INFO_CONSENT_REQUIRED", "건강 관련 정보 수집에 동의해주세요.");
    }

    /** 닉네임 형식 오류 예외 생성 팩토리 메서드 */
    public static MemberException invalidNickname() {
        return new MemberException("INVALID_NICKNAME", "닉네임은 2~20자, 특수문자 없이 입력해주세요.");
    }
}
