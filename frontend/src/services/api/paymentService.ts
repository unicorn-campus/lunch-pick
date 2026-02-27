/**
 * 결제 서비스 API 함수
 * payment-service 실제 컨트롤러 엔드포인트와 정확히 일치
 *
 * 응답 구조: ApiResponse<T> { success, data, error, timestamp }
 * 호출부에서 res.data.data 로 실제 페이로드에 접근한다.
 */
import { paymentApiClient } from './instances'
import type { ApiResponse } from '@/types/api'
import type {
  ActiveSubscriptionResponse,
  SubscriptionPlansResponse,
  CreateSubscriptionRequest,
  CreateSubscriptionResponse,
  CancelSubscriptionRequest,
  CancelSubscriptionResponse,
  ExtendTrialResponse,
} from '@/types/payment'

export const paymentService = {
  /** GET /api/v1/subscriptions/active — 활성 구독 조회 */
  getActiveSubscription: () =>
    paymentApiClient.get<ApiResponse<ActiveSubscriptionResponse>>('/subscriptions/active'),

  /** GET /api/v1/subscriptions/plans — 구독 플랜 조회 */
  getSubscriptionPlans: () =>
    paymentApiClient.get<ApiResponse<SubscriptionPlansResponse>>('/subscriptions/plans'),

  /** POST /api/v1/subscriptions — 구독 결제 */
  createSubscription: (data: CreateSubscriptionRequest) =>
    paymentApiClient.post<ApiResponse<CreateSubscriptionResponse>>('/subscriptions', data),

  /** DELETE /api/v1/subscriptions/{subscriptionId} — 구독 해지 */
  cancelSubscription: (subscriptionId: string, data: CancelSubscriptionRequest) =>
    paymentApiClient.delete<ApiResponse<CancelSubscriptionResponse>>(`/subscriptions/${subscriptionId}`, {
      data,
    }),

  /** POST /api/v1/subscriptions/extend-trial — 7일 무료 연장 */
  extendTrial: () =>
    paymentApiClient.post<ApiResponse<ExtendTrialResponse>>('/subscriptions/extend-trial'),
}
