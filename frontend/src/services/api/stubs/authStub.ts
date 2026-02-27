/**
 * 인증 스텁 데이터 (Mock 환경 전용)
 * Prism Mock 서버 환경에서만 사용하던 stub 로직이다.
 * 실제 백엔드 연동 후에는 authService.ts를 사용한다.
 *
 * Mock 환경으로 복귀 시 참고용으로 보존한다.
 * runtime-env.js 에서 HOST를 http://localhost:4010 으로 변경하면 Mock 환경으로 복귀된다.
 */

/** Mock 환경용 참고 토큰 */
export const MOCK_ACCESS_TOKEN =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJtZW1iZXJJZCI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMSJ9.sample'

export const MOCK_MEMBER_ID = '550e8400-e29b-41d4-a716-446655440001'
export const MOCK_NICKNAME = '런치왕김과장'
