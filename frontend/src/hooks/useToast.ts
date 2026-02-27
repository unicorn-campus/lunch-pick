/**
 * 토스트 알림 훅
 * useUiStore의 addToast를 편리하게 사용하기 위한 래퍼
 */
import { useUiStore } from '@/store'

export function useToast() {
  const addToast = useUiStore((state) => state.addToast)

  return {
    success: (message: string, duration?: number) =>
      addToast({ type: 'success', message, duration }),
    error: (message: string, duration?: number) =>
      addToast({ type: 'error', message, duration }),
    info: (message: string, duration?: number) =>
      addToast({ type: 'info', message, duration }),
    warning: (message: string, duration?: number) =>
      addToast({ type: 'warning', message, duration }),
  }
}
