'use client'

import { useState, useEffect } from 'react'
import { getRuntimeConfig } from '@/config/runtime'

const DEFAULT_LOCATION = { latitude: 37.5665, longitude: 126.978 }

interface GeolocationState {
  latitude: number
  longitude: number
  isLoading: boolean
  error: string | null
  isDefault: boolean
  locationName: string | null
}

/** 카카오 역지오코딩으로 시군구 동 이름을 가져온다 */
async function reverseGeocode(lat: number, lng: number): Promise<string | null> {
  const config = getRuntimeConfig()
  const apiKey = config.KAKAO_API_KEY
  if (!apiKey) return null

  try {
    const res = await fetch(
      `https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=${lng}&y=${lat}`,
      { headers: { Authorization: `KakaoAK ${apiKey}` } },
    )
    if (!res.ok) return null
    const data = await res.json()
    // region_type "H" = 행정동
    const region = data.documents?.find((d: { region_type: string }) => d.region_type === 'H') ?? data.documents?.[0]
    if (!region) return null
    const gu = region.region_2depth_name // 예: 마포구
    const dong = region.region_3depth_name // 예: 아현동
    return dong ? `${gu} ${dong}` : gu || null
  } catch {
    return null
  }
}

export function useGeolocation(): GeolocationState {
  const [state, setState] = useState<GeolocationState>({
    ...DEFAULT_LOCATION,
    isLoading: true,
    error: null,
    isDefault: true,
    locationName: null,
  })

  useEffect(() => {
    if (!navigator.geolocation) {
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: '이 브라우저에서는 위치 서비스를 지원하지 않아요.',
        isDefault: true,
      }))
      return
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords
        const locationName = await reverseGeocode(latitude, longitude)
        setState({
          latitude,
          longitude,
          isLoading: false,
          error: null,
          isDefault: false,
          locationName,
        })
      },
      async (error) => {
        let errorMessage = '위치를 가져올 수 없어요.'
        if (error.code === error.PERMISSION_DENIED) {
          errorMessage = '위치 권한이 거부되었어요. 기본 위치로 추천합니다.'
        } else if (error.code === error.TIMEOUT) {
          errorMessage = '위치 요청 시간이 초과되었어요.'
        }
        const locationName = await reverseGeocode(DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude)
        setState(prev => ({
          ...prev,
          isLoading: false,
          error: errorMessage,
          isDefault: true,
          locationName,
        }))
      },
      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 300000,
      },
    )
  }, [])

  return state
}
