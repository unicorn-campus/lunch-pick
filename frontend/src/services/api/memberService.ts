/**
 * 회원 서비스 API 함수
 * member-service 실제 컨트롤러 엔드포인트와 정확히 일치
 *
 * 엔드포인트 매핑 (백엔드 컨트롤러 기준):
 *   AuthController      @RequestMapping("/api/v1/auth")
 *   OnboardingController @RequestMapping("/api/v1/onboarding")
 *   ProfileController   @RequestMapping("/api/v1/members")
 *   SubscriptionController @RequestMapping("/api/v1/members/me")
 *
 * 응답 구조: ApiResponse<T> { success, data, error, timestamp }
 * 호출부에서 res.data.data 로 실제 페이로드에 접근한다.
 */
import { memberApiClient } from './instances'
import type { ApiResponse } from '@/types/api'
import type {
  KakaoLoginRequest,
  AuthResponse,
  OnboardingRequest,
  OnboardingResponse,
  OnboardingProgressRequest,
  LocationConsentRequest,
  LocationConsentResponse,
  DietaryRestrictionsRequest,
  DietaryRestrictionsResponse,
  MemberProfile,
  UpdateProfileRequest,
  SubscriptionStatus,
} from '@/types/member'

export const memberService = {
  /** POST /api/v1/auth/kakao — 카카오 소셜 로그인 */
  kakaoLogin: (data: KakaoLoginRequest) =>
    memberApiClient.post<ApiResponse<AuthResponse>>('/auth/kakao', data),

  /** POST /api/v1/onboarding — 취향 온보딩 퀴즈 제출 */
  submitOnboarding: (data: OnboardingRequest) =>
    memberApiClient.post<ApiResponse<OnboardingResponse>>('/onboarding', data),

  /** PUT /api/v1/onboarding/progress — 온보딩 진행 상태 임시 저장 */
  saveOnboardingProgress: (data: OnboardingProgressRequest) =>
    memberApiClient.put<ApiResponse<{ message: string; savedCount: number }>>(
      '/onboarding/progress',
      data,
    ),

  /** POST /api/v1/members/me/location-consent — 위치 정보 동의 */
  submitLocationConsent: (data: LocationConsentRequest) =>
    memberApiClient.post<ApiResponse<LocationConsentResponse>>('/members/me/location-consent', data),

  /** PUT /api/v1/members/me/dietary-restrictions — 알레르기/식이제한 설정 */
  updateDietaryRestrictions: (data: DietaryRestrictionsRequest) =>
    memberApiClient.put<ApiResponse<DietaryRestrictionsResponse>>('/members/me/dietary-restrictions', data),

  /** GET /api/v1/members/me — 프로필 조회 */
  getProfile: () =>
    memberApiClient.get<ApiResponse<MemberProfile>>('/members/me'),

  /** PUT /api/v1/members/me — 프로필 수정 */
  updateProfile: (data: UpdateProfileRequest) =>
    memberApiClient.put<ApiResponse<MemberProfile>>('/members/me', data),

  /** GET /api/v1/members/me/subscription — 구독 상태 조회 */
  getSubscriptionStatus: () =>
    memberApiClient.get<ApiResponse<SubscriptionStatus>>('/members/me/subscription'),
}
