import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  /**
   * 컨테이너 배포용 standalone 출력 모드
   * Docker 이미지 크기를 최소화하고 Node.js 단독 실행을 지원한다.
   */
  output: 'standalone',

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
}

export default nextConfig
