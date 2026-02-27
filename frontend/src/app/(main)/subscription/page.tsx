'use client'

/**
 * 구독 플랜 비교 / 결제 / 해지 페이지
 * UFR-PAY-010: GET /api/v1/subscriptions/plans
 * UFR-PAY-020: POST /api/v1/subscriptions
 * UFR-PAY-030: DELETE /api/v1/subscriptions/{id}, POST /api/v1/subscriptions/extend-trial
 */
import { useState } from 'react'
import Loading from '@/components/common/Loading'
import BottomSheet from '@/components/common/BottomSheet'
import Modal from '@/components/common/Modal'
import Button from '@/components/common/Button'
import { useSubscriptionPlans, useCreateSubscription, useCancelSubscription, useExtendTrial } from '@/hooks/usePayment'
import { useSubscriptionStatus } from '@/hooks/useMember'
import { useToast } from '@/hooks/useToast'
import type { PaymentMethod, SubscriptionPlansResponse } from '@/types/payment'

/** 데모 플랜 데이터 */
const DEMO_PLANS: SubscriptionPlansResponse = {
  plans: [
    {
      planId: 'FREE',
      planName: '무료',
      pricePerMonth: 0,
      totalPrice: 0,
      billingCycle: 'MONTHLY',
      discountRate: 0,
      features: ['하루 3회 추천', '기본 취향 학습', '최근 7일 이력'],
    },
    {
      planId: 'PREMIUM_MONTHLY',
      planName: '프리미엄 월간',
      pricePerMonth: 4900,
      totalPrice: 4900,
      billingCycle: 'MONTHLY',
      discountRate: 0,
      features: ['무제한 추천', 'AI 정밀 취향 분석', '전체 이력 보관', '우선 고객 지원'],
    },
    {
      planId: 'PREMIUM_ANNUAL',
      planName: '프리미엄 연간',
      pricePerMonth: 3900,
      totalPrice: 46800,
      billingCycle: 'ANNUAL',
      discountRate: 20,
      features: ['무제한 추천', 'AI 정밀 취향 분석', '전체 이력 보관', '우선 고객 지원'],
    },
  ],
  currentPlan: 'FREE',
  promotionMessage: '🎉 지금 가입하면 7일 무료 체험!',
}

function formatCardNumber(value: string) {
  return value
    .replace(/\D/g, '')
    .slice(0, 16)
    .replace(/(\d{4})(?=\d)/g, '$1-')
}

