'use client'

/**
 * 위치 동의 페이지
 * UFR-MBR-030: 위치정보법 고지 + 동의/거절
 * POST /api/v1/members/location-consent
 */
import { useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { useSubmitLocationConsent } from '@/hooks/useMember'
import { useToast } from '@/hooks/useToast'

export default function OnboardingLocationPage() {
  const router = useRouter()
  const toast = useToast()
  const { mutate: submitConsent, isPending } = useSubmitLocationConsent()

  function handleConsent(agreed: boolean) {
    submitConsent(
      {
        consented: agreed,
        consentedAt: new Date().toISOString(),
      },
      {
        onSuccess: (data) => {
          if (agreed) {
            toast.success(data.message ?? '위치 기반 추천이 활성화되었어요.')
          } else {
            toast.info(data.message ?? '위치를 직접 입력해주세요')
          }
          router.push('/onboarding/dietary')
        },
        onError: () => {
          toast.error('위치 동의 처리 중 오류가 발생했어요.')
        },
      },
    )
  }

  return (
    <main
      role="main"
      className="flex min-h-dvh flex-col items-center justify-center bg-[var(--color-surface)] px-[var(--margin-mobile)] py-[var(--space-xl)]"
    >
      <img
        src="/images/location-consent.png"
        alt=""
        aria-hidden="true"
        className="mb-[var(--space-l)] h-32 w-32 object-contain"
      />

      <h1
        className="mb-[var(--space-s)] text-center text-[var(--font-size-h2)] font-bold"
        style={{ lineHeight: 'var(--line-height-h2)' }}
      >
        위치 정보를 허용해주세요
      </h1>

      <p
        className="mb-[var(--space-l)] max-w-xs text-center text-[var(--font-size-body1)] text-[var(--color-text-secondary)]"
        style={{ lineHeight: 'var(--line-height-body1)' }}
      >
        주변 식당을 추천하기 위해<br />위치 정보가 필요해요
      </p>

      <div
        role="region"
        aria-label="위치 정보 수집 안내"
        className="mb-[var(--space-xl)] w-full max-w-sm rounded-[var(--radius-m)] bg-[var(--color-background)] p-[var(--space-m)]"
      >
        <div className="flex items-start gap-2 border-b border-[var(--color-border)] py-[var(--space-s)]">
          <span className="w-5 shrink-0" aria-hidden="true">📋</span>
          <span className="w-20 shrink-0 text-[var(--font-size-body2)] font-medium text-[var(--color-text-secondary)]">수집 목적</span>
          <span className="text-[var(--font-size-body2)]">주변 식당 추천 및 거리 계산</span>
        </div>
        <div className="flex items-start gap-2 border-b border-[var(--color-border)] py-[var(--space-s)]">
          <span className="w-5 shrink-0" aria-hidden="true">⏰</span>
          <span className="w-20 shrink-0 text-[var(--font-size-body2)] font-medium text-[var(--color-text-secondary)]">보유 기간</span>
          <span className="text-[var(--font-size-body2)]">6개월 (이후 자동 삭제)</span>
        </div>
        <div className="flex items-start gap-2 py-[var(--space-s)]">
          <span className="w-5 shrink-0" aria-hidden="true">🔒</span>
          <span className="w-20 shrink-0 text-[var(--font-size-body2)] font-medium text-[var(--color-text-secondary)]">철회 방법</span>
          <span className="text-[var(--font-size-body2)]">프로필 &gt; 설정에서 언제든 철회</span>
        </div>
      </div>

      <div className="flex w-full max-w-sm flex-col gap-[var(--space-s)]">
        <Button variant="primary" size="full" loading={isPending} onClick={() => handleConsent(true)}>
          동의하고 계속하기
        </Button>
        <Button variant="secondary" size="full" disabled={isPending} onClick={() => handleConsent(false)}>
          나중에 할게요
        </Button>
      </div>

      <p className="mt-[var(--space-m)] text-center text-[var(--font-size-caption)] text-[var(--color-text-disabled)]">
        동의하지 않으면 위치를 직접 입력해 주셔야 해요
      </p>
    </main>
  )
}
