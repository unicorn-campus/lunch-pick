'use client'

interface LoadingProps {
  fullscreen?: boolean
  message?: string
}

export default function Loading({ fullscreen = false, message }: LoadingProps) {
  if (fullscreen) {
    return (
      <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-white/80">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-[var(--color-border)] border-t-[var(--color-primary)]" />
        {message && (
          <p className="mt-4 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">{message}</p>
        )}
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center py-16">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-[var(--color-border)] border-t-[var(--color-primary)]" />
      {message && (
        <p className="mt-3 text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">{message}</p>
      )}
    </div>
  )
}

/** 카드 스켈레톤 로딩 */
export function CardSkeleton() {
  return (
    <div className="animate-pulse rounded-[var(--radius-l)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-1)]">
      <div className="mb-3 h-4 w-3/4 rounded bg-[var(--color-border)]" />
      <div className="mb-2 h-3 w-1/2 rounded bg-[var(--color-border)]" />
      <div className="mb-4 h-3 w-2/3 rounded bg-[var(--color-border)]" />
      <div className="h-10 w-full rounded-[var(--radius-m)] bg-[var(--color-border)]" />
    </div>
  )
}
