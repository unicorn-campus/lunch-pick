/**
 * 추천·이력 서비스 타입 정의
 * recommendation-service-api.yaml components/schemas 기반
 */

/** 추천 카드 */
export interface RecommendationCard {
  recommendationId: string
  restaurantId: string
  restaurantName: string
  representativeMenu: string
  reasonSummary: string
  confidenceScore: number
  distanceMeters: number
  estimatedWalkMinutes: number
  category: string
  isFallback: boolean
}

/** 오늘의 추천 응답 */
export interface TodayRecommendationsResponse {
  recommendations: RecommendationCard[]
  isColdStart: boolean
  coldStartMessage: string | null
  isFallback: boolean
  fallbackMessage: string | null
  generatedAt: string
}

/** 추천 이유 상세 응답 */
export interface RecommendationReasonResponse {
  recommendationId: string
  naturalLanguageReason: string
  confidenceScore: number
  contextTags: string[]
  isReasonReady: boolean
  fallbackMessage: string | null
}

/** 추천 수락 요청 */
export interface AcceptRecommendationRequest {
  acceptedAt: string
  reactionTimeMs: number
}

/** 추천 수락 응답 */
export interface AcceptRecommendationResponse {
  acceptanceId: string
  restaurantId: string
  restaurantName: string
  restaurantAddress: string
  message: string
}

/** 추천 거절 요청 */
export interface RejectRecommendationRequest {
  rejectReason: 'MOOD_NOT_MATCH' | 'TOO_FAR' | 'RECENTLY_VISITED' | 'OTHER'
}

/** 추천 거절 응답 */
export interface RejectRecommendationResponse {
  rejected: boolean
  alternativeRecommendation: RecommendationCard | null
  hasAlternative: boolean
  message: string | null
  noAlternativeMessage: string | null
}

/** 추천 새로고침 요청 */
export interface RefreshRecommendationsRequest {
  rejectedIds: string[]
  latitude: number
  longitude: number
}

/** 식사 기록 생성 요청 */
export interface CreateMealRequest {
  recommendationId?: string | null
  restaurantId: string
  menuName?: string
  recordedAt: string
}

/** 식사 기록 수정 요청 */
export interface UpdateMealRequest {
  restaurantId?: string
  menuName?: string
  recordedAt?: string
}

/** 식사 기록 응답 */
export interface MealResponse {
  mealId: string
  restaurantName: string
  menuName: string | null
  recordedAt: string
  message: string
}

/** 피드백 요청 */
export interface FeedbackRequest {
  satisfaction: 'GOOD' | 'BAD' | 'NEUTRAL'
  keyword?: 'TASTE' | 'PORTION' | 'SPEED' | null
}

/** 피드백 응답 */
export interface FeedbackResponse {
  message: string
  reflectionMessage: string
  totalFeedbackCount: number
}

/** 식사 이력 항목 */
export interface MealHistoryItem {
  mealId: string
  date: string
  restaurantName: string
  menuName: string | null
  category: string
  categoryColor: string
  satisfaction: 'GOOD' | 'BAD' | 'NEUTRAL' | null
  recordedAt: string
}

/** 식사 이력 응답 */
export interface MealHistoryResponse {
  meals: MealHistoryItem[]
  totalCount: number
  message: string | null
}

/** 카테고리 분포 */
export interface CategoryDistribution {
  category: string
  count: number
  percentage: number
  color: string
}

/** 요일별 패턴 */
export interface WeeklyPattern {
  dayOfWeek: 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI'
  topCategory: string
  averageSatisfaction: number
}

/** 만족도 트렌드 */
export interface SatisfactionTrend {
  week: string
  satisfactionRate: number
}

/** 마일스톤 배지 */
export interface MilestoneBadge {
  achieved: boolean
  count: number
  message: string
  accuracyImprovement: number
}

/** 인사이트 응답 */
export interface InsightsResponse {
  hasEnoughData: boolean
  currentRecordCount: number
  requiredRecordCount: number
  message: string | null
  topCategories: CategoryDistribution[]
  weeklyPattern: WeeklyPattern[]
  satisfactionTrend: SatisfactionTrend[]
  weeklySummary: string | null
  milestone: MilestoneBadge | null
}
