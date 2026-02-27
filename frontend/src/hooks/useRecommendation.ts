/**
 * 추천·이력 서비스 React Query 훅
 * recommendationService를 TanStack Query로 래핑한다.
 *
 * 응답 접근 패턴: res.data.data (ApiResponse<T> 래퍼 → 실제 페이로드)
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { recommendationService } from '@/services/api/recommendationService'
import type {
  AcceptRecommendationRequest,
  RejectRecommendationRequest,
  RefreshRecommendationsRequest,
  CreateMealRequest,
  UpdateMealRequest,
  FeedbackRequest,
} from '@/types/recommendation'

/** 오늘의 추천 조회 쿼리 */
export function useTodayRecommendations(params: {
  latitude: number
  longitude: number
  requestedAt?: string
}) {
  return useQuery({
    queryKey: ['recommendations', 'today', params.latitude, params.longitude],
    queryFn: () => recommendationService.getTodayRecommendations(params).then((res) => res.data.data),
    staleTime: 1000 * 60 * 30,
  })
}

/** 추천 이유 상세 쿼리 */
export function useRecommendationReason(recommendationId: string) {
  return useQuery({
    queryKey: ['recommendations', 'reason', recommendationId],
    queryFn: () =>
      recommendationService.getRecommendationReason(recommendationId).then((res) => res.data.data),
    enabled: !!recommendationId,
    staleTime: 1000 * 60 * 60,
  })
}

/** 추천 수락 뮤테이션 */
export function useAcceptRecommendation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      recommendationId,
      data,
    }: {
      recommendationId: string
      data: AcceptRecommendationRequest
    }) =>
      recommendationService
        .acceptRecommendation(recommendationId, data)
        .then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations', 'today'] })
    },
  })
}

/** 추천 거절 뮤테이션 */
export function useRejectRecommendation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      recommendationId,
      data,
    }: {
      recommendationId: string
      data: RejectRecommendationRequest
    }) =>
      recommendationService
        .rejectRecommendation(recommendationId, data)
        .then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations', 'today'] })
    },
  })
}

/** 추천 새로고침 뮤테이션 */
export function useRefreshRecommendations() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: RefreshRecommendationsRequest) =>
      recommendationService.refreshRecommendations(data).then((res) => res.data.data),
    onSuccess: (data, variables) => {
      // mutation 응답 데이터로 쿼리 캐시를 직접 교체한다.
      // invalidateQueries → refetch 방식은 recommendation-service DB2 캐시를
      // 다시 읽어 동일 데이터를 반환하므로, setQueryData로 즉시 반영한다.
      queryClient.setQueryData(
        ['recommendations', 'today', variables.latitude, variables.longitude],
        data,
      )
    },
  })
}

/** 식사 기록 생성 뮤테이션 */
export function useCreateMeal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateMealRequest) =>
      recommendationService.createMeal(data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['history'] })
    },
  })
}

/** 식사 기록 수정 뮤테이션 */
export function useUpdateMeal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ mealId, data }: { mealId: string; data: UpdateMealRequest }) =>
      recommendationService.updateMeal(mealId, data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['history'] })
    },
  })
}

/** 식사 기록 취소 뮤테이션 */
export function useDeleteMeal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (mealId: string) =>
      recommendationService.deleteMeal(mealId).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['history'] })
    },
  })
}

/** 피드백 제출 뮤테이션 */
export function useSubmitFeedback() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ mealId, data }: { mealId: string; data: FeedbackRequest }) =>
      recommendationService.submitFeedback(mealId, data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['history'] })
      queryClient.invalidateQueries({ queryKey: ['insights'] })
    },
  })
}

/** 식사 이력 타임라인 쿼리 */
export function useHistoryTimeline(params?: { startDate?: string; endDate?: string }) {
  return useQuery({
    queryKey: ['history', 'timeline', params],
    queryFn: () => recommendationService.getHistoryTimeline(params).then((res) => res.data.data),
    staleTime: 1000 * 60 * 5,
  })
}

/** 인사이트 쿼리 */
export function useInsights() {
  return useQuery({
    queryKey: ['insights'],
    queryFn: () => recommendationService.getInsights().then((res) => res.data.data),
    staleTime: 1000 * 60 * 10,
  })
}
