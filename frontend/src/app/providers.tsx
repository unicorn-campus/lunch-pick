'use client'

/**
 * 클라이언트 사이드 Provider 모음
 * TanStack Query, Zustand persist hydration 등을 여기서 초기화한다.
 * 앱 진입 시 authStore.initialize()를 호출하여 새로고침 후 인증 상태를 복원한다.
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import { useState, useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'

interface ProvidersProps {
  children: React.ReactNode
}

function AuthInitializer() {
  const initialize = useAuthStore((state) => state.initialize)
  useEffect(() => {
    initialize()
  }, [initialize])
  return null
}

export default function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // 5분간 데이터를 fresh로 유지
            staleTime: 1000 * 60 * 5,
            // 1회 재시도
            retry: 1,
            // 윈도우 포커스 시 자동 재요청 비활성 (모바일 UX 최적화)
            refetchOnWindowFocus: false,
          },
          mutations: {
            retry: 0,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <AuthInitializer />
      {children}
    </QueryClientProvider>
  )
}
