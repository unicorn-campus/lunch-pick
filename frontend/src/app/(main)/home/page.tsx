'use client'

/**
 * 홈 — 오늘의 추천 페이지
 * UFR-REC-010: 오늘의 추천 3개 조회
 * UFR-REC-020: 추천 이유 상세 (바텀시트)
 * UFR-REC-040: 추천 수락
 * UFR-REC-050: 추천 거절 + 대체 추천
 */
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Loading, { CardSkeleton } from '@/components/common/Loading'
import BottomSheet from '@/components/common/BottomSheet'
import Button from '@/components/common/Button'
import {
  useTodayRecommendations,
  useRecommendationReason,
  useAcceptRecommendation,
  useRejectRecommendation,
  useRefreshRecommendations,
} from '@/hooks/useRecommendation'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/hooks/useToast'
import type { RecommendationCard } from '@/types/recommendation'

const DEFAULT_LOCATION = { latitude: 37.5665, longitude: 126.978 }

/** API 미연결 시 표시할 데모 추천 데이터 */
const DEMO_RECOMMENDATIONS: RecommendationCard[] = [
  {
    recommendationId: 'demo-001',
    restaurantId: 'rest-demo-001',
    restaurantName: '광화문 된장마을',
    representativeMenu: '된장찌개 정식',
    reasonSummary: '따뜻한 국물이 생각날 때, 집밥 느낌의 한식 한 끼 어때요?',
    confidenceScore: 92,
    distanceMeters: 350,
    estimatedWalkMinutes: 5,
    category: '한식',
    isFallback: false,
  },
  {
    recommendationId: 'demo-002',
    restaurantId: 'rest-demo-002',
    restaurantName: '스시히로',
    representativeMenu: '런치 스페셜 세트',
    reasonSummary: '신선한 해산물로 기분 전환! 평점 4.7의 인기 맛집이에요.',
    confidenceScore: 87,
    distanceMeters: 520,
    estimatedWalkMinutes: 7,
    category: '일식',
    isFallback: false,
  },
  {
    recommendationId: 'demo-003',
    restaurantId: 'rest-demo-003',
    restaurantName: '반미 사이공',
    representativeMenu: '클래식 반미 + 쌀국수 세트',
    reasonSummary: '이번 주 아직 동남아 음식을 안 드셨네요. 가볍게 한 끼!',
    confidenceScore: 81,
    distanceMeters: 680,
    estimatedWalkMinutes: 9,
    category: '베트남',
    isFallback: false,
  },
]

const REJECT_REASONS: { label: string; value: 'MOOD_NOT_MATCH' | 'TOO_FAR' | 'RECENTLY_VISITED' | 'OTHER' }[] = [
  { label: '오늘 기분 아님', value: 'MOOD_NOT_MATCH' },
  { label: '너무 멀어요', value: 'TOO_FAR' },
  { label: '최근에 갔어요', value: 'RECENTLY_VISITED' },
  { label: '기타', value: 'OTHER' },
]

/** 데모용 추천 이유 데이터 */
const DEMO_REASONS: Record<string, { naturalLanguageReason: string; confidenceScore: number; contextTags: string[] }> = {
  'demo-001': {
    naturalLanguageReason: '최근 3일간 매운 음식을 드셨어요. 오늘은 따뜻하고 담백한 된장찌개로 속을 달래보는 건 어떨까요? 가성비도 좋고, 도보 5분 거리라 빠르게 다녀올 수 있어요.',
    confidenceScore: 92,
    contextTags: ['최근 식사 패턴', '날씨: 쌀쌀함', '가성비 선호'],
  },
  'demo-002': {
    naturalLanguageReason: '이번 주 아직 일식을 안 드셨네요. 평점 4.7의 스시히로에서 신선한 런치 스페셜을 즐겨보세요! 점심 시간대 웨이팅이 적어요.',
    confidenceScore: 87,
    contextTags: ['카테고리 다양성', '높은 평점', '웨이팅 적음'],
  },
  'demo-003': {
    naturalLanguageReason: '동남아 음식을 좋아하시는데 최근 2주간 안 드셨어요. 반미와 쌀국수 세트로 가볍지만 든든한 한 끼 어때요?',
    confidenceScore: 81,
    contextTags: ['선호 카테고리', '오랜만에 방문', '가벼운 식사'],
  },
}

