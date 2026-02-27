'use client'

import { ReactNode, useEffect } from 'react'
import Button from './Button'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: ReactNode
  primaryLabel?: string
  onPrimary?: () => void
  secondaryLabel?: string
  onSecondary?: () => void
  primaryVariant?: 'primary' | 'danger'
}

export default function Modal({
  isOpen,
  onClose,
  title,
  children,
  primaryLabel,
  onPrimary,
  secondaryLabel,
  onSecondary,
  primaryVariant = 'primary',
}: ModalProps) {
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
      <div
        className="fixed inset-0 z-50 bg-[var(--color-overlay)]"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-48px)] max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-[var(--radius-l)] bg-[var(--color-surface)] p-6 shadow-[var(--shadow-4)]"
      >
        <h3
          id="modal-title"
          className="mb-3 text-[var(--font-size-h3)] font-semibold"
        >
          {title}
        </h3>
        <div className="mb-6 text-[var(--font-size-body2)] text-[var(--color-text-secondary)] leading-[var(--line-height-body2)]">
          {children}
        </div>
        <div className="flex gap-2">
          {secondaryLabel && (
            <Button variant="secondary" size="full" onClick={onSecondary ?? onClose}>
              {secondaryLabel}
            </Button>
          )}
          {primaryLabel && (
            <Button variant={primaryVariant} size="full" onClick={onPrimary}>
              {primaryLabel}
            </Button>
          )}
        </div>
      </div>
    </>
  )
}
