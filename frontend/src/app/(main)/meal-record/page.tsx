'use client'

/**
 * 식사 기록 + 피드백 페이지
 * UFR-REC-070: 원탭 식사 기록 (POST /api/v1/meals)
 * UFR-REC-080: 30초 실행 취소 (DELETE /api/v1/meals/{id})
 * UFR-REC-090: 피드백 제출 (POST /api/v1/meals/{id}/feedback)
 */
import { useState, useEffect, useRef, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { useCreateMeal, useDeleteMeal, useSubmitFeedback } from '@/hooks/useRecommendation'
import { useToast } from '@/hooks/useToast'
import type { FeedbackRequest } from '@/types/recommendation'

const FEEDBACK_KEYWORDS: { label: string; value: 'TASTE' | 'PRICE' | 'KINDNESS' }[] = [
  { label: '🍴 맛', value: 'TASTE' },
  { label: '💰 가격', value: 'PRICE' },
  { label: '😊 친절', value: 'KINDNESS' },
]

const UNDO_SECONDS = 30

function MealRecordContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const toast = useToast()

  const restaurantName = searchParams.get('name') ?? '광화문 된장마을'
  const restaurantId = searchParams.get('restaurantId') ?? 'rest-001'
  const recId = searchParams.get('recId') ?? undefined

  const [mealId, setMealId] = useState<string | null>(null)
  const [isRecorded, setIsRecorded] = useState(false)
  const [undoSeconds, setUndoSeconds] = useState(UNDO_SECONDS)
  const [showUndo, setShowUndo] = useState(false)
  const [showFeedback, setShowFeedback] = useState(false)
  const [satisfaction, setSatisfaction] = useState<'GOOD' | 'BAD' | null>(null)
  const [keyword, setKeyword] = useState<'TASTE' | 'PRICE' | 'KINDNESS' | null>(null)
  const [feedbackDone, setFeedbackDone] = useState(false)
  const [totalFeedbackCount, setTotalFeedbackCount] = useState<number | null>(null)

  const undoTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const { mutate: createMeal, isPending: isCreating } = useCreateMeal()
  const { mutate: deleteMeal, isPending: isDeleting } = useDeleteMeal()
  const { mutate: submitFeedback, isPending: isSubmittingFeedback } = useSubmitFeedback()

  useEffect(() => {
    return () => {
      if (undoTimerRef.current) clearInterval(undoTimerRef.current)
    }
  }, [])

  function startUndoTimer() {
    setUndoSeconds(UNDO_SECONDS)
    setShowUndo(true)
    undoTimerRef.current = setInterval(() => {
      setUndoSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(undoTimerRef.current!)
          setShowUndo(false)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const isDemo = restaurantId.startsWith('rest-demo')

  function handleRecord() {
    // 데모 모드: API 없이 로컬에서 기록 처리
    if (isDemo) {
      const demoMealId = `demo-meal-${Date.now()}`
      setMealId(demoMealId)
      setIsRecorded(true)
      toast.success(`${restaurantName} 식사 기록 완료!`)
      startUndoTimer()
      setTimeout(() => setShowFeedback(true), 800)
      return
    }

    createMeal(
      {
        recommendationId: recId ?? null,
        restaurantId,
        menuName: undefined,
        recordedAt: new Date().toLocaleString('sv-SE', { timeZone: 'Asia/Seoul' }).replace(' ', 'T'),
      },
      {
        onSuccess: (data) => {
          setMealId(data.mealId)
          setIsRecorded(true)
          toast.success(data.message)
          startUndoTimer()
          setTimeout(() => setShowFeedback(true), 800)
        },
        onError: (err: unknown) => {
          const axiosErr = err as { response?: { data?: { data?: null; error?: { error?: string; message?: string } } } }
          const errorData = axiosErr?.response?.data?.error
          const responseData = (err as { response?: { data?: { data?: { mealId?: string; duplicate?: boolean } } } })?.response?.data?.data
          if (responseData?.duplicate && responseData?.mealId) {
            toast.info('이미 기록되었어요. 수정하시겠어요?', {
              action: {
                label: '수정하기',
                onClick: () => {
                  setMealId(responseData.mealId!)
                  setIsRecorded(true)
                  setShowFeedback(true)
                },
              },
            })
          } else if (errorData?.message) {
            toast.error(errorData.message)
          } else {
            toast.error('기록 중 오류가 발생했어요.')
          }
        },
      },
    )
  }

  function handleUndo() {
    if (!mealId) return
    clearInterval(undoTimerRef.current!)
    setShowUndo(false)

    if (isDemo) {
      setIsRecorded(false)
      setMealId(null)
      setShowFeedback(false)
      setSatisfaction(null)
      setKeyword(null)
      toast.info('기록이 취소되었어요')
      return
    }

    deleteMeal(mealId, {
      onSuccess: () => {
        setIsRecorded(false)
        setMealId(null)
        setShowFeedback(false)
        setSatisfaction(null)
        setKeyword(null)
        toast.info('기록이 취소되었어요')
      },
      onError: (err: unknown) => {
        const axiosErr = err as { response?: { data?: { error?: string; message?: string } } }
        if (axiosErr?.response?.data?.error === 'CANCEL_TIMEOUT') {
          toast.info('이력 화면에서 수정할 수 있어요.')
        } else {
          toast.error('취소 처리 중 오류가 발생했어요.')
        }
      },
    })
  }

  function handleSubmitFeedback() {
    if (!mealId || !satisfaction) {
      toast.info('좋아요 또는 별로를 선택해주세요')
      return
    }

    if (isDemo) {
      setFeedbackDone(true)
      setTotalFeedbackCount(7)
      toast.success('피드백 감사합니다!')
      return
    }

    const feedbackData: FeedbackRequest = { satisfaction, keyword: keyword ?? null }
    submitFeedback(
      { mealId, data: feedbackData },
      {
        onSuccess: (res) => {
          setFeedbackDone(true)
          setTotalFeedbackCount(res.totalFeedbackCount)
          toast.success(res.message)
        },
        onError: () => toast.error('피드백 제출 중 오류가 발생했어요.'),
      },
    )
  }

  function handleSkipFeedback() {
    if (!mealId) return

    if (isDemo) {
      setFeedbackDone(true)
      return
    }

    submitFeedback(
      { mealId, data: { satisfaction: 'NEUTRAL', keyword: null } },
      {
        onSuccess: (res) => {
          setFeedbackDone(true)
          setTotalFeedbackCount(res.totalFeedbackCount)
        },
        onError: () => {
          setFeedbackDone(true)
        },
      },
    )
  }

  const now = new Date()
  const timeStr = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`

  return (
    <div className="px-[var(--margin-mobile)]">
      {/* 식사 기록 섹션 */}
      <section className="py-[var(--space-xl)] text-center" aria-label="식사 기록">
        <h1 className="mb-[var(--space-m)] text-[var(--font-size-h3)] font-semibold">
          {restaurantName}에서 식사하셨나요?
        </h1>

        <div className="mb-[var(--space-l)] inline-flex items-center gap-[var(--space-s)] rounded-[var(--radius-m)] bg-[var(--color-background)] px-[var(--space-l)] py-[var(--space-m)]">
          <div>
            <div className="font-semibold">{restaurantName}</div>
            <div className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">오늘 {timeStr}</div>
          </div>
        </div>

        <div className="mb-[var(--space-xl)]">
          <button
            id="recordBtn"
            onClick={handleRecord}
            disabled={isRecorded || isCreating}
            aria-label="식사 기록하기"
            className={`h-[120px] w-[120px] rounded-full border-none font-bold shadow-[var(--shadow-3)] transition-all duration-[var(--duration-fast)] ${
              isRecorded
                ? 'bg-[var(--color-success)] text-[48px]'
                : 'cursor-pointer bg-[var(--color-primary)] text-[var(--font-size-h3)] text-white hover:scale-105 hover:shadow-[var(--shadow-4)] active:scale-95'
            } disabled:cursor-not-allowed disabled:opacity-60`}
          >
            {isCreating ? (
              <span className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-white border-t-transparent" />
            ) : isRecorded ? (
              '✅'
            ) : (
              '먹었어요!'
            )}
          </button>
        </div>
      </section>

      {/* 피드백 섹션 */}
      {showFeedback && !feedbackDone && (
        <section
          className="border-t border-[var(--color-border)] py-[var(--space-l)]"
          aria-label="식사 피드백"
        >
          <h2 className="mb-[var(--space-l)] text-center text-[var(--font-size-h3)] font-semibold">
            오늘 점심 어땠어요?
          </h2>

          <div role="radiogroup" aria-label="만족도 선택" className="mb-[var(--space-m)] flex justify-center gap-[var(--space-xl)]">
            <button
              id="btnGood"
              onClick={() => setSatisfaction('GOOD')}
              aria-label="좋아요"
              aria-pressed={satisfaction === 'GOOD'}
              className={`flex h-[72px] w-[72px] items-center justify-center rounded-full border-2 text-[40px] transition-all duration-[var(--duration-fast)] ${
                satisfaction === 'GOOD'
                  ? 'border-[var(--color-success)] bg-green-50 scale-110'
                  : satisfaction === 'BAD'
                    ? 'opacity-40'
                    : 'border-[var(--color-border)]'
              }`}
            >
              👍
            </button>
            <button
              id="btnBad"
              onClick={() => setSatisfaction('BAD')}
              aria-label="별로"
              aria-pressed={satisfaction === 'BAD'}
              className={`flex h-[72px] w-[72px] items-center justify-center rounded-full border-2 text-[40px] transition-all duration-[var(--duration-fast)] ${
                satisfaction === 'BAD'
                  ? 'border-[var(--color-error)] bg-red-50 scale-110'
                  : satisfaction === 'GOOD'
                    ? 'opacity-40'
                    : 'border-[var(--color-border)]'
              }`}
            >
              👎
            </button>
          </div>

          <p className="mb-[var(--space-m)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            한 가지만 더! (선택)
          </p>

          <div
            role="group"
            aria-label="키워드 선택"
            className="mb-[var(--space-l)] flex justify-center gap-[var(--space-s)]"
          >
            {FEEDBACK_KEYWORDS.map((kw) => (
              <button
                key={kw.value}
                onClick={() => setKeyword(keyword === kw.value ? null : kw.value)}
                aria-pressed={keyword === kw.value}
                className={`rounded-full px-4 py-2 text-[var(--font-size-body2)] transition-colors duration-[var(--duration-fast)] ${
                  keyword === kw.value
                    ? 'bg-[var(--color-primary)] text-white'
                    : 'border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'
                }`}
              >
                {kw.label}
              </button>
            ))}
          </div>

          <div className="flex justify-center gap-[var(--space-m)]">
            <Button
              variant="primary"
              size="md"
              loading={isSubmittingFeedback}
              disabled={!satisfaction}
              onClick={handleSubmitFeedback}
            >
              피드백 완료
            </Button>
            <Button
              variant="secondary"
              size="md"
              disabled={isSubmittingFeedback}
              onClick={handleSkipFeedback}
            >
              건너뛰기
            </Button>
          </div>
        </section>
      )}

      {/* 피드백 완료 메시지 */}
      {feedbackDone && (
        <div className="border-t border-[var(--color-border)] py-[var(--space-l)] text-center">
          <p className="mb-[var(--space-s)] text-[var(--font-size-body2)] font-medium text-[var(--color-primary)]">
            💡 내일 추천에 반영할게요!
          </p>
          {totalFeedbackCount !== null && (
            <p className="text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
              지금까지 {totalFeedbackCount}번째 피드백!
            </p>
          )}
        </div>
      )}

      {/* 실행 취소 바 */}
      {showUndo && (
        <div
          role="alert"
          className="fixed bottom-[calc(var(--bottom-tab-height)+8px)] left-1/2 z-50 flex w-[calc(100%-32px)] max-w-[calc(var(--max-content-width)-32px)] -translate-x-1/2 items-center justify-between rounded-[var(--radius-m)] bg-[var(--color-secondary)] px-4 py-3 text-white shadow-[var(--shadow-4)]"
        >
          <span className="text-[var(--font-size-body2)]">
            🔄 실행 취소 ({undoSeconds}초 남음)
          </span>
          <button
            onClick={handleUndo}
            disabled={isDeleting}
            className="text-[var(--font-size-body2)] font-semibold text-[var(--color-primary)] disabled:opacity-50"
          >
            실행 취소
          </button>
        </div>
      )}
    </div>
  )
}

export default function MealRecordPage() {
  return (
    <Suspense fallback={<div className="px-[var(--margin-mobile)] py-[var(--space-xl)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">불러오는 중...</div>}>
      <MealRecordContent />
    </Suspense>
  )
}
