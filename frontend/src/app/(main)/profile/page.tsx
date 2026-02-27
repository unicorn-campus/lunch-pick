'use client'

/**
 * 프로필 / 설정 페이지
 * UFR-MBR-050: 프로필 조회·수정 (GET/PUT /api/v1/members/profile)
 * UFR-MBR-060: 알림 설정 토글
 * UFR-MBR-070: 구독 상태 표시 → 구독 관리 페이지 이동
 */
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Loading from '@/components/common/Loading'
import Modal from '@/components/common/Modal'
import { useProfile, useUpdateProfile } from '@/hooks/useMember'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/hooks/useToast'
import type { NotificationSettings, MemberProfile } from '@/types/member'

/** 데모 프로필 데이터 */
const DEMO_PROFILE: MemberProfile = {
  memberId: 'demo-member',
  nickname: '런치픽 유저',
  email: 'demo@lunchpick.kr',
  dietType: '일반',
  allergens: [],
  locationEnabled: false,
  notificationSettings: { recommendationAlert: true, feedbackReminder: true },
  subscription: { plan: 'FREE', historyLimitDays: null, expiresAt: null },
  onboardingCompleted: true,
  createdAt: new Date().toISOString(),
}

export default function ProfilePage() {
  const router = useRouter()
  const toast = useToast()
  const { clearAuth } = useAuthStore()

  const { data: apiProfile, isLoading, isError } = useProfile()
  const { mutate: updateProfile, isPending: isUpdating } = useUpdateProfile()

  const [demoProfile, setDemoProfile] = useState<MemberProfile>(DEMO_PROFILE)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [nicknameInput, setNicknameInput] = useState('')
  const [logoutModalOpen, setLogoutModalOpen] = useState(false)

  const isDemo = isError && !apiProfile
  const profile = apiProfile ?? (isError ? demoProfile : undefined)

  function openEditNickname() {
    setNicknameInput(profile?.nickname ?? '')
    setEditModalOpen(true)
  }

  function handleSaveNickname() {
    const trimmed = nicknameInput.trim()
    if (trimmed.length < 2 || trimmed.length > 20) {
      toast.error('닉네임은 2~20자여야 해요')
      return
    }

    if (isDemo) {
      setDemoProfile((prev) => ({ ...prev, nickname: trimmed }))
      toast.success('닉네임이 변경되었어요')
      setEditModalOpen(false)
      return
    }

    updateProfile(
      { nickname: trimmed },
      {
        onSuccess: () => {
          toast.success('설정이 저장되었어요')
          setEditModalOpen(false)
        },
        onError: () => toast.error('저장 중 오류가 발생했어요.'),
      },
    )
  }

  function handleToggleNotification(
    key: keyof NotificationSettings,
    currentValue: boolean,
  ) {
    if (!profile) return

    if (isDemo) {
      setDemoProfile((prev) => ({
        ...prev,
        notificationSettings: { ...prev.notificationSettings, [key]: !currentValue },
      }))
      toast.success('알림 설정이 저장되었어요')
      return
    }

    const newSettings: NotificationSettings = {
      ...profile.notificationSettings,
      [key]: !currentValue,
    }
    updateProfile(
      { notificationSettings: newSettings },
      {
        onSuccess: () => toast.success('알림 설정이 저장되었어요'),
        onError: () => toast.error('설정 저장 중 오류가 발생했어요.'),
      },
    )
  }

  function handleLogout() {
    clearAuth()
    router.replace('/login')
  }

  if (isLoading && !isError) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Loading message="프로필을 불러오는 중..." />
      </div>
    )
  }

  const planLabel =
    profile?.subscription.plan === 'PREMIUM' ? '💎 프리미엄' : '무료 플랜'
  const allergyLabel =
    profile?.allergens && profile.allergens.length > 0
      ? profile.allergens.slice(0, 2).join(', ') +
        (profile.allergens.length > 2 ? ` 외 ${profile.allergens.length - 2}개` : '')
      : '미설정'

  return (
    <div className="px-[var(--margin-mobile)] pb-[var(--space-xl)]">
      {/* 프로필 헤더 */}
      <div className="py-[var(--space-xl)] text-center">
        <img
          src="/images/logo.png"
          alt=""
          aria-hidden="true"
          className="mx-auto mb-[var(--space-m)] h-20 w-20 rounded-full object-cover"
        />
        <h1 className="text-[var(--font-size-h2)] font-bold">
          {profile?.nickname ?? ''}님
        </h1>
        <div className="mt-[var(--space-s)] inline-flex items-center gap-[var(--space-xs)] rounded-[var(--radius-xs)] bg-[var(--color-background)] px-[var(--space-s)] py-[var(--space-xs)] text-[var(--font-size-caption)] text-[var(--color-text-secondary)]">
          {planLabel}
        </div>
      </div>

      {/* 계정 섹션 */}
      <section className="mb-[var(--space-l)]">
        <div className="mb-[var(--space-s)] px-[var(--space-xs)] text-[var(--font-size-label)] font-medium uppercase text-[var(--color-text-secondary)]">
          계정
        </div>
        <div className="overflow-hidden rounded-[var(--radius-m)] bg-[var(--color-surface)] shadow-[var(--shadow-1)]">
          {/* 닉네임 */}
          <button
            onClick={openEditNickname}
            className="flex min-h-[52px] w-full items-center justify-between border-b border-[#F3F4F6] px-[var(--space-m)] transition-colors hover:bg-[var(--color-background)]"
          >
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">✏️</span>
              <span className="text-[var(--font-size-body1)]">닉네임</span>
            </div>
            <div className="flex items-center gap-[var(--space-s)]">
              <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                {profile?.nickname}
              </span>
              <span className="text-[var(--color-text-disabled)]">→</span>
            </div>
          </button>
          {/* 이메일 */}
          <div className="flex min-h-[52px] items-center justify-between px-[var(--space-m)]">
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">📧</span>
              <span className="text-[var(--font-size-body1)]">이메일</span>
            </div>
            <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
              {profile?.email}
            </span>
          </div>
        </div>
      </section>

      {/* 식이제한 섹션 */}
      <section className="mb-[var(--space-l)]">
        <div className="mb-[var(--space-s)] px-[var(--space-xs)] text-[var(--font-size-label)] font-medium uppercase text-[var(--color-text-secondary)]">
          식이제한
        </div>
        <div className="overflow-hidden rounded-[var(--radius-m)] bg-[var(--color-surface)] shadow-[var(--shadow-1)]">
          {/* 알레르기 설정 */}
          <button
            onClick={() => router.push('/onboarding/dietary')}
            className="flex min-h-[52px] w-full items-center justify-between border-b border-[#F3F4F6] px-[var(--space-m)] transition-colors hover:bg-[var(--color-background)]"
          >
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">⚠️</span>
              <span className="text-[var(--font-size-body1)]">알레르기 설정</span>
            </div>
            <div className="flex items-center gap-[var(--space-s)]">
              <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                {allergyLabel}
              </span>
              <span className="text-[var(--color-text-disabled)]">→</span>
            </div>
          </button>
          {/* 식이 유형 */}
          <button
            onClick={() => router.push('/onboarding/dietary')}
            className="flex min-h-[52px] w-full items-center justify-between px-[var(--space-m)] transition-colors hover:bg-[var(--color-background)]"
          >
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">🥗</span>
              <span className="text-[var(--font-size-body1)]">식이 유형</span>
            </div>
            <div className="flex items-center gap-[var(--space-s)]">
              <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                {profile?.dietType ?? '일반'}
              </span>
              <span className="text-[var(--color-text-disabled)]">→</span>
            </div>
          </button>
        </div>
      </section>

      {/* 알림 섹션 */}
      <section className="mb-[var(--space-l)]">
        <div className="mb-[var(--space-s)] px-[var(--space-xs)] text-[var(--font-size-label)] font-medium uppercase text-[var(--color-text-secondary)]">
          알림
        </div>
        <div className="overflow-hidden rounded-[var(--radius-m)] bg-[var(--color-surface)] shadow-[var(--shadow-1)]">
          {/* 추천 알림 */}
          <div className="flex min-h-[52px] items-center justify-between border-b border-[#F3F4F6] px-[var(--space-m)]">
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">🔔</span>
              <span className="text-[var(--font-size-body1)]">추천 알림</span>
            </div>
            <label className="relative inline-block h-7 w-12 cursor-pointer">
              <input
                type="checkbox"
                className="sr-only"
                aria-label="추천 알림 켜기/끄기"
                checked={profile?.notificationSettings?.recommendationAlert ?? true}
                onChange={() =>
                  handleToggleNotification(
                    'recommendationAlert',
                    profile?.notificationSettings?.recommendationAlert ?? true,
                  )
                }
                disabled={isUpdating}
              />
              <span
                className={`absolute inset-0 rounded-[14px] transition-colors duration-[var(--duration-fast)] ${
                  profile?.notificationSettings?.recommendationAlert ?? true
                    ? 'bg-[var(--color-primary)]'
                    : 'bg-[#D1D5DB]'
                }`}
              />
              <span
                className={`absolute bottom-[3px] left-[3px] h-[22px] w-[22px] rounded-full bg-white shadow transition-transform duration-[var(--duration-fast)] ${
                  profile?.notificationSettings?.recommendationAlert ?? true
                    ? 'translate-x-[20px]'
                    : ''
                }`}
              />
            </label>
          </div>
          {/* 피드백 리마인더 */}
          <div className="flex min-h-[52px] items-center justify-between px-[var(--space-m)]">
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">💬</span>
              <span className="text-[var(--font-size-body1)]">피드백 리마인더</span>
            </div>
            <label className="relative inline-block h-7 w-12 cursor-pointer">
              <input
                type="checkbox"
                className="sr-only"
                aria-label="피드백 리마인더 켜기/끄기"
                checked={profile?.notificationSettings?.feedbackReminder ?? true}
                onChange={() =>
                  handleToggleNotification(
                    'feedbackReminder',
                    profile?.notificationSettings?.feedbackReminder ?? true,
                  )
                }
                disabled={isUpdating}
              />
              <span
                className={`absolute inset-0 rounded-[14px] transition-colors duration-[var(--duration-fast)] ${
                  profile?.notificationSettings?.feedbackReminder ?? true
                    ? 'bg-[var(--color-primary)]'
                    : 'bg-[#D1D5DB]'
                }`}
              />
              <span
                className={`absolute bottom-[3px] left-[3px] h-[22px] w-[22px] rounded-full bg-white shadow transition-transform duration-[var(--duration-fast)] ${
                  profile?.notificationSettings?.feedbackReminder ?? true
                    ? 'translate-x-[20px]'
                    : ''
                }`}
              />
            </label>
          </div>
        </div>
      </section>

      {/* 구독 섹션 */}
      <section className="mb-[var(--space-l)]">
        <div className="mb-[var(--space-s)] px-[var(--space-xs)] text-[var(--font-size-label)] font-medium uppercase text-[var(--color-text-secondary)]">
          구독
        </div>
        <div className="overflow-hidden rounded-[var(--radius-m)] bg-[var(--color-surface)] shadow-[var(--shadow-1)]">
          <button
            onClick={() => router.push('/subscription')}
            className="flex min-h-[52px] w-full items-center justify-between px-[var(--space-m)] transition-colors hover:bg-[var(--color-background)]"
          >
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">💎</span>
              <span className="text-[var(--font-size-body1)]">구독 관리</span>
            </div>
            <div className="flex items-center gap-[var(--space-s)]">
              <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
                {profile?.subscription.plan === 'PREMIUM' ? '프리미엄' : '무료'}
              </span>
              <span className="text-[var(--color-text-disabled)]">→</span>
            </div>
          </button>
        </div>
      </section>

      {/* 위치 섹션 */}
      <section className="mb-[var(--space-l)]">
        <div className="mb-[var(--space-s)] px-[var(--space-xs)] text-[var(--font-size-label)] font-medium uppercase text-[var(--color-text-secondary)]">
          위치
        </div>
        <div className="overflow-hidden rounded-[var(--radius-m)] bg-[var(--color-surface)] shadow-[var(--shadow-1)]">
          <div className="flex min-h-[52px] items-center justify-between px-[var(--space-m)]">
            <div className="flex items-center gap-[var(--space-m)]">
              <span className="w-6 text-center text-[20px]">📍</span>
              <span className="text-[var(--font-size-body1)]">위치 정보 제공</span>
            </div>
            <div
              className={`rounded-[var(--radius-xs)] px-2 py-0.5 text-[var(--font-size-caption)] ${
                profile?.locationEnabled
                  ? 'bg-green-100 text-green-700'
                  : 'bg-[var(--color-background)] text-[var(--color-text-secondary)]'
              }`}
            >
              {profile?.locationEnabled ? '동의' : '미동의'}
            </div>
          </div>
        </div>
      </section>

      {/* 로그아웃 */}
      <div className="py-[var(--space-l)] text-center">
        <button
          onClick={() => setLogoutModalOpen(true)}
          className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)] underline-offset-2 hover:underline"
        >
          로그아웃
        </button>
      </div>

      {/* 닉네임 수정 모달 */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="닉네임 수정"
        primaryLabel={isUpdating ? '저장 중...' : '저장'}
        onPrimary={handleSaveNickname}
        secondaryLabel="취소"
        onSecondary={() => setEditModalOpen(false)}
      >
        <div className="mb-[var(--space-m)]">
          <label
            htmlFor="nicknameInput"
            className="mb-[var(--space-s)] block text-[var(--font-size-label)] font-medium"
          >
            닉네임
          </label>
          <input
            id="nicknameInput"
            type="text"
            maxLength={20}
            placeholder="2~20자"
            value={nicknameInput}
            onChange={(e) => setNicknameInput(e.target.value)}
            className="w-full rounded-[var(--radius-s)] border border-[var(--color-border)] p-3 text-[var(--font-size-body1)] focus:border-[var(--color-primary)] focus:outline-none"
          />
        </div>
      </Modal>

      {/* 로그아웃 확인 모달 */}
      <Modal
        isOpen={logoutModalOpen}
        onClose={() => setLogoutModalOpen(false)}
        title="로그아웃"
        primaryLabel="로그아웃"
        onPrimary={handleLogout}
        secondaryLabel="취소"
        onSecondary={() => setLogoutModalOpen(false)}
        primaryVariant="danger"
      >
        <p className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
          로그아웃 하시겠어요?
        </p>
      </Modal>
    </div>
  )
}
