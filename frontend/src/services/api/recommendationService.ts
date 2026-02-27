/**
 * 추천·이력 서비스 API 함수
 * recommendation-service 실제 컨트롤러 엔드포인트와 정확히 일치
 *
 * 응답 구조: ApiResponse<T> { success, data, error, timestamp }
 * 호출부에서 res.data.data 로 실제 페이로드에 접근한다.
 */
import { recommendationApiClient } from './instances'
import type { ApiResponse } from '@/types/api'
import type {
  TodayRecommendationsResponse,
  RecommendationReasonResponse,
  AcceptRecommendationRequest,
  AcceptRecommendationResponse,
  RejectRecommendationRequest,
  RejectRecommendationResponse,
  RefreshRecommendationsRequest,
  CreateMealRequest,
  UpdateMealRequest,
  MealResponse,
  FeedbackRequest,
  FeedbackResponse,
  MealHistoryResponse,
  InsightsResponse,
} from '@/types/recommendation'

export const recommendationService = {
  /** GET /api/v1/recommendations/today — 오늘의 추천 3개 조회 */
  getTodayRecommendations: (params: { latitude: number; longitude: number; requestedAt?: string }) =>
    recommendationApiClient.get<ApiResponse<TodayRecommendationsResponse>>('/recommendations/today', { params }),

  /** GET /api/v1/recommendations/{recommendationId}/reason — 추천 이유 상세 확인 */
  getRecommendationReason: (recommendationId: string) =>
    recommendationApiClient.get<ApiResponse<RecommendationReasonResponse>>(
      `/recommendations/${recommendationId}/reason`,
    ),

  /** POST /api/v1/recommendations/{recommendationId}/accept — 추천 수락 */
  acceptRecommendation: (recommendationId: string, data: AcceptRecommendationRequest) =>
    recommendationApiClient.post<ApiResponse<AcceptRecommendationResponse>>(
      `/recommendations/${recommendationId}/accept`,
      data,
    ),

  /** POST /api/v1/recommendations/{recommendationId}/reject — 추천 거절 */
  rejectRecommendation: (recommendationId: string, data: RejectRecommendationRequest) =>
    recommendationApiClient.post<ApiResponse<RejectRecommendationResponse>>(
      `/recommendations/${recommendationId}/reject`,
      data,
    ),

  /** POST /api/v1/recommendations/refresh — 추천 전체 새로고침 */
  refreshRecommendations: (data: RefreshRecommendationsRequest) =>
    recommendationApiClient.post<ApiResponse<TodayRecommendationsResponse>>('/recommendations/refresh', data),

  /** POST /api/v1/meals — 식사 완료 원탭 기록 */
  createMeal: (data: CreateMealRequest) =>
    recommendationApiClient.post<ApiResponse<MealResponse>>('/meals', data),

  /** PUT /api/v1/meals/{mealId} — 식사 기록 수정 */
  updateMeal: (mealId: string, data: UpdateMealRequest) =>
    recommendationApiClient.put<ApiResponse<MealResponse>>(`/meals/${mealId}`, data),

  /** DELETE /api/v1/meals/{mealId} — 식사 기록 취소 (30초 이내) */
  deleteMeal: (mealId: string) =>
    recommendationApiClient.delete<ApiResponse<{ message: string }>>(`/meals/${mealId}`),

  /** POST /api/v1/meals/{mealId}/feedback — 식사 피드백 제출 */
  submitFeedback: (mealId: string, data: FeedbackRequest) =>
    recommendationApiClient.post<ApiResponse<FeedbackResponse>>(`/meals/${mealId}/feedback`, data),

  /** GET /api/v1/history/timeline — 식사 이력 타임라인 조회 */
  getHistoryTimeline: (params?: { startDate?: string; endDate?: string }) =>
    recommendationApiClient.get<ApiResponse<MealHistoryResponse>>('/history/timeline', { params }),

  /** GET /api/v1/insights — 취향 인사이트 리포트 조회 */
  getInsights: () =>
    recommendationApiClient.get<ApiResponse<InsightsResponse>>('/insights'),
}
