import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  /**
   * 개발 모드 표시기 비활성화
   * 하단 탭바를 가리는 Dev Tools 플로팅 버튼을 숨긴다.
   */
  devIndicators: false,

  /**
   * 이미지 최적화
   * 외부 이미지 도메인은 실제 사용 시 추가한다.
   */
  images: {
    remotePatterns: [
      // 카카오 프로필 이미지
      {
        protocol: 'https',
        hostname: 'k.kakaocdn.net',
      },
      // 음식 이미지 (실제 CDN 도메인 추가 예정)
    ],
  },

  /**
   * API 리버스 프록시 설정
   * 개발 환경에서 CORS 없이 Mock 서버로 요청을 프록시한다.
   */
  async rewrites() {
    return [
      // member-service (prism-member:4010)
      {
        source: '/proxy/member/:path*',
        destination: `${process.env.NEXT_PUBLIC_MEMBER_HOST ?? 'http://localhost:4010'}/api/v1/:path*`,
      },
      // recommendation-service (prism-recommendation:4011)
      {
        source: '/proxy/recommendation/:path*',
        destination: `${process.env.NEXT_PUBLIC_RECOMMENDATION_HOST ?? 'http://localhost:4011'}/api/v1/:path*`,
      },
      // payment-service (prism-payment:4012)
      {
        source: '/proxy/payment/:path*',
        destination: `${process.env.NEXT_PUBLIC_PAYMENT_HOST ?? 'http://localhost:4012'}/api/v1/:path*`,
      },
    ]
  },
}

export default nextConfig
