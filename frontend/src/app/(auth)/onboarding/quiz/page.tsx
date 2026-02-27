'use client'

/**
 * 취향 퀴즈 페이지
 * UFR-MBR-020: 음식 카드 스와이프 취향 수집
 * POST /api/v1/members/onboarding
 * PUT /api/v1/members/onboarding/progress (중간 저장)
 */
import { useState, useCallback, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { useSubmitOnboarding, useSaveOnboardingProgress } from '@/hooks/useMember'
import { useOnboardingStore } from '@/store/onboardingStore'
import { useToast } from '@/hooks/useToast'

const FOOD_CARDS = [
  { cardId: 'card-korean-001', emoji: '🍲', name: '된장찌개', category: '한식', tags: '따뜻한 · 구수한 · 집밥 느낌' },
  { cardId: 'card-japanese-001', emoji: '🍱', name: '스시 런치', category: '일식', tags: '신선한 · 가벼운 · 깔끔한' },
  { cardId: 'card-chinese-001', emoji: '🥟', name: '짜장면', category: '중식', tags: '든든한 · 달콤짭짤한' },
  { cardId: 'card-western-001', emoji: '🥩', name: '스테이크 세트', category: '양식', tags: '고급스러운 · 고기 · 특별한' },
  { cardId: 'card-bunsik-001', emoji: '🍜', name: '떡볶이+순대', category: '분식', tags: '매콤한 · 저렴한 · 간식 같은' },
  { cardId: 'card-salad-001', emoji: '🥗', name: '샐러드 볼', category: '샐러드/건강식', tags: '건강한 · 가벼운 · 신선한' },
  { cardId: 'card-fastfood-001', emoji: '🍔', name: '버거 세트', category: '패스트푸드', tags: '빠른 · 든든한 · 간편한' },
  { cardId: 'card-korean-002', emoji: '🍖', name: '삼겹살', category: '한식', tags: '술자리 · 고기 · 회식' },
  { cardId: 'card-asian-001', emoji: '🍛', name: '카레 라이스', category: '아시안', tags: '향긋한 · 이국적인 · 독특한' },
  { cardId: 'card-korean-003', emoji: '🍚', name: '비빔밥', category: '한식', tags: '건강한 · 색깔있는 · 채소 풍부' },
]

export default function OnboardingQuizPage() {
  const router = useRouter()
  const toast = useToast()
  const { swipeResults, addSwipeResult, resetOnboarding, isMinCompleted } = useOnboardingStore()
  const { mutate: submitOnboarding, isPending: isSubmitting } = useSubmitOnboarding()
  const { mutate: saveProgress } = useSaveOnboardingProgress()

  // SSR/클라이언트 불일치 방지: 초기값은 0으로 고정, useEffect에서 localStorage 복원
  const [currentIndex, setCurrentIndex] = useState(0)
  const [animating, setAnimating] = useState<'left' | 'right' | null>(null)

  useEffect(() => {
    setCurrentIndex(swipeResults.length)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps
  const [isCompleted, setIsCompleted] = useState(false)
  const [topCategories, setTopCategories] = useState<string[]>([])

  const total = FOOD_CARDS.length
  const progress = (currentIndex / total) * 100
  const remainingCards = total - currentIndex

  const handleSwipe = useCallback(
    (liked: boolean) => {
      if (currentIndex >= total || animating) return
      const card = FOOD_CARDS[currentIndex]
      const direction = liked ? 'right' : 'left'
      setAnimating(direction)

      setTimeout(() => {
        addSwipeResult({ cardId: card.cardId, liked, category: card.category })
        const newIndex = currentIndex + 1
        setCurrentIndex(newIndex)
        setAnimating(null)

        // 중간 저장: 3장마다 자동 저장
        if (newIndex % 3 === 0 && newIndex < total) {
          saveProgress({
            swipeResults: [
              ...swipeResults,
              { cardId: card.cardId, liked, category: card.category },
            ],
          })
        }
      }, 350)
    },
    [currentIndex, total, animating, addSwipeResult, saveProgress, swipeResults],
  )

  function handleComplete() {
    submitOnboarding(
      {
        swipeResults: swipeResults,
        healthInfoConsentGiven: true,
      },
      {
        onSuccess: (data) => {
          setTopCategories(data.topCategories)
          setIsCompleted(true)
          resetOnboarding()
        },
        onError: () => {
          toast.error('취향 저장 중 오류가 발생했어요. 다시 시도해주세요.')
        },
      },
    )
  }

  function handleSkip() {
    router.push('/onboarding/location')
  }

  if (isCompleted) {
    return (
      <main className="flex min-h-dvh flex-col items-center justify-center bg-[var(--color-surface)] px-[var(--margin-mobile)] text-center">
        <img src="/images/onboarding-complete.png" alt="" aria-hidden="true" className="mb-[var(--space-m)] h-32 w-32 object-contain" />
        <h2 className="mb-[var(--space-s)] text-[var(--font-size-h2)] font-bold">취향 프로파일 완성!</h2>
        <p className="mb-[var(--space-l)] text-[var(--font-size-body1)] text-[var(--color-text-secondary)]">
          선호 카테고리 Top 3: {topCategories.join(', ')}
        </p>
        <Button variant="primary" size="full" onClick={() => router.push('/onboarding/location')}>
          다음으로
        </Button>
      </main>
    )
  }

  const currentCard = currentIndex < total ? FOOD_CARDS[currentIndex] : null
  const nextCard = currentIndex + 1 < total ? FOOD_CARDS[currentIndex + 1] : null

  return (
    <main
      role="main"
      className="flex min-h-dvh flex-col items-center bg-[var(--color-surface)] px-[var(--margin-mobile)] py-[var(--space-l)]"
    >
      {/* 헤더 */}
      <div className="mb-[var(--space-l)] flex w-full max-w-sm justify-between">
        <div />
        <button
          className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]"
          onClick={handleSkip}
          aria-label="퀴즈 건너뛰기"
        >
          건너뛰기
        </button>
      </div>

      <h1 className="mb-[var(--space-s)] text-center text-[var(--font-size-h2)] font-bold">
        당신의 취향을 알려주세요!
      </h1>
      <p className="mb-[var(--space-l)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
        좋으면 오른쪽, 싫으면 왼쪽으로 넘겨주세요
      </p>

      {/* 카드 영역 */}
      <div
        role="region"
        aria-label="음식 카드 스와이프"
        className="relative mb-[var(--space-l)] h-[360px] w-[280px]"
      >
        {/* 뒷 카드 (다음 카드 미리보기) */}
        {nextCard && (
          <div className="absolute inset-0 scale-[0.96] rounded-[var(--radius-l)] bg-[var(--color-surface)] shadow-[var(--shadow-1)] overflow-hidden opacity-70">
            <div className="flex h-[70%] items-center justify-center bg-gradient-to-br from-[#FFF5F0] to-[#FEF3C7] text-[80px]">
              {nextCard.emoji}
            </div>
          </div>
        )}

        {/* 현재 카드 */}
        {currentCard && (
          <div
            role="article"
            aria-label={currentCard.name}
            className={`absolute inset-0 overflow-hidden rounded-[var(--radius-l)] bg-[var(--color-surface)] shadow-[var(--shadow-2)] transition-all duration-[350ms] ease-in-out ${
              animating === 'right'
                ? 'translate-x-[150%] rotate-[15deg] opacity-0'
                : animating === 'left'
                  ? 'translate-x-[-150%] rotate-[-15deg] opacity-0'
                  : ''
            }`}
          >
            <div className="flex h-[70%] items-center justify-center bg-gradient-to-br from-[#FFF5F0] to-[#FEF3C7] text-[80px]">
              {currentCard.emoji}
            </div>
            <div className="p-[var(--space-m)] text-center">
              <div className="mb-[var(--space-s)] text-[var(--font-size-h3)] font-semibold">
                {currentCard.name}
              </div>
              <div className="text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
                {currentCard.tags}
              </div>
            </div>
          </div>
        )}

        {/* 모든 카드 완료 */}
        {!currentCard && (
          <div className="absolute inset-0 flex items-center justify-center rounded-[var(--radius-l)] bg-[var(--color-background)]">
            <p className="text-[var(--font-size-body1)] text-[var(--color-text-secondary)]">모든 카드를 완료했어요!</p>
          </div>
        )}
      </div>

      {/* 좋아요/싫어요 버튼 */}
      <div className="mb-[var(--space-l)] flex gap-[var(--space-xl)]">
        <button
          onClick={() => handleSwipe(false)}
          disabled={!currentCard || !!animating}
          aria-label="싫어요"
          className="flex h-[72px] w-[72px] items-center justify-center rounded-full border-2 border-[var(--color-error)] bg-[var(--color-surface)] text-[32px] transition-transform duration-[var(--duration-fast)] hover:scale-110 hover:bg-red-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          👎
        </button>
        <button
          onClick={() => handleSwipe(true)}
          disabled={!currentCard || !!animating}
          aria-label="좋아요"
          className="flex h-[72px] w-[72px] items-center justify-center rounded-full border-2 border-[var(--color-success)] bg-[var(--color-surface)] text-[32px] transition-transform duration-[var(--duration-fast)] hover:scale-110 hover:bg-green-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          👍
        </button>
      </div>

      {/* 진행 바 */}
      <div className="mb-[var(--space-s)] w-full max-w-[280px]">
        <div className="h-1.5 overflow-hidden rounded-full bg-[var(--color-border)]">
          <div
            className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>
      <p className="mb-[var(--space-s)] text-center text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
        {currentIndex}/{total}
      </p>

      {/* 격려 문구 */}
      <p className="mb-[var(--space-l)] min-h-[20px] text-center text-[var(--font-size-body2)] font-medium text-[var(--color-primary)]">
        {currentIndex < 7
          ? '조금만 더! 3분이면 끝나요'
          : currentIndex < total
            ? '거의 다 됐어요! 💪'
            : ''}
      </p>

      {/* 완료 버튼 (7장 이상 완료 시 또는 전체 완료 시 활성화) */}
      {(isMinCompleted() || currentIndex >= total) && (
        <Button
          variant="primary"
          size="full"
          loading={isSubmitting}
          onClick={handleComplete}
          className="max-w-sm"
        >
          취향 분석 완료!
        </Button>
      )}
    </main>
  )
}
