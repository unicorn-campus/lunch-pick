/**
 * 공통 포맷 유틸리티
 */

/** 가격 포맷 (숫자 → "8,500원") */
export function formatPrice(price: number): string {
  return `${price.toLocaleString('ko-KR')}원`
}

/** 거리 포맷 (미터 → "350m" 또는 "1.2km") */
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${meters}m`
  return `${(meters / 1000).toFixed(1)}km`
}

/** 도보 시간 포맷 (분 → "도보 5분") */
export function formatWalkingTime(minutes: number): string {
  return `도보 ${minutes}분`
}

/** 날짜 포맷 (ISO → "2월 26일 (수)") */
export function formatDate(isoDate: string): string {
  const date = new Date(isoDate)
  const days = ['일', '월', '화', '수', '목', '금', '토']
  const month = date.getMonth() + 1
  const day = date.getDate()
  const dayOfWeek = days[date.getDay()]
  return `${month}월 ${day}일 (${dayOfWeek})`
}

/** 시간 포맷 (ISO → "12:15") */
export function formatTime(isoDate: string): string {
  const date = new Date(isoDate)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

/** 카테고리 한글 레이블 */
export function getCategoryLabel(category: string): string {
  const labels: Record<string, string> = {
    KOREAN: '한식',
    WESTERN: '양식',
    CHINESE: '중식',
    JAPANESE: '일식',
    OTHER: '기타',
  }
  return labels[category] ?? category
}

/** 확신 스코어 색상 클래스 반환 */
export function getConfidenceColor(score: number): string {
  if (score >= 80) return 'var(--color-primary)'
  if (score >= 60) return 'var(--color-warning)'
  return 'var(--color-text-secondary)'
}
