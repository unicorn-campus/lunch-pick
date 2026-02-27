/**
 * 404 Not Found 페이지
 */
import Link from 'next/link'

export default function NotFound() {
  return (
    <div
      className="flex min-h-dvh flex-col items-center justify-center gap-6 p-[var(--margin-mobile)] text-center"
      style={{ backgroundColor: 'var(--color-background)' }}
    >
      <p
        className="font-bold"
        style={{ fontSize: 'var(--font-size-h1)', color: 'var(--color-text-primary)' }}
      >
        404
      </p>
      <p style={{ fontSize: 'var(--font-size-body1)', color: 'var(--color-text-secondary)' }}>
        페이지를 찾을 수 없어요.
      </p>
      <Link
        href="/"
        className="flex h-12 items-center justify-center rounded-[var(--radius-m)] px-6 font-medium text-white"
        style={{ backgroundColor: 'var(--color-primary)', fontSize: 'var(--font-size-label)' }}
      >
        홈으로 돌아가기
      </Link>
    </div>
  )
}
