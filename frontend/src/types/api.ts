/**
 * 공통 API 타입 정의
 * API 설계서(*.yaml) 기반으로 공통 응답 구조를 정의한다.
 */

/** 공통 API 성공 응답 래퍼 */
export interface ApiResponse<T> {
  success: boolean
  data: T
  error: string | null
  timestamp: string
}

/** 공통 에러 응답 */
export interface ApiError {
  error: string
  message: string
  timestamp: string
}

/** 페이지네이션 응답 */
export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
}

/** HTTP 메서드 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

/** API 요청 상태 */
export type RequestStatus = 'idle' | 'loading' | 'success' | 'error'
