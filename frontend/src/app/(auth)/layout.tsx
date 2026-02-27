'use client'

/**
 * 인증/온보딩 레이아웃
 * 로그인, 취향 퀴즈, 위치 동의, 식이제한 설정 화면에 적용된다.
 * 하단 탭바 없이 전체 화면을 사용한다.
 */
import ToastContainer from '@/components/common/Toast'

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-dvh flex-col bg-[var(--color-background)]">
      {children}
      <ToastContainer />
    </div>
  )
}
