'use client'

import { useEffect } from 'react'
import { useUiStore } from '@/store'
import type { Toast as ToastType } from '@/store'

function ToastItem({ toast }: { toast: ToastType }) {
  const removeToast = useUiStore((s) => s.removeToast)

  const bgClass = {
    success: 'bg-[var(--color-success)]',
    error: 'bg-[var(--color-error)]',
    info: 'bg-[var(--color-info)]',
    warning: 'bg-[var(--color-warning)]',
  }[toast.type]

  const icon = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠',
  }[toast.type]

  return (
    <div
      role="alert"
      aria-live="polite"
      className={`flex items-center gap-2 rounded-[var(--radius-m)] px-4 py-3 text-white shadow-[var(--shadow-3)] text-[var(--font-size-body2)] animate-in slide-in-from-bottom-2 ${bgClass}`}
    >
      <span aria-hidden="true" className="font-bold">{icon}</span>
      <span>{toast.message}</span>
      {toast.action && (
        <button
          onClick={() => {
            toast.action!.onClick()
            removeToast(toast.id)
          }}
          className="ml-auto font-semibold underline hover:opacity-80"
        >
          {toast.action.label}
        </button>
      )}
      <button
        onClick={() => removeToast(toast.id)}
        className={`${toast.action ? '' : 'ml-auto '}opacity-70 hover:opacity-100`}
        aria-label="알림 닫기"
      >
        ✕
      </button>
    </div>
  )
}

export default function ToastContainer() {
  const toasts = useUiStore((s) => s.toasts)

  if (toasts.length === 0) return null

  return (
    <div
      className="fixed bottom-[calc(var(--bottom-tab-height)+16px)] left-1/2 z-50 flex w-full max-w-[var(--max-content-width)] -translate-x-1/2 flex-col gap-2 px-4"
      aria-label="알림 영역"
    >
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} />
      ))}
    </div>
  )
}
