'use client'

/**
 * 인사이트 페이지
 * UFR-REC-110: 식사 이력 캘린더 (GET /api/v1/history/timeline)
 * UFR-REC-120: 취향 인사이트 리포트 (GET /api/v1/insights)
 */
import { useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Loading, { CardSkeleton } from '@/components/common/Loading'
import Button from '@/components/common/Button'
import { useHistoryTimeline, useInsights } from '@/hooks/useRecommendation'
import type { MealHistoryItem, InsightsResponse, MealHistoryResponse } from '@/types/recommendation'

type TabType = 'history' | 'insight'

const DAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

const CATEGORY_COLORS: Record<string, string> = {
  한식: '#EF4444',
  양식: '#3B82F6',
  중식: '#F59E0B',
  일식: '#8B5CF6',
  분식: '#EC4899',
  '샐러드/건강식': '#10B981',
  패스트푸드: '#F97316',
  아시안: '#06B6D4',
}

function getCategoryColor(category: string, fallbackColor?: string): string {
  return CATEGORY_COLORS[category] ?? fallbackColor ?? '#9CA3AF'
}

function CalendarView({ meals }: { meals: MealHistoryItem[] }) {
  const now = new Date()
  const [viewYear, setViewYear] = useState(now.getFullYear())
  const [viewMonth, setViewMonth] = useState(now.getMonth()) // 0-indexed

  const firstDay = new Date(viewYear, viewMonth, 1).getDay() // 0=Sun
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()

  // 날짜 → 식사 기록 맵
  const mealMap: Record<number, MealHistoryItem> = {}
  meals.forEach((m) => {
    const d = new Date(m.date)
    if (d.getFullYear() === viewYear && d.getMonth() === viewMonth) {
      mealMap[d.getDate()] = m
    }
  })

  function prevMonth() {
    if (viewMonth === 0) {
      setViewMonth(11)
      setViewYear((y) => y - 1)
    } else {
      setViewMonth((m) => m - 1)
    }
  }

  function nextMonth() {
    const today = new Date()
    if (viewYear > today.getFullYear() || (viewYear === today.getFullYear() && viewMonth >= today.getMonth())) return
    if (viewMonth === 11) {
      setViewMonth(0)
      setViewYear((y) => y + 1)
    } else {
      setViewMonth((m) => m + 1)
    }
  }

  const isCurrentMonth =
    viewYear === now.getFullYear() && viewMonth === now.getMonth()

  return (
    <div>
      {/* 월 네비게이션 */}
      <div className="mb-[var(--space-m)] flex items-center justify-between">
        <button
          onClick={prevMonth}
          aria-label="이전 달"
          className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-[var(--color-background)] text-[var(--color-text-secondary)]"
        >
          ←
        </button>
        <span className="text-[var(--font-size-h3)] font-semibold">
          {viewYear}년 {viewMonth + 1}월
        </span>
        <button
          onClick={nextMonth}
          aria-label="다음 달"
          disabled={isCurrentMonth}
          className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-[var(--color-background)] text-[var(--color-text-secondary)] disabled:opacity-30"
        >
          →
        </button>
      </div>

      {/* 캘린더 그리드 */}
      <div className="mb-[var(--space-l)] grid grid-cols-7 gap-[var(--space-xs)] text-center">
        {/* 요일 헤더 */}
        {DAY_LABELS.map((d) => (
          <div
            key={d}
            className="py-[var(--space-s)] text-[var(--font-size-caption)] text-[var(--color-text-secondary)]"
          >
            {d}
          </div>
        ))}

        {/* 빈 셀 (월 시작 요일 앞) */}
        {Array.from({ length: firstDay }).map((_, i) => (
          <div key={`empty-${i}`} />
        ))}

        {/* 날짜 셀 */}
        {Array.from({ length: daysInMonth }, (_, i) => i + 1).map((day) => {
          const meal = mealMap[day]
          const isToday =
            isCurrentMonth && day === now.getDate()
          return (
            <div
              key={day}
              className={`flex aspect-square flex-col items-center justify-center gap-[2px] rounded-[var(--radius-s)] text-[var(--font-size-body2)] ${
                isToday
                  ? 'bg-[var(--color-primary)] text-white'
                  : 'hover:bg-[var(--color-background)]'
              }`}
            >
              <span>{day}</span>
              {meal && (
                <span
                  className="h-[6px] w-[6px] rounded-full"
                  style={{
                    background: getCategoryColor(meal.category, meal.categoryColor),
                  }}
                />
              )}
            </div>
          )
        })}
      </div>

      {/* 범례 */}
      <div className="mb-[var(--space-l)] flex flex-wrap justify-center gap-[var(--space-m)]">
        {Object.entries(CATEGORY_COLORS)
          .slice(0, 4)
          .map(([cat, color]) => (
            <div key={cat} className="flex items-center gap-[var(--space-xs)]">
              <span
                className="h-2 w-2 rounded-full"
                style={{ background: color }}
              />
              <span className="text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
                {cat}
              </span>
            </div>
          ))}
      </div>
    </div>
  )
}

/** 데모 이력 데이터 */
const DEMO_HISTORY: MealHistoryResponse = {
  meals: [
    { mealId: 'dm-1', date: new Date().toISOString(), restaurantName: '광화문 된장마을', menuName: '된장찌개 정식', category: '한식', categoryColor: '#EF4444', satisfaction: 'GOOD', recordedAt: new Date().toISOString() },
    { mealId: 'dm-2', date: new Date(Date.now() - 86400000).toISOString(), restaurantName: '스시히로', menuName: '런치 스페셜 세트', category: '일식', categoryColor: '#8B5CF6', satisfaction: 'GOOD', recordedAt: new Date(Date.now() - 86400000).toISOString() },
    { mealId: 'dm-3', date: new Date(Date.now() - 86400000 * 2).toISOString(), restaurantName: '반미 사이공', menuName: '클래식 반미', category: '아시안', categoryColor: '#06B6D4', satisfaction: 'BAD', recordedAt: new Date(Date.now() - 86400000 * 2).toISOString() },
  ],
  totalCount: 3,
  message: null,
}

/** 데모 인사이트 데이터 */
const DEMO_INSIGHTS: InsightsResponse = {
  hasEnoughData: false,
  currentRecordCount: 3,
  requiredRecordCount: 10,
  message: null,
  topCategories: [
    { category: '한식', count: 5, percentage: 40, color: '#EF4444' },
    { category: '일식', count: 3, percentage: 25, color: '#8B5CF6' },
    { category: '아시안', count: 2, percentage: 15, color: '#06B6D4' },
    { category: '양식', count: 2, percentage: 15, color: '#3B82F6' },
  ],
  weeklyPattern: [
    { dayOfWeek: 'MON', topCategory: '한식', averageSatisfaction: 0.8 },
    { dayOfWeek: 'WED', topCategory: '일식', averageSatisfaction: 0.9 },
    { dayOfWeek: 'FRI', topCategory: '아시안', averageSatisfaction: 0.7 },
  ],
  satisfactionTrend: [
    { week: '1주차', satisfactionRate: 70 },
    { week: '2주차', satisfactionRate: 80 },
    { week: '3주차', satisfactionRate: 85 },
  ],
  weeklySummary: '이번 주는 한식과 일식을 골고루 드셨어요. 만족도가 점점 올라가고 있네요!',
  milestone: { achieved: true, count: 3, message: '3끼 기록 달성!', accuracyImprovement: 12 },
}

function InsightsContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const tabParam = searchParams.get('tab')
  const [activeTab, setActiveTab] = useState<TabType>(tabParam === 'insight' ? 'insight' : 'history')

  const { data: historyData, isLoading: isHistoryLoading, isError: isHistoryError } = useHistoryTimeline()
  const { data: insightsData, isLoading: isInsightsLoading, isError: isInsightsError } = useInsights()

  // 데모 모드: API 에러 시 데모 데이터 사용
  const effectiveHistory = historyData ?? (isHistoryError ? DEMO_HISTORY : undefined)
  const effectiveInsights = insightsData ?? (isInsightsError ? DEMO_INSIGHTS : undefined)
  const isDemo = isHistoryError || isInsightsError

  return (
    <div className="px-[var(--margin-mobile)]">
      {/* 탭 */}
      <div
        role="tablist"
        className="mb-[var(--space-l)] mt-[var(--space-m)] flex border-b-2 border-[#E5E7EB]"
      >
        <button
          id="tabHistory"
          role="tab"
          aria-selected={activeTab === 'history'}
          aria-controls="panelHistory"
          onClick={() => setActiveTab('history')}
          className={`relative flex-1 py-[var(--space-m)] text-[var(--font-size-label)] font-medium transition-colors duration-[var(--duration-fast)] ${
            activeTab === 'history'
              ? 'text-[var(--color-primary)]'
              : 'text-[var(--color-text-secondary)]'
          }`}
        >
          이력
          {activeTab === 'history' && (
            <span className="absolute bottom-[-2px] left-0 right-0 h-[2px] bg-[var(--color-primary)]" />
          )}
        </button>
        <button
          id="tabInsight"
          role="tab"
          aria-selected={activeTab === 'insight'}
          aria-controls="panelInsight"
          onClick={() => setActiveTab('insight')}
          className={`relative flex-1 py-[var(--space-m)] text-[var(--font-size-label)] font-medium transition-colors duration-[var(--duration-fast)] ${
            activeTab === 'insight'
              ? 'text-[var(--color-primary)]'
              : 'text-[var(--color-text-secondary)]'
          }`}
        >
          인사이트
          {activeTab === 'insight' && (
            <span className="absolute bottom-[-2px] left-0 right-0 h-[2px] bg-[var(--color-primary)]" />
          )}
        </button>
      </div>

      {/* 데모 모드 안내 */}
      {isDemo && (
        <div className="mb-[var(--space-m)] rounded-[var(--radius-m)] bg-[#FFFBEB] p-[var(--space-s)] text-center text-[var(--font-size-caption)] text-[#92400E]">
          🎬 데모 모드 — 샘플 데이터를 표시합니다
        </div>
      )}

      {/* 이력 탭 */}
      <div
        id="panelHistory"
        role="tabpanel"
        aria-labelledby="tabHistory"
        hidden={activeTab !== 'history'}
      >
        {isHistoryLoading ? (
          <>
            <CardSkeleton />
            <CardSkeleton />
          </>
        ) : effectiveHistory ? (
          <>
            <CalendarView meals={effectiveHistory.meals} />

            {/* 프리미엄 안내 (FREE 플랜에서 30일 이전 데이터 제한) */}
            {effectiveHistory.message && (
              <div className="flex items-center gap-[var(--space-s)] rounded-[var(--radius-m)] bg-[#FFF5F0] p-[var(--space-m)]">
                <span aria-hidden="true">💎</span>
                <div className="flex-1 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                  30일 전 기록은 프리미엄에서 확인할 수 있어요
                </div>
                <button
                  onClick={() => router.push('/subscription')}
                  className="whitespace-nowrap text-[var(--font-size-body2)] text-[var(--color-primary)]"
                >
                  체험하기
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="py-[var(--space-xl)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            아직 기록된 식사가 없어요
          </div>
        )}
      </div>

      {/* 인사이트 탭 */}
      <div
        id="panelInsight"
        role="tabpanel"
        aria-labelledby="tabInsight"
        hidden={activeTab !== 'insight'}
      >
        {isInsightsLoading ? (
          <>
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
          </>
        ) : effectiveInsights ? (
          <>
            {/* 마일스톤 배너 */}
            {effectiveInsights.milestone?.achieved && (
              <div className="mb-[var(--space-m)] rounded-[var(--radius-m)] bg-[#FEF3C7] p-[var(--space-m)] text-[var(--font-size-body2)] text-[#92400E]">
                🎉 {effectiveInsights.milestone.count}끼 기록 달성! 추천 정확도가{' '}
                <strong>{effectiveInsights.milestone.accuracyImprovement}%</strong> 올랐어요
              </div>
            )}

            {/* 이번 주 패턴 */}
            {effectiveInsights.weeklySummary && (
              <div className="mb-[var(--space-m)] rounded-[var(--radius-l)] bg-[var(--color-surface)] p-[var(--space-m)] shadow-[var(--shadow-2)]">
                <h3 className="mb-[var(--space-m)] text-[var(--font-size-label)] font-medium">
                  이번 주 당신의 점심 패턴
                </h3>
                <p className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                  &ldquo;{effectiveInsights.weeklySummary}&rdquo;
                </p>
              </div>
            )}

            {/* 선호 카테고리 Top 5 */}
            {effectiveInsights.topCategories.length > 0 && (
              <div className="mb-[var(--space-m)] rounded-[var(--radius-l)] bg-[var(--color-surface)] p-[var(--space-m)] shadow-[var(--shadow-2)]">
                <h3 className="mb-[var(--space-m)] text-[var(--font-size-label)] font-medium">
                  선호 카테고리 Top {Math.min(effectiveInsights.topCategories.length, 5)}
                </h3>
                <div className="space-y-[var(--space-s)]">
                  {effectiveInsights.topCategories.slice(0, 5).map((cat, i) => (
                    <div key={cat.category} className="flex items-center gap-[var(--space-s)]">
                      <span className="w-5 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                        {i + 1}.
                      </span>
                      <span className="w-10 text-[var(--font-size-body2)] font-medium">
                        {cat.category}
                      </span>
                      <div className="h-4 flex-1 overflow-hidden rounded-full bg-[#E5E7EB]">
                        <div
                          className="h-full rounded-full transition-all duration-500"
                          style={{
                            width: `${cat.percentage}%`,
                            background: getCategoryColor(cat.category, cat.color),
                          }}
                        />
                      </div>
                      <span className="w-9 text-right text-[var(--font-size-body2)] font-bold">
                        {cat.percentage}%
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 만족도 요약 */}
            {effectiveInsights.satisfactionTrend.length > 0 && (
              <div className="mb-[var(--space-m)] rounded-[var(--radius-l)] bg-[var(--color-surface)] p-[var(--space-m)] shadow-[var(--shadow-2)]">
                <h3 className="mb-[var(--space-m)] text-[var(--font-size-label)] font-medium">
                  만족도 변화 (최근 30일)
                </h3>
                <div className="flex h-[120px] items-center justify-center rounded-[var(--radius-s)] bg-[var(--color-background)]">
                  <div className="text-center">
                    <div className="text-[36px]">😊</div>
                    <div className="mt-1 text-[var(--font-size-h2)] font-bold">
                      {effectiveInsights.satisfactionTrend[effectiveInsights.satisfactionTrend.length - 1]
                        ?.satisfactionRate ?? 0}
                      %
                    </div>
                  </div>
                </div>
                <div className="mt-[var(--space-s)] flex justify-between text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
                  <span>
                    추세:{' '}
                    {effectiveInsights.satisfactionTrend.length >= 2 &&
                    effectiveInsights.satisfactionTrend[effectiveInsights.satisfactionTrend.length - 1]
                      .satisfactionRate >=
                      effectiveInsights.satisfactionTrend[effectiveInsights.satisfactionTrend.length - 2]
                        .satisfactionRate ? (
                      <span className="text-[var(--color-success)]">상승 ↑</span>
                    ) : (
                      <span className="text-[var(--color-error)]">하락 ↓</span>
                    )}
                  </span>
                  <span>총 {effectiveInsights.currentRecordCount}끼 기록</span>
                </div>
              </div>
            )}

            {/* 데이터 부족 배너 */}
            {!effectiveInsights.hasEnoughData && (
              <div className="rounded-[var(--radius-m)] bg-blue-50 p-[var(--space-m)] text-[var(--font-size-body2)] text-[var(--color-info)]">
                📊 {effectiveInsights.requiredRecordCount}끼 이상 기록하면 취향 인사이트가 열려요! (현재{' '}
                {effectiveInsights.currentRecordCount}끼)
              </div>
            )}
          </>
        ) : (
          <div className="py-[var(--space-xl)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            인사이트 데이터를 불러올 수 없어요
          </div>
        )}

        {/* 구독 업그레이드 유도 */}
        <div className="mt-[var(--space-l)] rounded-[var(--radius-m)] border border-dashed border-[var(--color-border)] p-[var(--space-m)] text-center">
          <p className="mb-[var(--space-s)] text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            💎 더 자세한 인사이트는 프리미엄에서
          </p>
          <Button variant="secondary" size="sm" onClick={() => router.push('/subscription')}>
            프리미엄 보기
          </Button>
        </div>
      </div>
    </div>
  )
}

export default function InsightsPage() {
  return (
    <Suspense fallback={<div className="px-[var(--margin-mobile)] py-[var(--space-xl)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">불러오는 중...</div>}>
      <InsightsContent />
    </Suspense>
  )
}