function ReasonSheet({
  recommendationId,
  restaurantName,
  isOpen,
  onClose,
  onAccept,
  isDemo,
}: {
  recommendationId: string
  restaurantName: string
  isOpen: boolean
  onClose: () => void
  onAccept: () => void
  isDemo: boolean
}) {
  const { data: reason, isLoading, isError } = useRecommendationReason(
    isOpen ? recommendationId : '',
  )

  const demoReason = DEMO_REASONS[recommendationId]
  const effectiveReason = reason ?? (isDemo || isError ? demoReason : undefined)

  return (
    <BottomSheet isOpen={isOpen} onClose={onClose} ariaLabel="추천 이유 상세">
      <h3 className="mb-[var(--space-m)] text-[var(--font-size-h3)] font-semibold">
        {restaurantName}을 추천한 이유
      </h3>

      {isLoading && !isError && !isDemo ? (
        <Loading message="이유를 불러오는 중..." />
      ) : effectiveReason ? (
        <>
          <div className="mb-[var(--space-l)] rounded-[var(--radius-s)] bg-[var(--color-background)] p-[var(--space-m)] text-[var(--font-size-body1)]" style={{ lineHeight: 'var(--line-height-body1)' }}>
            &ldquo;{effectiveReason.naturalLanguageReason}&rdquo;
          </div>

          {/* 확신 스코어 */}
          <div className="mb-[var(--space-l)]">
            <p className="mb-[var(--space-s)] text-[var(--font-size-label)] font-medium">확신 스코어</p>
            <div className="flex items-center gap-3">
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-[var(--color-border)]">
                <div
                  className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-500"
                  style={{ width: `${effectiveReason.confidenceScore}%` }}
                />
              </div>
              <span className="text-[var(--font-size-label)] font-semibold text-[var(--color-primary)]">
                {effectiveReason.confidenceScore}%
              </span>
            </div>
          </div>

          {/* 컨텍스트 태그 */}
          {effectiveReason.contextTags.length > 0 && (
            <div className="mb-[var(--space-l)]">
              <p className="mb-[var(--space-s)] text-[var(--font-size-label)] font-medium">반영된 컨텍스트</p>
              <div className="flex flex-wrap gap-2">
                {effectiveReason.contextTags.map((tag) => (
                  <span key={tag} className="rounded-[var(--radius-xs)] bg-blue-50 px-2 py-1 text-[var(--font-size-caption)] text-[var(--color-info)]">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}

          <Button variant="primary" size="full" onClick={onAccept}>
            여기 갈래요 →
          </Button>
        </>
      ) : null}
    </BottomSheet>
  )
}

function RecommendationCardItem({
  rec,
  rank,
  onAccept,
  onReject,
  onShowReason,
}: {
  rec: RecommendationCard
  rank: number
  onAccept: (rec: RecommendationCard) => void
  onReject: (rec: RecommendationCard) => void
  onShowReason: (rec: RecommendationCard) => void
}) {
  return (
    <div
      role="listitem"
      aria-label={`추천 ${rank}: ${rec.restaurantName}`}
      className="mb-[var(--space-m)] rounded-[var(--radius-l)] bg-[var(--color-surface)] p-[var(--space-m)] shadow-[var(--shadow-2)]"
    >
      <div className="mb-1 text-[var(--font-size-caption)] font-semibold text-[var(--color-primary)]">
        {rank}.
      </div>
      <div className="mb-2 flex items-start justify-between">
        <div>
          <span className="text-[var(--font-size-h3)] font-semibold">{rec.restaurantName}</span>
          <span className="ml-1 text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">({rec.category})</span>
        </div>
        <span className="text-[var(--font-size-body2)] font-semibold text-[var(--color-warning)]">
          ⭐ {rec.confidenceScore}%
        </span>
      </div>
      <p className="mb-1 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
        대표: {rec.representativeMenu}
      </p>
      <p className="mb-2 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
        📍 도보 {rec.estimatedWalkMinutes}분 | {rec.distanceMeters}m
      </p>
      <p className="mb-[var(--space-m)] text-[var(--font-size-body2)] text-[var(--color-text-primary)] italic">
        &ldquo;{rec.reasonSummary}&rdquo;
      </p>

      {rec.isFallback && (
        <p className="mb-2 text-[var(--font-size-caption)] text-[var(--color-warning)]">
          ⚠ AI 장애로 기본 추천이 표시되고 있어요
        </p>
      )}

      <div className="flex items-center justify-between">
        <div className="flex gap-3">
          <button
            className="text-[var(--font-size-body2)] text-[var(--color-primary)] underline-offset-2 hover:underline"
            onClick={() => onShowReason(rec)}
            aria-label="추천 이유 상세 보기"
          >
            왜?
          </button>
          <button
            className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)] underline-offset-2 hover:underline"
            onClick={() => onReject(rec)}
            aria-label="거절하기"
          >
            거절
          </button>
        </div>
        <Button variant="primary" size="sm" onClick={() => onAccept(rec)}>
          여기 갈래요 →
        </Button>
      </div>
    </div>
  )
}

export default function HomePage() {
  const router = useRouter()
  const toast = useToast()
  const { nickname } = useAuthStore()

  const [reasonSheet, setReasonSheet] = useState<RecommendationCard | null>(null)
  const [rejectSheet, setRejectSheet] = useState<RecommendationCard | null>(null)
  const [startTime] = useState(() => Date.now())

  const { data, isLoading, isError, refetch } = useTodayRecommendations(DEFAULT_LOCATION)
  const { mutate: acceptRec, isPending: isAccepting } = useAcceptRecommendation()
  const { mutate: rejectRec, isPending: isRejecting } = useRejectRecommendation()
  const { mutate: refreshRecs, isPending: isRefreshing } = useRefreshRecommendations()

  const isDemo = isError && !data

  function handleAccept(rec: RecommendationCard) {
    // 데모 모드: API 없이 바로 길찾기 페이지로 이동
    if (isDemo) {
      toast.success(`${rec.restaurantName}(으)로 가볼게요!`)
      router.push(
        `/navigation?name=${encodeURIComponent(rec.restaurantName)}&address=${encodeURIComponent('서울시 종로구 (데모)')}&restaurantId=${rec.restaurantId}&recId=${rec.recommendationId}&walkMinutes=${rec.estimatedWalkMinutes}&distanceMeters=${rec.distanceMeters}`,
      )
      return
    }

    const reactionTimeMs = Date.now() - startTime
    acceptRec(
      {
        recommendationId: rec.recommendationId,
        data: { acceptedAt: new Date().toISOString(), reactionTimeMs },
      },
      {
        onSuccess: (res) => {
          toast.success(res.message)
          router.push(
            `/navigation?name=${encodeURIComponent(res.restaurantName)}&address=${encodeURIComponent(res.restaurantAddress)}&restaurantId=${res.restaurantId}&recId=${rec.recommendationId}`,
          )
        },
        onError: () => toast.error('수락 처리 중 오류가 발생했어요.'),
      },
    )
  }

  function handleRejectConfirm(
    rec: RecommendationCard,
    reason: 'MOOD_NOT_MATCH' | 'TOO_FAR' | 'RECENTLY_VISITED' | 'OTHER',
  ) {
    setRejectSheet(null)

    if (isDemo) {
      toast.info(`${rec.restaurantName} 거절됨 (사유: ${reason}). 다음에 반영할게요!`)
      return
    }

    rejectRec(
      {
        recommendationId: rec.recommendationId,
        data: { rejectReason: reason },
      },
      {
        onSuccess: (res) => {
          if (res.hasAlternative && res.alternativeRecommendation) {
            toast.info('이런 건 어때요? 대체 추천을 준비했어요')
            refetch()
          } else {
            toast.info(res.noAlternativeMessage ?? '주변에 더 추천할 곳이 없어요. 전체 새로고침을 해보세요.')
          }
        },
        onError: () => toast.error('거절 처리 중 오류가 발생했어요.'),
      },
    )
  }

  function handleRefresh() {
    if (isDemo) {
      toast.success('데모 모드에서는 동일한 추천이 표시됩니다.')
      return
    }

    const rejectedIds = data?.recommendations.map((r) => r.recommendationId) ?? []
    refreshRecs(
      { rejectedIds, ...DEFAULT_LOCATION },
      {
        onSuccess: () => toast.success('추천이 새로고침되었어요!'),
        onError: () => toast.error('새로고침 중 오류가 발생했어요.'),
      },
    )
  }

  return (
    <div className="px-[var(--margin-mobile)] py-[var(--space-m)]">
      {/* 인사 */}
      <div className="mb-[var(--space-s)]">
        <p className="text-[var(--font-size-h2)] font-bold">
          안녕하세요, {nickname ?? '런치픽'}님!
        </p>
        <p className="mt-1 text-[var(--font-size-body1)] text-[var(--color-text-secondary)]">
          오늘 점심 추천이에요 🍽️
        </p>
      </div>

      {/* 콜드스타트 배너 */}
      {data?.isColdStart && (
        <div className="mb-[var(--space-m)] flex items-start gap-2 rounded-[var(--radius-m)] bg-[#FFFBEB] p-[var(--space-m)] text-[var(--font-size-body2)] text-[#92400E]">
          <span aria-hidden="true">💡</span>
          <span>{data.coldStartMessage ?? '아직 취향을 학습 중이에요. 3일만 더 기록하면 추천이 확 달라져요!'}</span>
        </div>
      )}

      {/* 피드백 반영 태그 */}
      {!data?.isColdStart && data && (
        <div className="mb-[var(--space-m)] inline-flex items-center gap-1 rounded-[var(--radius-xs)] bg-blue-50 px-2 py-1 text-[var(--font-size-caption)] font-medium text-[var(--color-info)]">
          ✓ 어제 피드백 반영
        </div>
      )}

      {/* 추천 카드 목록 */}
      {isLoading && (
        <div role="list" aria-label="오늘의 추천 로딩 중">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>
      )}

      {isError && !data && (
        <div className="mb-[var(--space-m)] rounded-[var(--radius-m)] bg-[#FFFBEB] p-[var(--space-s)] text-center text-[var(--font-size-caption)] text-[#92400E]">
          🎬 데모 모드 — 백엔드 미연결 시 샘플 데이터를 표시합니다
        </div>
      )}

      {(data || isError) && (
        <div role="list" aria-label="오늘의 추천">
          {(data?.recommendations ?? DEMO_RECOMMENDATIONS).map((rec, idx) => (
            <RecommendationCardItem
              key={rec.recommendationId}
              rec={rec}
              rank={idx + 1}
              onAccept={handleAccept}
              onReject={(r) => setRejectSheet(r)}
              onShowReason={(r) => setReasonSheet(r)}
            />
          ))}
        </div>
      )}

      {/* 새로고침 버튼 */}
      <button
        onClick={handleRefresh}
        disabled={isRefreshing}
        aria-label="전체 새로고침"
        className="mb-[var(--space-l)] flex w-full items-center justify-center gap-2 rounded-[var(--radius-m)] border border-dashed border-[var(--color-border)] p-[var(--space-m)] text-[var(--font-size-body2)] text-[var(--color-text-secondary)] transition-colors hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] disabled:opacity-50"
      >
        {isRefreshing ? (
          <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
        ) : (
          '🔄'
        )}{' '}
        전체 새로고침
      </button>

      {/* 추천 이유 바텀시트 */}
      {reasonSheet && (
        <ReasonSheet
          recommendationId={reasonSheet.recommendationId}
          restaurantName={reasonSheet.restaurantName}
          isOpen={!!reasonSheet}
          onClose={() => setReasonSheet(null)}
          onAccept={() => {
            setReasonSheet(null)
            handleAccept(reasonSheet)
          }}
          isDemo={isDemo}
        />
      )}

      {/* 거절 사유 바텀시트 */}
      <BottomSheet
        isOpen={!!rejectSheet}
        onClose={() => setRejectSheet(null)}
        title="거절 사유를 알려주세요"
        ariaLabel="거절 사유 선택"
      >
        <div className="flex flex-col gap-[var(--space-s)]">
          {REJECT_REASONS.map((reason) => (
            <button
              key={reason.value}
              onClick={() => rejectSheet && handleRejectConfirm(rejectSheet, reason.value)}
              disabled={isRejecting}
              className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] bg-[var(--color-background)] p-[var(--space-m)] text-left text-[var(--font-size-body1)] transition-colors hover:border-[var(--color-primary)] hover:bg-orange-50 disabled:opacity-50"
            >
              {reason.label}
            </button>
          ))}
        </div>
      </BottomSheet>
    </div>
  )
}
