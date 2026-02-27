/**
 * 회원 서비스 타입 정의
 * member-service-api.yaml components/schemas 기반
 */

/** 카카오 로그인 요청 */
export interface KakaoLoginRequest {
  authorizationCode: string
}

/** 인증 응답 */
export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  memberId: string
  isNewUser: boolean
  onboardingCompleted: boolean
}

/** 카드 스와이프 결과 */
export interface CardSwipeResult {
  cardId: string
  liked: boolean
  category?: string
}

/** 온보딩 요청 */
export interface OnboardingRequest {
  swipeResults: CardSwipeResult[]
  healthInfoConsentGiven: boolean
}

/** 온보딩 응답 */
export interface OnboardingResponse {
  message: string
  topCategories: string[]
  tasteVectorCreated: boolean
}

/** 온보딩 중간 저장 요청 */
export interface OnboardingProgressRequest {
  swipeResults: CardSwipeResult[]
}

/** 위치 동의 요청 */
export interface LocationConsentRequest {
  consented: boolean
  consentedAt: string
}

/** 위치 동의 응답 */
export interface LocationConsentResponse {
  locationEnabled: boolean
  message: string
}

/** 알레르기/식이제한 설정 요청 */
export interface DietaryRestrictionsRequest {
  healthInfoConsentGiven: boolean
  allergens?: string[]
  customAllergens?: string[]
  dietType?: string
}

/** 알레르기/식이제한 설정 응답 */
export interface DietaryRestrictionsResponse {
  message: string
  appliedAllergens: string[]
  dietType: string
}

/** 알림 설정 */
export interface NotificationSettings {
  recommendationAlert: boolean
  feedbackReminder: boolean
}

/** 구독 상태 */
export interface SubscriptionStatus {
  plan: 'FREE' | 'PREMIUM'
  historyLimitDays: number | null
  expiresAt: string | null
}

/** 회원 프로필 */
export interface MemberProfile {
  memberId: string
  nickname: string
  email: string
  dietType: string
  allergens: string[]
  locationEnabled: boolean
  notificationSettings: NotificationSettings
  subscription: SubscriptionStatus
  onboardingCompleted: boolean
  createdAt: string
}

/** 프로필 수정 요청 */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  notificationSettings?: NotificationSettings
}

/** 에러 응답 */
export interface ErrorResponse {
  error: string
  message: string
  timestamp: string
}
