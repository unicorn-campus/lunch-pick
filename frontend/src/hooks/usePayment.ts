/**
 * 결제 서비스 React Query 훅
 * paymentService를 TanStack Query로 래핑한다.
 *
 * 응답 접근 패턴: res.data.data (ApiResponse<T> 래퍼 → 실제 페이로드)
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { paymentService } from '@/services/api/paymentService'
import type {
  CreateSubscriptionRequest,
  CancelSubscriptionRequest,
} from '@/types/payment'

/** 활성 구독 조회 쿼리 */
export function useActiveSubscription() {
  return useQuery({
    queryKey: ['subscription', 'active'],
    queryFn: () => paymentService.getActiveSubscription().then((res) => res.data?.data ?? null),
    retry: false,
  })
}

/** 구독 플랜 조회 쿼리 */
export function useSubscriptionPlans() {
  return useQuery({
    queryKey: ['subscription', 'plans'],
    queryFn: () => paymentService.getSubscriptionPlans().then((res) => res.data.data),
    staleTime: 1000 * 60 * 60,
  })
}

/** 구독 결제 뮤테이션 */
export function useCreateSubscription() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSubscriptionRequest) =>
      paymentService.createSubscription(data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      queryClient.invalidateQueries({ queryKey: ['subscription-status'] })
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })
}

/** 구독 해지 뮤테이션 */
export function useCancelSubscription() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      subscriptionId,
      data,
    }: {
      subscriptionId: string
      data: CancelSubscriptionRequest
    }) => paymentService.cancelSubscription(subscriptionId, data).then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      queryClient.invalidateQueries({ queryKey: ['subscription-status'] })
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })
}

/** 7일 무료 연장 뮤테이션 */
export function useExtendTrial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => paymentService.extendTrial().then((res) => res.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      queryClient.invalidateQueries({ queryKey: ['subscription-status'] })
    },
  })
}
