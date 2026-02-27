/**
 * UI 상태 스토어 (Zustand)
 * 전역 UI 상태(로딩, 토스트, 바텀시트)를 관리한다.
 */
import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface Toast {
  id: string
  type: ToastType
  message: string
  duration?: number
}

interface UiState {
  /** 전역 로딩 상태 */
  isLoading: boolean
  setLoading: (loading: boolean) => void

  /** 토스트 알림 */
  toasts: Toast[]
  addToast: (toast: Omit<Toast, 'id'>) => void
  removeToast: (id: string) => void

  /** 활성 바텀시트 */
  activeBottomSheet: string | null
  openBottomSheet: (sheetId: string) => void
  closeBottomSheet: () => void
}

export const useUiStore = create<UiState>((set) => ({
  isLoading: false,
  setLoading: (isLoading) => set({ isLoading }),

  toasts: [],
  addToast: (toast) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2)}`
    set((state) => ({ toasts: [...state.toasts, { ...toast, id }] }))
    // 자동 제거 (기본 3초)
    setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
    }, toast.duration ?? 3000)
  },
  removeToast: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),

  activeBottomSheet: null,
  openBottomSheet: (sheetId) => set({ activeBottomSheet: sheetId }),
  closeBottomSheet: () => set({ activeBottomSheet: null }),
}))
