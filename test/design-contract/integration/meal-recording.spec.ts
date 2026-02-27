import request from 'supertest';

/**
 * 설계 계약 테스트: 식사 기록 (외부 시퀀스)
 * 원본 설계서: docs/design/sequence/outer/식사기록.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 서비스 간 호출 흐름:
 *   API Gateway → 추천·이력 서비스 → 추천·이력 DB (식사 기록 저장)
 *                                   → Redis (추천 캐시 무효화)
 */

const BASE_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';

describe('식사 기록', () => {

  describe('식사 완료 원탭 기록 (UFR-REC-070)', () => {

    it('기록 정상 처리 시 식사 기록을 저장하고 추천 캐시를 무효화한 후 201 Created를 반환한다', async () => {
      /**
       * 정상 처리 흐름:
       * - 추천·이력 서비스: 식사 시간대(10:30~15:00) 합리성 확인
       * - 추천·이력 서비스 → DB: 오늘 동일 식사 중복 확인 → 중복 없음 확인
       * - 추천·이력 서비스 → DB: 식사 기록 저장 {회원ID, 식당ID, 식사시각, 수락된 추천ID}
       * - 추천·이력 서비스 → Redis: 해당 사용자 추천 캐시 무효화 (rec:{user_id}:*)
       * - 응답: 201 Created {"오늘 점심 기록 완료!"}
       */
      const res = await request(BASE_URL)
        .post('/api/v1/meals')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          recommendationId: 'rec-550e8400-e29b-41d4-a716-446655440010',
          restaurantId: 'rest-001',
          menuName: '된장찌개 정식',
          recordedAt: '2026-02-26T12:30:00Z',
        })
        .expect(201);

      expect(res.body).toHaveProperty('mealId');
      expect(res.body).toHaveProperty('restaurantName');
      expect(res.body).toHaveProperty('menuName');
      expect(res.body).toHaveProperty('recordedAt');
      expect(res.body).toHaveProperty('message', '오늘 점심 기록 완료!');
    });

    it('중복 기록 시 409 Conflict와 "이미 기록되었어요. 수정하시겠어요?" 안내를 반환한다', async () => {
      /**
       * 중복 기록 흐름:
       * - 추천·이력 서비스 → DB: 중복 기록 감지
       * - 409 Conflict 반환
       */
      const res = await request(BASE_URL)
        .post('/api/v1/meals')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          recommendationId: 'rec-550e8400-e29b-41d4-a716-446655440010',
          restaurantId: 'rest-001',
          menuName: '된장찌개 정식',
          recordedAt: '2026-02-26T12:30:00Z',
        })
        .expect(409);

      expect(res.body).toHaveProperty('error', 'DUPLICATE_MEAL_RECORD');
      expect(res.body).toHaveProperty('message', '이미 기록되었어요. 수정하시겠어요?');
      expect(res.body).toHaveProperty('timestamp');
    });

    it('식사 시간대 외 기록 시도 시 400 Bad Request와 유효하지 않은 식사 시간대 안내를 반환한다', async () => {
      /**
       * 식사 시간대 외 흐름:
       * - 추천·이력 서비스: 식사 시간대(10:30~15:00) 확인 → 범위 이탈
       * - 400 Bad Request {유효하지 않은 식사 시간대}
       */
      const res = await request(BASE_URL)
        .post('/api/v1/meals')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          recommendationId: 'rec-550e8400-e29b-41d4-a716-446655440010',
          restaurantId: 'rest-001',
          menuName: '된장찌개 정식',
          recordedAt: '2026-02-26T09:00:00Z', // 10:30 이전
        })
        .expect(400);

      expect(res.body).toHaveProperty('error', 'INVALID_MEAL_TIME');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
    });

  });

  describe('식사 기록 빠른 수정 (UFR-REC-080)', () => {

    it('30초 이내 취소 시 식사 기록을 삭제하고 200 OK를 반환한다', async () => {
      /**
       * 30초 이내 취소 흐름:
       * - 추천·이력 서비스: 30초 경과 여부 확인 → 이내
       * - 추천·이력 서비스 → DB: 식사 기록 삭제
       * - 추천·이력 서비스 → Redis: 추천 캐시 복원 (무효화 취소 불가 — 새 추천 요청 시 재생성)
       * - 200 OK {취소 완료}
       */
      const res = await request(BASE_URL)
        .delete('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('message');
    });

    it('30초 초과 시 409 Conflict와 "이력 화면에서 수정할 수 있어요" 안내를 반환한다', async () => {
      /**
       * 30초 초과 흐름:
       * - 추천·이력 서비스: 30초 경과 여부 확인 → 초과
       * - 409 Conflict {"이력 화면에서 수정할 수 있어요"}
       */
      const res = await request(BASE_URL)
        .delete('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(409);

      expect(res.body).toHaveProperty('error', 'CANCEL_TIMEOUT');
      expect(res.body).toHaveProperty('message', '이력 화면에서 수정할 수 있어요.');
      expect(res.body).toHaveProperty('timestamp');
    });

  });

  describe('간단 피드백 제출 (UFR-REC-090)', () => {

    it('피드백 제출 시 만족도와 키워드를 저장하고 누적 피드백 횟수와 반영 메시지를 반환한다', async () => {
      /**
       * 피드백 제출 흐름:
       * - 사용자: 만족도 선택(좋아요/별로) + 키워드 선택(맛/양/속도, 선택)
       * - 추천·이력 서비스 → DB: 피드백 저장 {만족도, 키워드, 제출시각}
       * - 응답: 200 OK {"내일 추천에 반영할게요", 누적 피드백 횟수}
       */
      const res = await request(BASE_URL)
        .post('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020/feedback')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          satisfaction: 'GOOD',
          keyword: 'TASTE',
        })
        .expect(200);

      expect(res.body).toHaveProperty('message', '피드백 감사해요!');
      expect(res.body).toHaveProperty('reflectionMessage', '내일 추천에 반영할게요.');
      expect(res.body).toHaveProperty('totalFeedbackCount');
      expect(typeof res.body.totalFeedbackCount).toBe('number');
    });

    it('피드백 스킵 시 중립(NEUTRAL) 기본값으로 저장하고 200 OK를 반환한다', async () => {
      /**
       * 피드백 스킵 흐름:
       * - 사용자: 피드백 화면 닫기(스킵)
       * - Frontend → API: POST /api/v1/meals/{meal_id}/feedback {만족도: 중립(기본값)}
       * - 추천·이력 서비스 → DB: 중립 피드백 저장
       * - 200 OK
       */
      const res = await request(BASE_URL)
        .post('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020/feedback')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          satisfaction: 'NEUTRAL',
          keyword: null,
        })
        .expect(200);

      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('reflectionMessage');
      expect(res.body).toHaveProperty('totalFeedbackCount');
    });

  });

});
