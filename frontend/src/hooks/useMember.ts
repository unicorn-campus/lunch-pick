/**
 * 회원 서비스 React Query 훅
 * memberService를 TanStack Query로 래핑한다.
 * 페이지 컴포넌트에서는 반드시 이 훅을 통해 API를 사용한다.
 *
 * 응답 접근 패턴: res.data.data (ApiResponse<T> 래퍼 → 실제 페이로드)
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { memberService } from '@/services/api/memberService'
import { authService } from '@/services/api/authService'
import { useAuthStore } from '@/store/authStore'
import type {
  KakaoLoginRequest,
  OnboardingRequest,
  OnboardingProgressRequest,
  LocationConsentRequest,
  DietaryRestrictionsRequest,
  UpdateProfileRequest,
} from '@/types/member'

/** 카카오 로그인 뮤테이션 */
export function useKakaoLogin() {
  const { setAuth } = useAuthStore()
  return useMutation({
    mutationFn: (data: KakaoLoginRequest) =>
      memberService.kakaoLogin(data).then((res) => res.data.data),
    onSuccess: (data) => {
      authService.saveToken(data.accessToken)
      setAuth(
        data.accessToken,
        data.memberId,
        data.nickname || '',
        data.isNewUser,
      )
    },
  })
}

/** 취향 온보딩 퀴즈 제출 뮤테이션 */
export function useSubmitOnboarding() {
  const { setOnboardingCompleted } = useAuthStore()
  return useMutation({
    mutationFn: (data: OnboardingRequest) =>
      memberService.submitOnboarding(data).then((res) => res.data.data),
    onSuccess: () => {
      setOnboardingCompleted()
    },
  })
}

/** 온보딩 중간 저장 뮤테이션 */
export function useSaveOnboardingProgress() {
  return useMutation({
    mutationFn: (data: OnboardingProgressRequest) =>
      memberService.saveOnboardingProgress(data).then((res) => res.data.data),
  })
}

/** 위치 동의 뮤테이션 */
export function useSubmitLocationConsent() {
  return useMutation({
    mutationFn: (data: LocationConsentRequest) =>
      memberService.submitLocationConsent(data).then((res) => res.data.data),
  })
}

/** 식이제한 설정 뮤테이션 */
export function useUpdateDietaryRestrictions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: DietaryRestrictionsRequest) =>
      memberService.updateDietaryRestrictions(data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })
}

/** 프로필 조회 쿼리 */
export function useProfile() {
  return useQuery({
    queryKey: ['profile'],
    queryFn: () => memberService.getProfile().then((res) => res.data.data),
    staleTime: 1000 * 60 * 10,
  })
}

/** 프로필 수정 뮤테이션 */
export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateProfileRequest) =>
      memberService.updateProfile(data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })
}

/** 구독 상태 조회 쿼리 */
export function useSubscriptionStatus() {
  return useQuery({
    queryKey: ['subscription-status'],
    queryFn: () => memberService.getSubscriptionStatus().then((res) => res.data.data),
    staleTime: 1000 * 60 * 5,
  })
}
