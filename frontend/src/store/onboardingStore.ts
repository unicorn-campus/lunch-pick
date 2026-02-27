/**
 * 온보딩 상태 스토어 (Zustand)
 * 취향 퀴즈 중간 저장 및 진행 상태를 관리한다.
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { CardSwipeResult } from '@/types/member'

interface OnboardingState {
  /** 완료한 스와이프 결과 목록 */
  swipeResults: CardSwipeResult[]
  /** 현재 스텝 인덱스 */
  currentStep: number
  /** 최소 완료 기준 (7장) */
  minRequired: number

  addSwipeResult: (item: CardSwipeResult) => void
  setCurrentStep: (step: number) => void
  resetOnboarding: () => void
  isMinCompleted: () => boolean
}

export const useOnboardingStore = create<OnboardingState>()(
  persist(
    (set, get) => ({
      swipeResults: [],
      currentStep: 0,
      minRequired: 7,

      addSwipeResult: (item) =>
        set((state) => ({
          swipeResults: [...state.swipeResults, item],
          currentStep: state.currentStep + 1,
        })),

      setCurrentStep: (step) => set({ currentStep: step }),

      resetOnboarding: () =>
        set({ swipeResults: [], currentStep: 0 }),

      isMinCompleted: () => get().swipeResults.length >= get().minRequired,
    }),
    {
      name: 'lunchpick-onboarding',
    },
  ),
)
