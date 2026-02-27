import request from 'supertest';

/**
 * 설계 계약 테스트: 오늘의 추천 조회 (외부 시퀀스)
 * 원본 설계서: docs/design/sequence/outer/오늘의추천조회.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 서비스 간 호출 흐름:
 *   API Gateway → 추천·이력 서비스 → 회원 서비스(취향 프로파일)
 *                                   → 날씨 API
 *                                   → AI Pipeline(추천 생성)
 *                                   → Redis(캐시)
 */

const BASE_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';

describe('오늘의 추천 조회', () => {

  describe('오늘의 추천 3개 조회 (UFR-REC-010)', () => {

    describe('위치 인식 결과에 따른 분기', () => {

      it('위치 인식 성공 시 추천 조회 요청을 진행한다', async () => {
        // note: Frontend에서 GPS 자동 인식 성공 후 위도/경도를 포함하여 요청
        // note: API Gateway에서 JWT 검증 후 추천·이력 서비스로 전달
        const res = await request(BASE_URL)
          .get('/api/v1/recommendations/today')
          .query({ latitude: 37.5665, longitude: 126.9780 })
          .set('Authorization', 'Bearer test-jwt-token')
          .expect(200);

        expect(res.body).toHaveProperty('recommendations');
        expect(res.body).toHaveProperty('isColdStart');
        expect(res.body).toHaveProperty('isFallback');
        expect(res.body).toHaveProperty('generatedAt');
      });

      it('위치 미확인 시 위치 설정 안내 메시지를 반환한다', async () => {
        // note: Frontend에서 위치 인식 실패 시 수동 입력 경로 제공
        // note: 위도/경도 누락 시 400 응답
        const res = await request(BASE_URL)
          .get('/api/v1/recommendations/today')
          .set('Authorization', 'Bearer test-jwt-token')
          // latitude, longitude 미전달
          .expect(400);

        expect(res.body).toHaveProperty('error', 'LOCATION_REQUIRED');
        expect(res.body).toHaveProperty('message', '위치를 설정해주세요.');
        expect(res.body).toHaveProperty('timestamp');
      });

    });

    describe('위치 인식 성공 후 — 추천 캐시 조회 분기', () => {

      it('캐시 히트(< 200ms) 시 캐싱된 추천 결과를 즉시 반환한다', async () => {
        // note: 캐시 키 = rec:{user_id}:{location_grid}:{weather_code}:{weekday}
        // note: 추천·이력 서비스 → Redis 캐시 조회 → 200 OK 반환
        const res = await request(BASE_URL)
          .get('/api/v1/recommendations/today')
          .query({ latitude: 37.5665, longitude: 126.9780 })
          .set('Authorization', 'Bearer test-jwt-token')
          .expect(200);

        expect(res.body).toHaveProperty('recommendations');
        expect(Array.isArray(res.body.recommendations)).toBe(true);
        expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
        expect(res.body.recommendations[0]).toHaveProperty('recommendationId');
        expect(res.body.recommendations[0]).toHaveProperty('restaurantName');
        expect(res.body.recommendations[0]).toHaveProperty('representativeMenu');
        expect(res.body.recommendations[0]).toHaveProperty('reasonSummary');
        expect(res.body.recommendations[0]).toHaveProperty('confidenceScore');
        expect(res.body.recommendations[0]).toHaveProperty('distanceMeters');
        expect(res.body.recommendations[0]).toHaveProperty('estimatedWalkMinutes');
        expect(res.body.recommendations[0]).toHaveProperty('category');
        expect(res.body.recommendations[0]).toHaveProperty('isFallback');
      });

      describe('캐시 미스 — AI Pipeline 호출 분기', () => {

        it('LLM 정상 응답(< 3초) 시 AI 추천 3개를 캐시 저장 후 반환한다', async () => {
          /**
           * 캐시 미스 흐름:
           * 1. 추천·이력 서비스 → 회원 서비스: GET /internal/members/{user_id}/taste-profile
           * 2. 추천·이력 서비스 → 날씨 API: GET /current-weather
           * 3. 추천·이력 서비스 → 추천·이력 DB: 최근 7일 식사 이력 조회
           * 4. 추천·이력 서비스 → AI Pipeline: POST /internal/ai/recommend
           * 5. AI Pipeline → LLM API 호출 → 추천 3개 생성
           * 6. 추천·이력 서비스 → Redis: 추천 결과 캐싱 (TTL: 당일 13:00)
           */
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
          expect(res.body.recommendations[0]).toHaveProperty('confidenceScore');
          expect(res.body).toHaveProperty('isFallback', false);
          expect(res.body).toHaveProperty('isColdStart');
          expect(res.body).toHaveProperty('generatedAt');
        });

        it('LLM Circuit Breaker Open(폴백) 시 규칙 기반 추천과 "최신 추천을 불러오고 있어요" 안내를 반환한다', async () => {
          /**
           * 폴백 흐름:
           * - AI Pipeline → 폴백 추천 반환 (반경 500m 인기 메뉴 + 알레르기 필터)
           * - 추천·이력 서비스 → Redis: 폴백 추천 캐싱
           * - 응답에 fallbackMessage = "최신 추천을 불러오고 있어요"
           */
          const res = await request(BASE_URL)
            .get('/api/v1/recommendations/today')
            .query({ latitude: 37.5665, longitude: 126.9780 })
            .set('Authorization', 'Bearer test-jwt-token')
            .expect(200);

          expect(res.body).toHaveProperty('recommendations');
          expect(res.body).toHaveProperty('isFallback', true);
          expect(res.body).toHaveProperty('fallbackMessage');
          expect(res.body.fallbackMessage).toBeTruthy();
        });

      });

    });

  });

  describe('추천 이유 상세 확인 (UFR-REC-020)', () => {

    it('추천 이유 조회 성공 시 자연어 이유, 확신 스코어, 컨텍스트 태그를 반환한다', async () => {
      // note: 자연어 이유 형태: "비 오는 날 + 어제 양식 → 따뜻한 한식 추천"
      // note: 컨텍스트 태그: [날씨, 이력, 취향, 요일, 시간] 중 해당하는 것
      const res = await request(BASE_URL)
        .get('/api/v1/recommendations/rec-550e8400-e29b-41d4-a716-446655440010/reason')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('recommendationId');
      expect(res.body).toHaveProperty('naturalLanguageReason');
      expect(res.body).toHaveProperty('confidenceScore');
      expect(res.body).toHaveProperty('contextTags');
      expect(res.body).toHaveProperty('isReasonReady', true);
      expect(res.body).toHaveProperty('fallbackMessage', null);
      expect(Array.isArray(res.body.contextTags)).toBe(true);
    });

    it('설명 생성 실패 시 기본 추천 이유(거리/평점)와 "추천 이유를 준비 중이에요" 안내를 반환한다', async () => {
      // note: LLM 설명 생성 실패 시에도 200 OK 반환 — 기본 이유(거리/평점) 포함
      // note: isReasonReady = false, fallbackMessage 포함
      const res = await request(BASE_URL)
        .get('/api/v1/recommendations/rec-550e8400-e29b-41d4-a716-446655440010/reason')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('recommendationId');
      expect(res.body).toHaveProperty('naturalLanguageReason');
      expect(res.body).toHaveProperty('isReasonReady', false);
      expect(res.body).toHaveProperty('fallbackMessage');
      expect(res.body.fallbackMessage).toBeTruthy();
    });

  });

});
