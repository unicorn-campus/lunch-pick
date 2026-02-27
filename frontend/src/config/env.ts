/**
 * Next.js 환경변수 설정
 * Next.js App Router에서는 런타임 환경변수를 NEXT_PUBLIC_ 접두사로 관리한다.
 * 서버 컴포넌트에서는 접두사 없이 사용 가능.
 */

export const ENV = {
  /** 앱 실행 환경 */
  APP_ENV: process.env.NEXT_PUBLIC_APP_ENV ?? 'development',

  /** 서비스별 API 호스트 (Mock: Prism, Prod: 실제 서버) */
  MEMBER_HOST: process.env.NEXT_PUBLIC_MEMBER_HOST ?? 'http://localhost:4010',
  RECOMMENDATION_HOST: process.env.NEXT_PUBLIC_RECOMMENDATION_HOST ?? 'http://localhost:4011',
  PAYMENT_HOST: process.env.NEXT_PUBLIC_PAYMENT_HOST ?? 'http://localhost:4012',
  AI_HOST: process.env.NEXT_PUBLIC_AI_HOST ?? 'http://localhost:4013',

  /** API 공통 경로 */
  API_GROUP: '/api/v1',

  /** NextAuth */
  NEXTAUTH_URL: process.env.NEXTAUTH_URL ?? 'http://localhost:3000',
  NEXTAUTH_SECRET: process.env.NEXTAUTH_SECRET ?? '',

  /** 카카오 OAuth */
  KAKAO_CLIENT_ID: process.env.KAKAO_CLIENT_ID ?? '',
  KAKAO_CLIENT_SECRET: process.env.KAKAO_CLIENT_SECRET ?? '',
} as const

/** 서비스명으로 baseURL 반환 */
export function getServiceBaseUrl(service: 'member' | 'recommendation' | 'payment' | 'ai'): string {
  const hostMap = {
    member: ENV.MEMBER_HOST,
    recommendation: ENV.RECOMMENDATION_HOST,
    payment: ENV.PAYMENT_HOST,
    ai: ENV.AI_HOST,
  }
  return `${hostMap[service]}${ENV.API_GROUP}`
}
