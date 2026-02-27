/**
 * 환경변수 설정
 * runtime-env.js (window.__runtime_config__)에서 런타임 주입된 값을 사용한다.
 * 빌드 타임 의존성 없이 배포 환경별 설정 전환이 가능하다.
 */
import { getRuntimeConfig } from './runtime'

function getConfig() {
  return getRuntimeConfig()
}

export const ENV = {
  /** 앱 실행 환경 */
  APP_ENV: getConfig().APP_ENV ?? 'development',

  /** 서비스별 API 호스트 */
  MEMBER_HOST: getConfig().MEMBER_HOST ?? 'http://localhost:4010',
  RECOMMENDATION_HOST: getConfig().RECOMMENDATION_HOST ?? 'http://localhost:4011',
  PAYMENT_HOST: getConfig().PAYMENT_HOST ?? 'http://localhost:4012',
  AI_HOST: getConfig().AI_HOST ?? 'http://localhost:4013',

  /** API 공통 경로 */
  API_GROUP: getConfig().API_GROUP ?? '/api/v1',

  /** 카카오 OAuth / Maps */
  KAKAO_CLIENT_ID: getConfig().KAKAO_CLIENT_ID ?? '',
  KAKAO_API_KEY: getConfig().KAKAO_API_KEY ?? '',
  KAKAO_JS_KEY: getConfig().KAKAO_JS_KEY ?? '',
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
