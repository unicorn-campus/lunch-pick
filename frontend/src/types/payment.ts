/**
 * 결제 서비스 타입 정의
 * payment-service-api.yaml components/schemas 기반
 */

/** 활성 구독 조회 응답 */
export interface ActiveSubscriptionResponse {
  subscriptionId: string
  planId: string
  status: string
  currentPeriodEndsAt: string | null
}

/** 구독 플랜 */
export interface SubscriptionPlan {
  planId: 'FREE' | 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'
  planName: string
  pricePerMonth: number
  totalPrice: number
  billingCycle: 'MONTHLY' | 'ANNUAL'
  discountRate: number
  features: string[]
}

/** 구독 플랜 목록 응답 */
export interface SubscriptionPlansResponse {
  plans: SubscriptionPlan[]
  currentPlan: 'FREE' | 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'
  promotionMessage: string | null
}

/** 결제 수단 */
export interface PaymentMethod {
  type: 'CREDIT_CARD' | 'DEBIT_CARD'
  cardNumber: string
  expiryMonth: number
  expiryYear: number
  cvc: string
  cardholderName?: string
}

/** 구독 결제 요청 */
export interface CreateSubscriptionRequest {
  planId: 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'
  paymentMethod: PaymentMethod
  autoRenewalAgreed: boolean
  withdrawalRightAcknowledged: boolean
}

/** 구독 결제 응답 */
export interface CreateSubscriptionResponse {
  subscriptionId: string
  planId: string
  status: 'ACTIVE' | 'PENDING_CANCEL'
  startedAt: string
  nextBillingAt: string
  amount: number
  transactionId: string
  message: string
  withdrawalDeadline: string
}

/** 구독 해지 요청 */
export interface CancelSubscriptionRequest {
  cancelReason: 'COST' | 'NOT_USING' | 'QUALITY' | 'OTHER'
  cancelReasonDetail?: string | null
}

/** 구독 해지 응답 */
export interface CancelSubscriptionResponse {
  subscriptionId: string
  status: 'PENDING_CANCEL'
  currentPeriodEndsAt: string
  freePlanStartsAt: string
  message: string
  dataWarningMessage: string
}

/** 7일 무료 연장 응답 */
export interface ExtendTrialResponse {
  message: string
  newExpiresAt: string
}
