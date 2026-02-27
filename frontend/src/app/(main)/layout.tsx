'use client'

/**
 * 메인 앱 레이아웃
 * 홈, 식사기록, 인사이트, 프로필, 구독 화면에 적용된다.
 * 상단 헤더 + 콘텐츠 영역 + 하단 탭바 구조.
 */
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import ToastContainer from '@/components/common/Toast'

const NAV_ITEMS = [
  { href: '/home', icon: '🏠', label: '홈' },
  { href: '/insights', icon: '📊', label: '이력' },
  { href: '/insights', icon: '💡', label: '인사이트' },
  { href: '/profile', icon: '👤', label: '프로필' },
]

export default function MainLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()

  return (
    <div className="flex min-h-dvh flex-col bg-[var(--color-background)]">
      {/* 상단 헤더 */}
      <header
        className="sticky top-0 z-40 flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface)] px-[var(--margin-mobile)]"
        style={{ height: 'var(--header-height)' }}
      >
        <div className="flex items-center gap-1">
          <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">📍</span>
          <span className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">현재 위치</span>
        </div>
        <div className="flex items-center gap-1">
          <img src="/images/logo.png" alt="런치픽" className="h-7 w-7 rounded-[var(--radius-xs)]" />
          <span className="text-[var(--font-size-h3)] font-bold text-[var(--color-primary)]">
            런치픽
          </span>
        </div>
        <button
          aria-label="알림"
          className="p-1 text-[var(--color-text-secondary)]"
        >
          🔔
        </button>
      </header>

      {/* 메인 콘텐츠 */}
      <main className="flex-1 overflow-y-auto" style={{ paddingBottom: 'var(--bottom-tab-height)' }}>
        {children}
      </main>

      {/* 하단 탭바 */}
      <nav
        className="fixed bottom-0 left-1/2 z-50 flex w-full -translate-x-1/2 items-center justify-around border-t border-[var(--color-border)] bg-[var(--color-surface)]"
        style={{ maxWidth: 'var(--max-content-width)', height: 'var(--bottom-tab-height)' }}
        aria-label="주 네비게이션"
      >
        <Link
          href="/home"
          className={`flex flex-col items-center gap-0.5 p-2 ${pathname === '/home' ? 'text-[var(--color-primary)]' : 'text-[var(--color-text-secondary)]'}`}
        >
          <span aria-hidden="true">🏠</span>
          <span className="text-[10px]">홈</span>
        </Link>
        <Link
          href="/insights"
          className={`flex flex-col items-center gap-0.5 p-2 ${pathname === '/insights' ? 'text-[var(--color-primary)]' : 'text-[var(--color-text-secondary)]'}`}
        >
          <span aria-hidden="true">📋</span>
          <span className="text-[10px]">이력</span>
        </Link>
        <Link
          href="/insights?tab=insight"
          className={`flex flex-col items-center gap-0.5 p-2 ${pathname === '/insights' ? 'text-[var(--color-text-secondary)]' : 'text-[var(--color-text-secondary)]'}`}
        >
          <span aria-hidden="true">📊</span>
          <span className="text-[10px]">인사이트</span>
        </Link>
        <Link
          href="/profile"
          className={`flex flex-col items-center gap-0.5 p-2 ${pathname === '/profile' ? 'text-[var(--color-primary)]' : 'text-[var(--color-text-secondary)]'}`}
        >
          <span aria-hidden="true">👤</span>
          <span className="text-[10px]">프로필</span>
        </Link>
      </nav>

      <ToastContainer />
    </div>
  )
}