export default function SubscriptionPage() {
  const toast = useToast()
  const { data: plansData, isLoading: isLoadingPlans, isError: isPlansError } = useSubscriptionPlans()
  const { data: subStatus } = useSubscriptionStatus()
  const effectivePlans = plansData ?? (isPlansError ? DEMO_PLANS : undefined)
  const isDemo = !plansData && isPlansError
  const { mutate: createSubscription, isPending: isPaying } = useCreateSubscription()
  const { mutate: cancelSubscription, isPending: isCancelling } = useCancelSubscription()
  const { mutate: extendTrial, isPending: isExtending } = useExtendTrial()

  const [showPaymentSheet, setShowPaymentSheet] = useState(false)
  const [showCancelModal, setShowCancelModal] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'>('PREMIUM_MONTHLY')
  const [cardNumber, setCardNumber] = useState('')
  const [cardExpiry, setCardExpiry] = useState('')
  const [cardCvc, setCardCvc] = useState('')
  const [cardholderName, setCardholderName] = useState('')
  const [autoRenewal, setAutoRenewal] = useState(false)
  const [withdrawalAck, setWithdrawalAck] = useState(false)
  const [subscriptionId, setSubscriptionId] = useState<string | null>(null)

  const isPremium = subStatus?.plan === 'PREMIUM' || effectivePlans?.currentPlan?.startsWith('PREMIUM')

  function handlePayment() {
    const rawCard = cardNumber.replace(/-/g, '')
    if (rawCard.length !== 16) {
      toast.error('카드 번호 16자리를 입력해주세요.')
      return
    }
    if (!autoRenewal || !withdrawalAck) {
      toast.error('자동 갱신 동의 및 청약철회권 확인이 필요해요.')
      return
    }
    const expiryParts = cardExpiry.split('/')
    const expiryMonth = parseInt(expiryParts[0] ?? '1', 10)
    const expiryYear = parseInt(`20${expiryParts[1] ?? '28'}`, 10)

    const paymentMethod: PaymentMethod = {
      type: 'CREDIT_CARD',
      cardNumber: rawCard.replace(/(\d{4})(?=\d)/g, '$1-'),
      expiryMonth,
      expiryYear,
      cvc: cardCvc,
      cardholderName: cardholderName || undefined,
    }

    if (isDemo) {
      setSubscriptionId(`demo-sub-${Date.now()}`)
      setShowPaymentSheet(false)
      toast.success('프리미엄 구독이 시작되었어요! (데모)')
      return
    }

    createSubscription(
      {
        planId: selectedPlan,
        paymentMethod,
        autoRenewalAgreed: autoRenewal,
        withdrawalRightAcknowledged: withdrawalAck,
      },
      {
        onSuccess: (data) => {
          setSubscriptionId(data.subscriptionId)
          setShowPaymentSheet(false)
          toast.success(data.message)
        },
        onError: (err: unknown) => {
          const axiosErr = err as { response?: { data?: { message?: string } } }
          toast.error(axiosErr?.response?.data?.message ?? '결제 중 오류가 발생했어요.')
        },
      },
    )
  }

  function handleCancelConfirm() {
    setShowCancelModal(false)
    if (isDemo) {
      toast.info('구독이 해지되었어요. (데모)')
      return
    }
    const subId = subscriptionId ?? 'sub-550e8400-e29b-41d4-a716-446655440050'
    cancelSubscription(
      { subscriptionId: subId, data: { cancelReason: 'COST' } },
      {
        onSuccess: (data) => {
          toast.info(data.message)
        },
        onError: () => toast.error('해지 처리 중 오류가 발생했어요.'),
      },
    )
  }

  function handleExtendTrial() {
    if (isDemo) {
      toast.success('7일 무료 체험이 연장되었어요! (데모)')
      return
    }
    extendTrial(undefined, {
      onSuccess: (data) => {
        toast.success(data.message)
      },
      onError: (err: unknown) => {
        const axiosErr = err as { response?: { data?: { message?: string } } }
        toast.error(axiosErr?.response?.data?.message ?? '연장 처리 중 오류가 발생했어요.')
      },
    })
  }

  if (isLoadingPlans && !isPlansError) return <Loading message="플랜 정보를 불러오는 중..." />

  return (
    <div className="px-[var(--margin-mobile)] py-[var(--space-m)]">
      {/* 전환 트리거 배너 */}
      <div className="mb-[var(--space-l)] rounded-[var(--radius-l)] bg-gradient-to-br from-[#FFF5F0] to-[#FEF3C7] p-[var(--space-l)] text-center">
        <img src="/images/premium-badge.png" alt="" aria-hidden="true" className="mx-auto mb-[var(--space-s)] h-16 w-16 object-contain" />
        <h1
          className="mb-[var(--space-xs)] text-[var(--font-size-h3)] font-semibold"
          style={{ lineHeight: 'var(--line-height-h3)' }}
        >
          30일간 쌓인 취향 데이터가<br />내일 사라져요. 유지하시겠어요?
        </h1>
        {effectivePlans?.promotionMessage && (
          <p className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            {effectivePlans.promotionMessage}
          </p>
        )}
      </div>

      {/* 현재 플랜 표시 */}
      {effectivePlans && (
        <div className="mb-[var(--space-m)] flex items-center justify-between rounded-[var(--radius-m)] border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3">
          <div>
            <div className="text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">현재 플랜</div>
            <div className="font-semibold">
              {effectivePlans.currentPlan === 'FREE' ? '무료' : '프리미엄'}
            </div>
          </div>
          <div className="text-right">
            <div className="text-[var(--font-size-h3)] font-bold">₩0</div>
            <div className="text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
              {effectivePlans.plans.find((p) => p.planId === 'FREE')?.features.join(' · ')}
            </div>
          </div>
        </div>
      )}

      {/* 결제 주기 선택 탭 */}
      <div className="mb-[var(--space-m)] flex rounded-[var(--radius-m)] bg-[var(--color-background)] p-1">
        <button
          onClick={() => setSelectedPlan('PREMIUM_MONTHLY')}
          className={`flex-1 rounded-[var(--radius-s)] py-2 text-center text-[var(--font-size-body2)] font-medium transition-colors ${
            selectedPlan === 'PREMIUM_MONTHLY'
              ? 'bg-[var(--color-surface)] text-[var(--color-primary)] shadow-[var(--shadow-1)]'
              : 'text-[var(--color-text-secondary)]'
          }`}
        >
          월간 결제
        </button>
        <button
          onClick={() => setSelectedPlan('PREMIUM_ANNUAL')}
          className={`flex-1 rounded-[var(--radius-s)] py-2 text-center text-[var(--font-size-body2)] font-medium transition-colors ${
            selectedPlan === 'PREMIUM_ANNUAL'
              ? 'bg-[var(--color-surface)] text-[var(--color-primary)] shadow-[var(--shadow-1)]'
              : 'text-[var(--color-text-secondary)]'
          }`}
        >
          연간 결제 <span className="text-[var(--font-size-caption)] text-[var(--color-primary)]">20% 할인</span>
        </button>
      </div>

      {/* 프리미엄 플랜 카드 */}
      {(() => {
        const plan = effectivePlans?.plans.find((p) => p.planId === selectedPlan)
        if (!plan) return null
        return (
          <div className="mb-[var(--space-l)] rounded-[var(--radius-l)] border-2 border-[var(--color-primary)] p-[var(--space-l)]">
            <div className="mb-[var(--space-m)] text-center">
              <span className="inline-block rounded-[var(--radius-xs)] bg-[var(--color-primary)] px-3 py-0.5 text-[var(--font-size-caption)] font-bold text-white">
                추천
              </span>
              <h2 className="mt-2 text-[var(--font-size-h3)] font-bold">⭐ 프리미엄</h2>
              <div className="mt-1">
                <span className="text-[var(--font-size-h2)] font-bold">
                  ₩{plan.pricePerMonth.toLocaleString()}
                </span>
                <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">/월</span>
              </div>
              {selectedPlan === 'PREMIUM_ANNUAL' && (
                <div className="mt-1 text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
                  연 ₩{plan.totalPrice.toLocaleString()} (월 ₩{plan.pricePerMonth.toLocaleString()})
                </div>
              )}
            </div>

            <div className="mb-[var(--space-l)] flex flex-col gap-3">
              {plan.features.map((f) => (
                <div key={f} className="flex items-center gap-3 text-[var(--font-size-body1)]">
                  <span className="text-[var(--color-primary)]" aria-hidden="true">★</span>
                  <span>{f}</span>
                </div>
              ))}
            </div>

            <Button
              variant="primary"
              size="full"
              disabled={isPremium}
              onClick={() => setShowPaymentSheet(true)}
            >
              {isPremium ? '이용 중' : '7일 무료 체험 시작'}
            </Button>
            <p className="mt-2 text-center text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
              7일 무료 체험 후 자동 결제
            </p>
          </div>
        )
      })()}

      {/* 법적 고지사항 */}
      <div
        role="region"
        aria-label="법적 고지사항"
        className="mb-[var(--space-l)] rounded-[var(--radius-s)] bg-[var(--color-background)] p-[var(--space-m)]"
      >
        {['청약철회권 7일 보장', '언제든 해지 가능', '자동 갱신 결제 (매월/매년)', '무료 체험 기간 내 해지 시 비용 없음'].map(
          (item) => (
            <div key={item} className="flex items-start gap-2 py-1 text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
              <span>•</span>
              <span>{item}</span>
            </div>
          ),
        )}
      </div>

      {/* 구독 해지 영역 (프리미엄 가입 후 표시) */}
      {isPremium && (
        <div className="mt-[var(--space-xl)] border-t border-[var(--color-border)] pt-[var(--space-l)]">
          <p className="mb-[var(--space-m)] text-[var(--font-size-label)] text-[var(--color-text-secondary)]">구독 해지</p>
          <div className="flex flex-col gap-2">
            <Button
              variant="secondary"
              size="full"
              loading={isExtending}
              onClick={handleExtendTrial}
            >
              7일 무료 연장하기
            </Button>
            <Button
              variant="secondary"
              size="full"
              disabled={isCancelling}
              onClick={() => setShowCancelModal(true)}
            >
              구독 해지하기
            </Button>
          </div>
        </div>
      )}

      {/* 결제 바텀시트 */}
      <BottomSheet
        isOpen={showPaymentSheet}
        onClose={() => setShowPaymentSheet(false)}
        title="결제 방식 선택"
        ariaLabel="결제"
      >
        {/* 플랜 선택 */}
        <div role="radiogroup" aria-label="결제 주기" className="mb-[var(--space-m)] flex gap-[var(--space-s)]">
          {(['PREMIUM_MONTHLY', 'PREMIUM_ANNUAL'] as const).map((planId) => (
            <button
              key={planId}
              role="radio"
              aria-checked={selectedPlan === planId}
              onClick={() => setSelectedPlan(planId)}
              className={`flex-1 rounded-[var(--radius-s)] border p-[var(--space-m)] text-center text-[var(--font-size-body2)] transition-colors ${
                selectedPlan === planId
                  ? 'border-[var(--color-primary)] bg-orange-50'
                  : 'border-[var(--color-border)]'
              }`}
            >
              <div className="font-semibold">
                {planId === 'PREMIUM_MONTHLY' ? '월간' : '연간'}
              </div>
              <div className="mt-1">
                {planId === 'PREMIUM_MONTHLY' ? '₩4,900/월' : '₩3,900/월'}
              </div>
              {planId === 'PREMIUM_ANNUAL' && (
                <div className="mt-0.5 text-[var(--font-size-caption)] text-[var(--color-primary)]">20% 할인</div>
              )}
            </button>
          ))}
        </div>

        {/* 카드 입력 */}
        <div className="mb-[var(--space-m)] flex flex-col gap-[var(--space-s)]">
          <div>
            <label htmlFor="cardNumber" className="mb-1 block text-[var(--font-size-body2)] font-medium">
              카드 번호
            </label>
            <input
              id="cardNumber"
              type="text"
              inputMode="numeric"
              placeholder="0000-0000-0000-0000"
              maxLength={19}
              value={cardNumber}
              onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
              className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body1)] focus:border-[var(--color-primary)] focus:outline-none"
            />
          </div>
          <div className="flex gap-[var(--space-s)]">
            <div className="flex-1">
              <label htmlFor="cardExpiry" className="mb-1 block text-[var(--font-size-body2)] font-medium">
                유효기간
              </label>
              <input
                id="cardExpiry"
                type="text"
                inputMode="numeric"
                placeholder="MM/YY"
                maxLength={5}
                value={cardExpiry}
                onChange={(e) => {
                  const v = e.target.value.replace(/\D/g, '').slice(0, 4)
                  setCardExpiry(v.length > 2 ? `${v.slice(0, 2)}/${v.slice(2)}` : v)
                }}
                className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body1)] focus:border-[var(--color-primary)] focus:outline-none"
              />
            </div>
            <div className="flex-1">
              <label htmlFor="cardCvc" className="mb-1 block text-[var(--font-size-body2)] font-medium">
                CVC
              </label>
              <input
                id="cardCvc"
                type="text"
                inputMode="numeric"
                placeholder="000"
                maxLength={4}
                value={cardCvc}
                onChange={(e) => setCardCvc(e.target.value.replace(/\D/g, '').slice(0, 4))}
                className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body1)] focus:border-[var(--color-primary)] focus:outline-none"
              />
            </div>
          </div>
          <div>
            <label htmlFor="cardholderName" className="mb-1 block text-[var(--font-size-body2)] font-medium">
              카드 소유자 이름 (선택)
            </label>
            <input
              id="cardholderName"
              type="text"
              placeholder="홍길동"
              value={cardholderName}
              onChange={(e) => setCardholderName(e.target.value)}
              className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body1)] focus:border-[var(--color-primary)] focus:outline-none"
            />
          </div>
        </div>

        {/* 동의 체크박스 */}
        <div className="mb-[var(--space-m)] flex flex-col gap-2">
          <label className="flex items-center gap-2 text-[var(--font-size-body2)] cursor-pointer">
            <input
              type="checkbox"
              checked={autoRenewal}
              onChange={(e) => setAutoRenewal(e.target.checked)}
              className="h-4 w-4 rounded accent-[var(--color-primary)]"
            />
            자동 갱신 결제에 동의합니다
          </label>
          <label className="flex items-center gap-2 text-[var(--font-size-body2)] cursor-pointer">
            <input
              type="checkbox"
              checked={withdrawalAck}
              onChange={(e) => setWithdrawalAck(e.target.checked)}
              className="h-4 w-4 rounded accent-[var(--color-primary)]"
            />
            청약철회권 7일 고지를 확인했습니다
          </label>
        </div>

        <Button
          variant="primary"
          size="full"
          loading={isPaying}
          disabled={!autoRenewal || !withdrawalAck}
          onClick={handlePayment}
        >
          결제하기
        </Button>
        <p className="mt-[var(--space-s)] text-center text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
          7일 무료 체험 후 자동 결제됩니다
        </p>
      </BottomSheet>

      {/* 해지 확인 모달 */}
      <Modal
        isOpen={showCancelModal}
        onClose={() => setShowCancelModal(false)}
        title="정말 해지하시겠어요?"
        primaryLabel="해지하기"
        onPrimary={handleCancelConfirm}
        primaryVariant="danger"
        secondaryLabel="유지하기"
        onSecondary={() => setShowCancelModal(false)}
      >
        무료 전환 후 30일 이전 기록은 열람할 수 없어요.
        <br /><br />
        해지 전 7일 무료 연장을 드릴까요?
      </Modal>
    </div>
  )
}
