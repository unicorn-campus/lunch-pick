'use client'

/**
 * 로그인 페이지
 * UFR-MBR-010: 카카오 소셜 로그인
 * POST /api/v1/auth/kakao
 */
import { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import Button from '@/components/common/Button'
import { useKakaoLogin } from '@/hooks/useMember'
import { useToast } from '@/hooks/useToast'

function LoginContent() {
  const router = useRouter()
  const toast = useToast()
  const searchParams = useSearchParams()
  const { mutate: kakaoLogin, isPending } = useKakaoLogin()
  const [isLoading, setIsLoading] = useState(false)

  const error = searchParams.get('error')
  useEffect(() => {
    if (error) toast.error('로그인에 실패했어요. 다시 시도해주세요.')
  }, [error, toast])

  function handleKakaoLogin() {
    setIsLoading(true)
    const urlParams = new URLSearchParams(window.location.search)
    const authorizationCode = urlParams.get('code')

    if (!authorizationCode) {
      const KAKAO_CLIENT_ID = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID ?? ''
      if (!KAKAO_CLIENT_ID) {
        // 카카오 Client ID 미설정 시 데모 모드로 안내
        toast.info('카카오 앱 키가 설정되지 않았어요. 데모 모드를 이용해주세요.')
        setIsLoading(false)
        return
      }
      const REDIRECT_URI = encodeURIComponent(`${window.location.origin}/login`)
      window.location.href = `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`
      return
    }

    kakaoLogin(
      { authorizationCode },
      {
        onSuccess: (data) => {
          toast.success('카카오 로그인 완료!')
          if (data.isNewUser || !data.onboardingCompleted) {
            router.push('/onboarding/quiz')
          } else {
            router.push('/home')
          }
        },
        onError: () => {
          toast.error('인증에 실패했어요. 다시 시도해주세요.')
          setIsLoading(false)
        },
      },
    )
  }

  function handleDemoLogin() {
    toast.success('데모 모드로 시작합니다!')
    router.push('/home')
  }

  return (
    <main
      role="main"
      className="flex min-h-dvh flex-col items-center justify-center bg-[var(--color-surface)] px-[var(--space-xl)] text-center"
    >
      <img
        src="/images/logo.png"
        alt="런치픽 로고"
        className="mb-[var(--space-s)] h-20 w-20 rounded-[var(--radius-l)]"
      />

      <h1
        className="mb-[var(--space-s)] text-[var(--font-size-h1)] font-bold text-[var(--color-primary)]"
        style={{ lineHeight: 'var(--line-height-h1)' }}
      >
        런치픽
      </h1>

      <p
        className="mb-[var(--space-m)] text-[var(--font-size-body1)] text-[var(--color-text-secondary)]"
        style={{ lineHeight: 'var(--line-height-body1)' }}
      >
        3분이면 당신만의<br />점심 파트너가 완성돼요
      </p>

      <img
        src="/images/hero-food.png"
        alt="다양한 점심 음식 일러스트"
        className="mb-[var(--space-xxl)] h-48 w-48 object-contain"
      />

      <button
        onClick={handleKakaoLogin}
        disabled={isPending || isLoading}
        aria-label="카카오 계정으로 로그인"
        className="flex h-12 w-full max-w-xs items-center justify-center gap-2 rounded-[var(--radius-m)] bg-[#FEE500] text-[#191919] font-medium text-[var(--font-size-label)] transition-transform duration-[var(--duration-fast)] active:scale-[0.97] hover:bg-[#F5DC00] disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {isPending || isLoading ? (
          <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-[#191919] border-t-transparent" />
        ) : (
          <>
            <img src="/images/kakao-logo.png" alt="" aria-hidden="true" className="h-5 w-5 object-contain" />
            카카오로 시작하기
          </>
        )}
      </button>

      <button
        onClick={handleDemoLogin}
        aria-label="데모 모드로 시작"
        className="mt-3 flex h-12 w-full max-w-xs items-center justify-center gap-2 rounded-[var(--radius-m)] border border-[var(--color-border)] bg-[var(--color-surface)] font-medium text-[var(--font-size-label)] text-[var(--color-text-secondary)] transition-transform duration-[var(--duration-fast)] active:scale-[0.97] hover:bg-[var(--color-background)]"
      >
        🎬 데모 모드로 둘러보기
      </button>

      <p
        className="mt-[var(--space-xxl)] text-[var(--font-size-caption)] text-[var(--color-text-disabled)]"
        style={{ lineHeight: 'var(--line-height-body2)' }}
      >
        로그인 시{' '}
        <a href="#" className="text-[var(--color-text-secondary)] underline">이용약관</a>
        {' '}및{' '}
        <a href="#" className="text-[var(--color-text-secondary)] underline">개인정보처리방침</a>에
        <br />동의하는 것으로 간주합니다.
      </p>
    </main>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginContent />
    </Suspense>
  )
}
