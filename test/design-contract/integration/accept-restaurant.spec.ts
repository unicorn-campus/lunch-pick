import request from 'supertest';

/**
 * 설계 계약 테스트: 추천 수락 및 식당 선택 (외부 시퀀스)
 * 원본 설계서: docs/design/sequence/outer/추천수락-식당선택.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 서비스 간 호출 흐름:
 *   API Gateway → 추천·이력 서비스 → 추천·이력 DB (수락/거절 이력 저장)
 *                                   → Redis (대체 추천 캐시 조회)
 *                                   → 지도 API (도보 경로)
 */

const BASE_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';

describe('추천 수락 및 식당 선택', () => {

  describe('추천 수락 (UFR-REC-040)', () => {

    it('추천 수락 시 수락 이력을 저장하고 식당 정보를 반환한다', async () => {
      /**
       * 수락 흐름:
       * - Frontend에서 반응 시간(ms) 측정 후 함께 전송 (취향 학습에 활용)
       * - 추천·이력 서비스 → DB: 수락 이력 저장 {회원ID, 추천ID, 식당ID, 수락시각, 반응시간}
       * - 응답: acceptanceId, restaurantId, restaurantName, restaurantAddress
       */
      const res = await request(BASE_URL)
        .post('/api/v1/recommendations/rec-550e8400-e29b-41d4-a716-446655440010/accept')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          acceptedAt: '2026-02-26T12:05:30Z',
          reactionTimeMs: 4200,
        })
        .expect(200);

      expect(res.body).toHaveProperty('acceptanceId');
      expect(res.body).toHaveProperty('restaurantId');
      expect(res.body).toHaveProperty('restaurantName');
      expect(res.body).toHaveProperty('restaurantAddress');
      expect(res.body).toHaveProperty('message');
    });

  });

  describe('추천 거절 및 대체 추천 (UFR-REC-050)', () => {

    it('대체 추천 후보 존재 시 "이런 건 어때요?" 메시지와 대체 추천 1개를 반환한다', async () => {
      /**
       * 거절 흐름:
       * - 추천·이력 서비스 → DB: 거절 이력 저장 {회원ID, 추천ID, 거절사유, 거절시각}
       * - 추천·이력 서비스 → Redis: 대체 추천 캐시 확인 (거절 식당 제외 차순위)
       * - 대체 후보 존재 → hasAlternative: true
       */
      const res = await request(BASE_URL)
        .post('/api/v1/recommendations/rec-550e8400-e29b-41d4-a716-446655440010/reject')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          rejectReason: 'MOOD_NOT_MATCH',
        })
        .expect(200);

      expect(res.body).toHaveProperty('rejected', true);
      expect(res.body).toHaveProperty('hasAlternative', true);
      expect(res.body).toHaveProperty('alternativeRecommendation');
      expect(res.body.alternativeRecommendation).toHaveProperty('recommendationId');
      expect(res.body.alternativeRecommendation).toHaveProperty('restaurantName');
      expect(res.body.alternativeRecommendation).toHaveProperty('representativeMenu');
      expect(res.body).toHaveProperty('message', '이런 건 어때요?');
    });

    it('대체 추천 후보 없음 시 "주변에 더 추천할 곳이 없어요. 거리를 넓혀볼까요?" 메시지를 반환한다', async () => {
      /**
       * 대체 후보 없음 흐름:
       * - hasAlternative: false
       * - alternativeRecommendation: null
       * - noAlternativeMessage: "주변에 더 추천할 곳이 없어요. 거리를 넓혀볼까요?"
       */
      const res = await request(BASE_URL)
        .post('/api/v1/recommendations/rec-550e8400-e29b-41d4-a716-446655440010/reject')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          rejectReason: 'TOO_FAR',
        })
        .expect(200);

      expect(res.body).toHaveProperty('rejected', true);
      expect(res.body).toHaveProperty('hasAlternative', false);
      expect(res.body).toHaveProperty('alternativeRecommendation', null);
      expect(res.body).toHaveProperty('noAlternativeMessage');
      expect(res.body.noAlternativeMessage).toBeTruthy();
    });

  });

  describe('전체 새로고침 (3개 모두 거절 시)', () => {

    it('거절된 추천 ID 목록을 전달하면 기존 캐시를 무효화하고 새로운 추천 3개를 반환한다', async () => {
      /**
       * 새로고침 흐름:
       * - 추천·이력 서비스 → Redis: 기존 캐시 무효화
       * - 추천·이력 서비스: 새 추천 3개 요청 (AI Pipeline 또는 캐시)
       * - 응답 형태: TodayRecommendationsResponse (오늘의 추천 조회와 동일)
       */
      const res = await request(BASE_URL)
        .post('/api/v1/recommendations/refresh')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          rejectedIds: [
            'rec-550e8400-e29b-41d4-a716-446655440010',
            'rec-550e8400-e29b-41d4-a716-446655440011',
            'rec-550e8400-e29b-41d4-a716-446655440012',
          ],
          latitude: 37.5665,
          longitude: 126.9780,
        })
        .expect(200);

      expect(res.body).toHaveProperty('recommendations');
      expect(Array.isArray(res.body.recommendations)).toBe(true);
      expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
      expect(res.body).toHaveProperty('generatedAt');
    });

  });

  describe('식당 길찾기 안내 (UFR-REC-060)', () => {

    it('경로 조회 성공 시 도보 소요시간, 지도 링크, 외부 지도 앱 딥링크를 반환한다', async () => {
      /**
       * 경로 조회 흐름:
       * - 추천·이력 서비스 → 지도 API: 도보 경로 API 요청 {출발지, 목적지}
       * - 지도 API → 도보 경로 + 예상 소요시간 반환
       * - 응답: 도보 시간(분), 지도 링크, 카카오맵/네이버지도 딥링크
       */
      const res = await request(BASE_URL)
        .get('/api/v1/restaurants/rest-001/directions')
        .query({ latitude: 37.5665, longitude: 126.9780 })
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('walkMinutes');
      expect(res.body).toHaveProperty('mapLink');
      expect(res.body).toHaveProperty('deepLinks');
    });

    it('경로 조회 실패 시 식당 주소 텍스트와 "지도 앱에서 검색하기" 버튼 정보를 반환한다', async () => {
      /**
       * 경로 조회 실패 흐름:
       * - 지도 API 오류 발생
       * - 응답: 식당 주소 텍스트, "지도 앱에서 검색하기" 버튼 정보
       * - 서비스 레벨 실패로 전파하지 않고 fallback 정보 제공
       */
      const res = await request(BASE_URL)
        .get('/api/v1/restaurants/rest-001/directions')
        .query({ latitude: 37.5665, longitude: 126.9780 })
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('restaurantAddress');
      expect(res.body).toHaveProperty('searchGuideMessage');
    });

  });

});
