/**
 * 루트 페이지
 * 인증 상태에 따라 홈 또는 로그인으로 리디렉션한다.
 * 실제 구현 전까지는 앱 진입점 안내 화면을 표시한다.
 */
import Link from 'next/link'

export default function RootPage() {
  return (
    <div
      className="flex min-h-dvh flex-col items-center justify-center gap-8 p-[var(--margin-mobile)]"
      style={{ backgroundColor: 'var(--color-background)' }}
    >
      {/* 브랜드 로고 영역 */}
      <div className="flex flex-col items-center gap-3 text-center">
        <div
          className="flex h-16 w-16 items-center justify-center rounded-[var(--radius-l)] text-3xl"
          style={{ backgroundColor: 'var(--color-primary)' }}
          aria-hidden="true"
        >
          🍽️
        </div>
        <h1
          className="font-bold"
          style={{
            fontSize: 'var(--font-size-h1)',
            lineHeight: 'var(--line-height-h1)',
            color: 'var(--color-text-primary)',
          }}
        >
          런치픽
        </h1>
        <p style={{ fontSize: 'var(--font-size-body1)', color: 'var(--color-text-secondary)' }}>
          오늘 점심, 3초면 결정
        </p>
      </div>

      {/* 페이지 링크 */}
      <nav className="flex w-full flex-col gap-3">
        <Link
          href="/login"
          className="flex h-12 items-center justify-center rounded-[var(--radius-m)] font-medium text-white transition-opacity hover:opacity-90"
          style={{ backgroundColor: 'var(--color-primary)', fontSize: 'var(--font-size-label)' }}
        >
          시작하기 (로그인)
        </Link>
        {process.env.NODE_ENV === 'development' && (
          <Link
            href="/home"
            className="flex h-12 items-center justify-center rounded-[var(--radius-m)] border font-medium transition-opacity hover:opacity-80"
            style={{
              borderColor: 'var(--color-primary)',
              color: 'var(--color-primary)',
              fontSize: 'var(--font-size-label)',
            }}
          >
            홈으로 (개발용)
          </Link>
        )}
      </nav>

      <p
        className="text-center"
        style={{ fontSize: 'var(--font-size-caption)', color: 'var(--color-text-disabled)' }}
      >
        프론트엔드 프로젝트 초기화 완료 — 페이지 구현은 Sprint 1~7에서 진행
      </p>
    </div>
  )
}
