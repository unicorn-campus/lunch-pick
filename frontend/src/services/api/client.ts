/**
 * Axios 공통 클라이언트
 * 요청/응답 인터셉터로 JWT 인증과 에러 처리를 담당한다.
 *
 * 토큰 저장 키: lunchpick_token (localStorage)
 * 401 응답: 토큰 제거 후 /login 리다이렉트
 * 403 응답: 콘솔 경고 (권한 없음)
 * 네트워크 에러: 콘솔 경고 (서버 미응답)
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'

const DEFAULT_CONFIG: AxiosRequestConfig = {
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
}

export function createApiClient(baseURL: string, config?: AxiosRequestConfig): AxiosInstance {
  const client = axios.create({
    baseURL,
    ...DEFAULT_CONFIG,
    ...config,
  })

  // 요청 인터셉터: JWT 토큰 주입
  client.interceptors.request.use(
    (cfg) => {
      // 클라이언트 사이드에서만 localStorage 접근
      if (typeof window !== 'undefined') {
        const token = localStorage.getItem('lunchpick_token')
        if (token) {
          cfg.headers.Authorization = `Bearer ${token}`
        }
      }
      return cfg
    },
    (error: AxiosError) => Promise.reject(error),
  )

  // 응답 인터셉터: 공통 에러 처리
  client.interceptors.response.use(
    (response: AxiosResponse) => response,
    (error: AxiosError) => {
      if (!error.response) {
        // 네트워크 오류 (서버 미응답, 타임아웃)
        console.warn('[API] 서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.')
        return Promise.reject(error)
      }

      if (error.response.status === 401) {
        // 인증 만료: 토큰 제거 후 로그인 페이지로
        if (typeof window !== 'undefined') {
          localStorage.removeItem('lunchpick_token')
          // Zustand persist 스토어 초기화
          localStorage.removeItem('lunchpick-auth')
          window.location.href = '/login'
        }
      }

      if (error.response.status === 403) {
        // 권한 없음
        console.warn('[API] 접근 권한이 없습니다.')
      }

      return Promise.reject(error)
    },
  )

  return client
}
