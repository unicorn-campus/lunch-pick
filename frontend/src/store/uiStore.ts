/**
 * UI 상태 스토어 (Zustand)
 * 전역 UI 상태(로딩, 토스트, 바텀시트)를 관리한다.
 */
import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastAction {
  label: string
  onClick: () => void
}

export interface Toast {
  id: string
  type: ToastType
  message: string
  duration?: number
  action?: ToastAction
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
    // 액션 버튼이 있으면 자동 제거하지 않음 (사용자가 직접 닫거나 액션 클릭)
    const autoDismiss = toast.duration ?? (toast.action ? 0 : 3000)
    if (autoDismiss > 0) {
      setTimeout(() => {
        set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
      }, autoDismiss)
    }
  },
  removeToast: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),

  activeBottomSheet: null,
  openBottomSheet: (sheetId) => set({ activeBottomSheet: sheetId }),
  closeBottomSheet: () => set({ activeBottomSheet: null }),
}))
