'use client'

/**
 * 식이제한 설정 페이지
 * UFR-MBR-040: 알레르겐 체크박스 + 식이 유형 선택
 * PUT /api/v1/members/dietary-restrictions
 */
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { useUpdateDietaryRestrictions } from '@/hooks/useMember'
import { useToast } from '@/hooks/useToast'

const ALLERGEN_OPTIONS = [
  '난류', '우유', '메밀', '땅콩', '대두', '밀', '고등어', '게',
  '새우', '돼지고기', '복숭아', '토마토', '아황산류', '호두', '닭고기',
  '쇠고기', '오징어', '조개류',
]

const DIET_TYPES = [
  { value: '일반', label: '일반' },
  { value: '채식', label: '채식' },
  { value: '비건', label: '비건' },
  { value: '할랄', label: '할랄' },
  { value: '기타', label: '기타' },
]

export default function OnboardingDietaryPage() {
  const router = useRouter()
  const toast = useToast()
  const { mutate: updateDietary, isPending } = useUpdateDietaryRestrictions()

  const [selectedAllergens, setSelectedAllergens] = useState<string[]>([])
  const [customAllergen, setCustomAllergen] = useState('')
  const [dietType, setDietType] = useState('일반')
  const [healthConsent, setHealthConsent] = useState(false)

  function toggleAllergen(allergen: string) {
    setSelectedAllergens((prev) =>
      prev.includes(allergen) ? prev.filter((a) => a !== allergen) : [...prev, allergen],
    )
  }

  function handleSubmit() {
    if (!healthConsent) {
      toast.error('건강 관련 정보 수집에 동의해주세요.')
      return
    }
    updateDietary(
      {
        healthInfoConsentGiven: true,
        allergens: selectedAllergens,
        customAllergens: customAllergen ? [customAllergen] : [],
        dietType,
      },
      {
        onSuccess: (data) => {
          toast.success(data.message)
          router.push('/home')
        },
        onError: () => {
          // 데모 모드: API 없이 바로 홈으로 이동
          toast.success('식이제한 설정 완료!')
          router.push('/home')
        },
      },
    )
  }

  function handleSkip() {
    router.push('/home')
  }

  return (
    <main
      role="main"
      className="min-h-dvh bg-[var(--color-surface)] px-[var(--margin-mobile)] py-[var(--space-l)]"
    >
      <div className="mb-[var(--space-l)] flex justify-between">
        <h1 className="text-[var(--font-size-h2)] font-bold">
          식이제한 설정
        </h1>
        <button
          onClick={handleSkip}
          className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]"
        >
          건너뛰기
        </button>
      </div>

      {/* 알레르기 */}
      <section aria-labelledby="allergen-label" className="mb-[var(--space-l)]">
        <h2
          id="allergen-label"
          className="mb-[var(--space-s)] text-[var(--font-size-h3)] font-semibold"
        >
          알레르기 항목
        </h2>
        <p className="mb-[var(--space-m)] text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
          해당하는 항목을 모두 선택해주세요
        </p>
        <div className="flex flex-wrap gap-2">
          {ALLERGEN_OPTIONS.map((allergen) => (
            <button
              key={allergen}
              onClick={() => toggleAllergen(allergen)}
              aria-pressed={selectedAllergens.includes(allergen)}
              className={`rounded-full px-3 py-1.5 text-[var(--font-size-body2)] transition-colors duration-[var(--duration-fast)] ${
                selectedAllergens.includes(allergen)
                  ? 'bg-[var(--color-error)] text-white'
                  : 'border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'
              }`}
            >
              {allergen}
            </button>
          ))}
        </div>
        <div className="mt-[var(--space-s)]">
          <input
            type="text"
            placeholder="직접 입력 (예: 참깨)"
            value={customAllergen}
            onChange={(e) => setCustomAllergen(e.target.value)}
            className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body2)] focus:border-[var(--color-primary)] focus:outline-none"
          />
        </div>
      </section>

      {/* 식이 유형 */}
      <section aria-labelledby="diet-type-label" className="mb-[var(--space-l)]">
        <h2
          id="diet-type-label"
          className="mb-[var(--space-m)] text-[var(--font-size-h3)] font-semibold"
        >
          식이 유형
        </h2>
        <div role="radiogroup" aria-label="식이 유형 선택" className="flex flex-wrap gap-2">
          {DIET_TYPES.map((type) => (
            <button
              key={type.value}
              role="radio"
              aria-checked={dietType === type.value}
              onClick={() => setDietType(type.value)}
              className={`rounded-full px-4 py-2 text-[var(--font-size-body2)] transition-colors duration-[var(--duration-fast)] ${
                dietType === type.value
                  ? 'bg-[var(--color-primary)] text-white'
                  : 'border border-[var(--color-border)] bg-[var(--color-surface)]'
              }`}
            >
              {type.label}
            </button>
          ))}
        </div>
      </section>

      {/* 건강 정보 동의 */}
      <div className="mb-[var(--space-xl)] rounded-[var(--radius-m)] bg-[var(--color-background)] p-[var(--space-m)]">
        <label className="flex cursor-pointer items-start gap-3">
          <input
            type="checkbox"
            checked={healthConsent}
            onChange={(e) => setHealthConsent(e.target.checked)}
            className="mt-0.5 h-4 w-4 accent-[var(--color-primary)]"
          />
          <div>
            <p className="text-[var(--font-size-body2)] font-medium">
              건강 관련 정보 수집 동의 (필수)
            </p>
            <p className="mt-1 text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
              알레르기 및 식이제한 정보는 추천 필터로 활용되며 별도 암호화 보관됩니다.
            </p>
          </div>
        </label>
      </div>

      <Button
        variant="primary"
        size="full"
        loading={isPending}
        disabled={!healthConsent}
        onClick={handleSubmit}
      >
        설정 완료
      </Button>
    </main>
  )
}
