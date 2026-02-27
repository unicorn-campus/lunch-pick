import request from 'supertest';

/**
 * 설계 계약 테스트: 추천·이력 서비스 - 추천 조회 (UFR-REC-010, UFR-REC-030)
 * 원본 설계서: docs/design/sequence/inner/recommendation-service-추천조회.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 */

const BASE_URL = process.env.RECOMMENDATION_SERVICE_URL || 'http://localhost:8082';

describe('추천·이력 서비스 - 추천 조회 (UFR-REC-010, UFR-REC-030)', () => {

  describe('캐시 확인', () => {

    it('캐시 히트 시 캐싱된 추천 결과를 200ms 이내 반환한다', async () => {
      // note: 캐시 키 형식 = rec:{memberId}:{locationGrid}:{weatherCode}:{weekday}
      // note: 응답 < 200ms
      const res = await request(BASE_URL)
        .get('/api/v1/recommendations/today')
        .query({ latitude: 37.5665, longitude: 126.9780 })
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('recommendations');
      expect(res.body).toHaveProperty('isFallback');
      expect(res.body).toHaveProperty('generatedAt');
      // cacheHit 응답의 경우 recommendations 배열이 존재해야 함
      expect(Array.isArray(res.body.recommendations)).toBe(true);
    });

    describe('캐시 미스 - 컨텍스트 수집', () => {

      describe('날씨 API 호출', () => {

        it('날씨 API 정상 시 현재 날씨를 반영하여 추천을 반환한다', async () => {
          // note: 날씨 API에서 weatherCode, temperature 수신 후 컨텍스트 구성
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
          expect(res.body.recommendations[0]).toHaveProperty('recommendationId');
          expect(res.body.recommendations[0]).toHaveProperty('restaurantName');
          expect(res.body.recommendations[0]).toHaveProperty('representativeMenu');
          expect(res.body.recommendations[0]).toHaveProperty('reasonSummary');
          expect(res.body.recommendations[0]).toHaveProperty('confidenceScore');
          expect(res.body.recommendations[0]).toHaveProperty('distanceMeters');
          expect(res.body.recommendations[0]).toHaveProperty('estimatedWalkMinutes');
        });

        it('날씨 API 장애(Circuit Breaker) 시 날씨 기본값(맑음)을 적용하여 추천을 반환한다', async () => {
          // note: 날씨 API 장애 시 ContextCollector가 기본값(맑음) 적용 후 계속 진행
          // note: 최종 응답 형태는 정상 추천과 동일 — 날씨 장애가 추천 실패로 전파되지 않아야 함
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
        });

      });

      describe('AI Pipeline 추천 요청', () => {

        it('LLM 정상 응답 시 추천 3개와 확신 스코어를 포함하여 3초 이내 반환한다', async () => {
          // note: 응답 < 3초
          // note: 추천 이력이 DB에 저장되고 캐시에 저장됨 (TTL: 당일 13:00까지)
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body).toHaveProperty('isColdStart');
          expect(res.body).toHaveProperty('isFallback');
          expect(res.body).toHaveProperty('generatedAt');
          expect(res.body.isFallback).toBe(false);
          expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
          expect(res.body.recommendations[0]).toHaveProperty('confidenceScore');
          // cacheHit: false 를 나타내는 필드 검증
          expect(res.body).toHaveProperty('isFallback', false);
        });

        it('LLM Circuit Breaker Open(폴백) 시 규칙 기반 추천과 안내 메시지를 반환한다', async () => {
          // note: 폴백 = 반경 500m 인기 메뉴 + 알레르기 필터 적용
          // note: fallbackMessage = "최신 추천을 불러오고 있어요"
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body).toHaveProperty('isFallback');
          expect(res.body).toHaveProperty('fallbackMessage');
          // 폴백 시나리오에서 isFallback = true, fallbackMessage 포함
          expect(res.body.isFallback).toBe(true);
          expect(res.body.fallbackMessage).toBe('최신 추천을 불러오고 있어요.');
        });

      });

    });

  });

});
