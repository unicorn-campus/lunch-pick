/**
 * 인증 서비스 함수
 * member-service AuthController 기반 카카오 OAuth 인증 흐름을 담당한다.
 *
 * 카카오 로그인 흐름:
 *   1. 카카오 OAuth 인증 페이지로 리다이렉트
 *   2. 인가 코드(authorizationCode) 수신
 *   3. POST /api/v1/auth/kakao 로 인가 코드 전달
 *   4. JWT accessToken 수신 후 localStorage 저장
 *   5. isNewUser 여부에 따라 온보딩 또는 홈으로 라우팅
 */
import { memberApiClient } from './instances'
import type { ApiResponse } from '@/types/api'
import type { KakaoLoginRequest, AuthResponse } from '@/types/member'

/** 토큰 저장 키 (client.ts 인터셉터와 동일) */
export const TOKEN_KEY = 'lunchpick_token'
export const AUTH_STORE_KEY = 'lunchpick-auth'

export const authService = {
  /**
   * 카카오 소셜 로그인
   * 기존 회원: 200 OK (isNewUser: false)
   * 신규 회원: 201 Created (isNewUser: true)
   */
  kakaoLogin: (data: KakaoLoginRequest) =>
    memberApiClient.post<ApiResponse<AuthResponse>>('/auth/kakao', data),

  /**
   * 토큰을 localStorage에 저장한다.
   * client.ts 요청 인터셉터가 이 키를 읽어 Authorization 헤더에 주입한다.
   */
  saveToken: (accessToken: string): void => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(TOKEN_KEY, accessToken)
    }
  },

  /** localStorage에서 토큰을 제거하고 인증 스토어를 초기화한다. */
  clearToken: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(AUTH_STORE_KEY)
    }
  },

  /** 현재 저장된 토큰을 반환한다. */
  getToken: (): string | null => {
    if (typeof window === 'undefined') return null
    return localStorage.getItem(TOKEN_KEY)
  },

  /** 토큰 존재 여부로 인증 상태를 확인한다. */
  isAuthenticated: (): boolean => {
    return authService.getToken() !== null
  },
}
