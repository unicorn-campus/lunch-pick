'use client'

/**
 * 길찾기 안내 페이지
 * UFR-REC-060: 도보 경로, 카카오맵/네이버지도 딥링크
 */
import { Suspense, useEffect, useRef, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import Button from '@/components/common/Button'
import { useToast } from '@/hooks/useToast'
import { ENV } from '@/config/env'

/** 카카오맵 컴포넌트 — SDK 로드 후 식당명으로 검색하여 지도 표시 */
function KakaoMap({ restaurantName, className }: { restaurantName: string; className?: string }) {
  const mapRef = useRef<HTMLDivElement>(null)
  const [mapError, setMapError] = useState(false)

  useEffect(() => {
    const appKey = ENV.KAKAO_JS_KEY || ENV.KAKAO_CLIENT_ID
    if (!appKey) {
      setMapError(true)
      return
    }

    // 이미 로드된 경우
    if (window.kakao?.maps?.services) {
      initMap()
      return
    }

    // 이미 스크립트 태그가 있으면 중복 추가 방지
    const existing = document.querySelector('script[src*="dapi.kakao.com"]')
    if (existing) {
      existing.addEventListener('load', () => initMap())
      return
    }

    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`
    script.onload = () => initMap()
    script.onerror = () => setMapError(true)
    document.head.appendChild(script)

    function initMap() {
      try {
        if (!window.kakao?.maps) {
          setMapError(true)
          return
        }
        window.kakao.maps.load(() => {
          if (!mapRef.current) return
          const defaultCenter = new window.kakao.maps.LatLng(37.5665, 126.978)
          const map = new window.kakao.maps.Map(mapRef.current, {
            center: defaultCenter,
            level: 3,
          })

          // 식당명으로 장소 검색
          const ps = new window.kakao.maps.services.Places()
          ps.keywordSearch(restaurantName, (data: any[], status: string) => {
            if (status === window.kakao.maps.services.Status.OK && data[0]) {
              const pos = new window.kakao.maps.LatLng(data[0].y, data[0].x)
              map.setCenter(pos)
              new window.kakao.maps.Marker({ map, position: pos })
            } else {
              // 검색 실패 시 기본 위치에 마커
              new window.kakao.maps.Marker({ map, position: defaultCenter })
            }
          })
        })
      } catch {
        setMapError(true)
      }
    }
  }, [restaurantName])

  if (mapError) {
    return (
      <div className={`flex items-center justify-center rounded-[var(--radius-l)] bg-gradient-to-br from-[#F3F4F6] to-[#E5E7EB] ${className}`}>
        <div className="flex items-center gap-3 text-[var(--color-text-secondary)]">
          <span className="text-[32px]">📍</span>
          <div>
            <div className="text-[var(--font-size-body2)] font-medium">지도를 불러올 수 없습니다</div>
            <div className="text-[var(--font-size-caption)]">아래 버튼으로 외부 지도를 열어주세요</div>
          </div>
        </div>
      </div>
    )
  }

  return <div ref={mapRef} className={`rounded-[var(--radius-l)] ${className}`} />
}

function NavigationContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const toast = useToast()

  const restaurantName = searchParams.get('name') ?? '식당'
  const restaurantAddress = searchParams.get('address') ?? '주소 정보 없음'
  const restaurantId = searchParams.get('restaurantId') ?? ''
  const recId = searchParams.get('recId') ?? ''

  // 도보 정보는 홈 페이지의 추천 카드에서 전달되지 않으므로 기본값 표시
  const walkMinutes = searchParams.get('walkMinutes') ?? '10'
  const distanceMeters = searchParams.get('distanceMeters') ?? '800'

  // 복귀 예상 시각: 현재 시간 + 도보 시간(분) * 2 + 30분(식사)
  const now = new Date()
  const totalAddMinutes = Number(walkMinutes) * 2 + 30
  const returnDate = new Date(now.getTime() + totalAddMinutes * 60 * 1000)
  const returnTimeStr = `${returnDate.getHours()}:${String(returnDate.getMinutes()).padStart(2, '0')}`

  function openKakaoMap() {
    // 카카오맵 딥링크: 주소 검색
    const encoded = encodeURIComponent(restaurantName)
    const kakaoUrl = `kakaomap://search?q=${encoded}`
    const kakaoWebUrl = `https://map.kakao.com/link/search/${encoded}`
    try {
      window.location.href = kakaoUrl
      setTimeout(() => {
        window.open(kakaoWebUrl, '_blank')
      }, 1500)
    } catch {
      window.open(kakaoWebUrl, '_blank')
    }
    toast.info('카카오맵으로 이동합니다')
  }

  function openNaverMap() {
    // 네이버지도 딥링크: 목적지 검색
    const encoded = encodeURIComponent(restaurantName)
    const naverUrl = `nmap://search?query=${encoded}&appname=com.unicorn.lunchpick`
    const naverWebUrl = `https://map.naver.com/v5/search/${encoded}`
    try {
      window.location.href = naverUrl
      setTimeout(() => {
        window.open(naverWebUrl, '_blank')
      }, 1500)
    } catch {
      window.open(naverWebUrl, '_blank')
    }
    toast.info('네이버지도로 이동합니다')
  }

  function goToRecord() {
    router.push(
      `/meal-record?name=${encodeURIComponent(restaurantName)}&restaurantId=${restaurantId}&recId=${recId}`,
    )
  }

  return (
    <div className="px-[var(--margin-mobile)] py-[var(--space-m)]">
      {/* 식당 정보 */}
      <div className="py-[var(--space-m)]">
        <h1 className="mb-[var(--space-xs)] text-[var(--font-size-h2)] font-bold">
          {restaurantName}
        </h1>
        <p className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
          {restaurantAddress}
        </p>
      </div>

      {/* 카카오맵 */}
      <KakaoMap
        restaurantName={restaurantName}
        className="mb-[var(--space-l)] h-[200px] w-full"
      />

      {/* 도보 정보 */}
      <div className="mb-[var(--space-l)] flex items-center gap-[var(--space-m)] rounded-[var(--radius-m)] bg-[var(--color-surface)] p-[var(--space-m)] shadow-[var(--shadow-1)]">
        <div className="text-[32px]">🚶</div>
        <div className="flex-1">
          <div className="text-[var(--font-size-h3)] font-bold">
            도보 {walkMinutes}분
          </div>
          <div className="text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">
            약 {distanceMeters}m
          </div>
        </div>
      </div>

      {/* 외부 지도 버튼 */}
      <div className="mb-[var(--space-l)] flex gap-[var(--space-s)]">
        <Button
          variant="secondary"
          size="md"
          onClick={openKakaoMap}
          aria-label="카카오맵에서 열기"
          className="flex-1"
        >
          카카오맵에서 열기
        </Button>
        <Button
          variant="secondary"
          size="md"
          onClick={openNaverMap}
          aria-label="네이버지도에서 열기"
          className="flex-1"
        >
          네이버지도에서 열기
        </Button>
      </div>

      {/* 복귀 예상 시각 */}
      <div className="mb-[var(--space-l)] rounded-[var(--radius-m)] bg-[#ECFDF5] p-[var(--space-m)] text-center text-[var(--font-size-body2)] text-[#065F46]">
        💡 {returnTimeStr}까지 복귀 가능해요
      </div>

      {/* 식사 후 기록 버튼 */}
      <div className="mt-[var(--space-l)]">
        <Button variant="primary" size="full" onClick={goToRecord}>
          식사 후 기록하기
        </Button>
      </div>
    </div>
  )
}

export default function NavigationPage() {
  return (
    <Suspense fallback={<div className="px-[var(--margin-mobile)] py-[var(--space-xl)] text-center text-[var(--font-size-body2)] text-[var(--color-text-secondary)]">불러오는 중...</div>}>
      <NavigationContent />
    </Suspense>
  )
}
