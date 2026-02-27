'use client'

import { ReactNode, useEffect } from 'react'

interface BottomSheetProps {
  isOpen: boolean
  onClose: () => void
  children: ReactNode
  title?: string
  ariaLabel?: string
}

export default function BottomSheet({
  isOpen,
  onClose,
  children,
  title,
  ariaLabel,
}: BottomSheetProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <>
      {/* 오버레이 */}
      <div
        className="fixed inset-0 z-40 bg-[var(--color-overlay)]"
        onClick={onClose}
        aria-hidden="true"
      />
      {/* 시트 */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        className="fixed bottom-0 left-1/2 z-50 w-full max-w-[var(--max-content-width)] -translate-x-1/2 rounded-t-[var(--radius-xl)] bg-[var(--color-surface)] shadow-[var(--shadow-4)] animate-in slide-in-from-bottom duration-300"
      >
        {/* 핸들 */}
        <div className="flex justify-center pt-3 pb-1">
          <div className="h-1 w-10 rounded-full bg-[var(--color-border)]" />
        </div>
        {title && (
          <h3 className="px-4 pb-3 text-[var(--font-size-h3)] font-semibold">
            {title}
          </h3>
        )}
        <div className="max-h-[70dvh] overflow-y-auto px-4" style={{ paddingBottom: 'calc(var(--bottom-tab-height) + 16px)' }}>{children}</div>
      </div>
    </>
  )
}
