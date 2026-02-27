/**
 * 서비스별 Axios 인스턴스
 * 각 마이크로서비스마다 독립된 baseURL을 가진다.
 * Mock → 실제 서버 전환 시 환경변수(.env.local)만 변경하면 된다.
 */
import { createApiClient } from './client'
import { getServiceBaseUrl } from '@/config/env'

/** 회원 서비스 클라이언트 (member-service:8081, Mock: prism-member:4010) */
export const memberApiClient = createApiClient(getServiceBaseUrl('member'))

/** 추천 서비스 클라이언트 (recommendation-service:8082, Mock: prism-recommendation:4011) */
export const recommendationApiClient = createApiClient(getServiceBaseUrl('recommendation'))

/** 결제 서비스 클라이언트 (payment-service:8083, Mock: prism-payment:4012) */
export const paymentApiClient = createApiClient(getServiceBaseUrl('payment'))
