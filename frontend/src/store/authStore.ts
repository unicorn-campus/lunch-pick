/**
 * 인증 상태 스토어 (Zustand)
 * JWT 토큰과 회원 정보를 관리한다.
 *
 * 토큰 저장: authService.saveToken() → localStorage('lunchpick_token')
 * persist 스토어: 토큰 제외한 인증 메타정보만 저장 (lunchpick-auth)
 * 초기화: clearAuth() → 토큰 + persist 스토어 모두 제거
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { authService } from '@/services/api/authService'

interface AuthState {
  isAuthenticated: boolean
  token: string | null
  memberId: string | null
  nickname: string | null
  isNewUser: boolean
  onboardingCompleted: boolean

  setAuth: (token: string, memberId: string, nickname: string, isNewUser: boolean) => void
  setOnboardingCompleted: () => void
  setNickname: (nickname: string) => void
  clearAuth: () => void
  initialize: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isAuthenticated: false,
      token: null,
      memberId: null,
      nickname: null,
      isNewUser: false,
      onboardingCompleted: false,

      setAuth: (token, memberId, nickname, isNewUser) => {
        authService.saveToken(token)
        set({ isAuthenticated: true, token, memberId, nickname, isNewUser })
      },

      setOnboardingCompleted: () =>
        set({ onboardingCompleted: true }),

      setNickname: (nickname: string) =>
        set({ nickname }),

      clearAuth: () => {
        authService.clearToken()
        set({
          isAuthenticated: false,
          token: null,
          memberId: null,
          nickname: null,
          isNewUser: false,
          onboardingCompleted: false,
        })
      },

      /**
       * 앱 최초 진입 또는 새로고침 시 localStorage 토큰 존재 여부로 인증 상태를 복원한다.
       * 실제 토큰 유효성 검증은 API 요청 시 401 응답 인터셉터가 처리한다.
       */
      initialize: () => {
        const token = authService.getToken()
        if (token) {
          set({ isAuthenticated: true, token })
        } else {
          set({ isAuthenticated: false, token: null })
        }
      },
    }),
    {
      name: 'lunchpick-auth',
      // token은 authService를 통해 localStorage에 직접 관리하므로 persist에서 제외
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        memberId: state.memberId,
        nickname: state.nickname,
        onboardingCompleted: state.onboardingCompleted,
      }),
    },
  ),
)
