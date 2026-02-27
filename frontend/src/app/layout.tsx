import type { Metadata, Viewport } from 'next'
import './globals.css'
import Providers from './providers'

export const metadata: Metadata = {
  title: '런치픽 - 오늘 점심, 3초면 결정',
  description: '매일 반복되는 점심 메뉴 고민을 AI가 해결해드려요. 당신의 취향을 학습해 딱 맞는 메뉴를 추천합니다.',
  keywords: ['점심 추천', '메뉴 추천', '런치픽', '직장인 점심', 'AI 추천'],
  manifest: '/manifest.json',
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  themeColor: '#FF6B35',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          {/* 모바일 앱 느낌의 최대 폭 레이아웃: 480px 중앙 정렬 */}
          <div className="mx-auto min-h-dvh" style={{ maxWidth: 'var(--max-content-width)' }}>
            {children}
          </div>
        </Providers>
      </body>
    </html>
  )
}
