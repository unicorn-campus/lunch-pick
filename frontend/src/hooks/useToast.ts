/**
 * 토스트 알림 훅
 * useUiStore의 addToast를 편리하게 사용하기 위한 래퍼
 */
import { useUiStore } from '@/store'
import type { ToastAction } from '@/store'

interface ToastOptions {
  duration?: number
  action?: ToastAction
}

export function useToast() {
  const addToast = useUiStore((state) => state.addToast)

  return {
    success: (message: string, options?: number | ToastOptions) => {
      const opts = typeof options === 'number' ? { duration: options } : options
      addToast({ type: 'success', message, ...opts })
    },
    error: (message: string, options?: number | ToastOptions) => {
      const opts = typeof options === 'number' ? { duration: options } : options
      addToast({ type: 'error', message, ...opts })
    },
    info: (message: string, options?: number | ToastOptions) => {
      const opts = typeof options === 'number' ? { duration: options } : options
      addToast({ type: 'info', message, ...opts })
    },
    warning: (message: string, options?: number | ToastOptions) => {
      const opts = typeof options === 'number' ? { duration: options } : options
      addToast({ type: 'warning', message, ...opts })
    },
  }
}
