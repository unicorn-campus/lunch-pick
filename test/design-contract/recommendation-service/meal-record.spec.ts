import request from 'supertest';

/**
 * 설계 계약 테스트: 추천·이력 서비스 - 식사 기록 (UFR-REC-070, UFR-REC-080)
 * 원본 설계서: docs/design/sequence/inner/recommendation-service-식사기록.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 */

const BASE_URL = process.env.RECOMMENDATION_SERVICE_URL || 'http://localhost:8082';

describe('추천·이력 서비스 - 식사 기록 (UFR-REC-070)', () => {

  describe('식사 시간대 유효성 검증', () => {

    it('식사 시간대 외(10:30~15:00 범위 이탈) 시 INVALID_MEAL_TIME 예외를 반환한다', async () => {
      // note: 식사 가능 시간대 = 10:30 ~ 15:00
      // note: 범위 외 요청 시 ValidationException {code: INVALID_MEAL_TIME}
      const res = await request(BASE_URL)
        .post('/api/v1/meals')
        .set('Authorization', 'Bearer test-jwt-token')
        .send({
          recommendationId: 'rec-550e8400-e29b-41d4-a716-446655440010',
          restaurantId: 'rest-001',
          menuName: '된장찌개 정식',
          recordedAt: '2026-02-26T09:00:00Z', // 10:30 이전 — 유효하지 않은 시간대
        })
        .expect(400);

      expect(res.body).toHaveProperty('error', 'INVALID_MEAL_TIME');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
    });

    describe('식사 시간대 정상 — 중복 기록 방지(멱등성 체크)', () => {

      it('이미 기록 존재 시 DUPLICATE_MEAL 예외와 수정 안내 메시지를 반환한다', async () => {
        // note: ConflictException {code: DUPLICATE_MEAL, message: "이미 기록되었어요. 수정하시겠어요?"}
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

      it('신규 기록 가능 시 식사 기록을 저장하고 추천 캐시를 무효화한 후 mealId를 반환한다', async () => {
        // note: DB에 INSERT 후 Redis 캐시 rec:{memberId}:* 패턴 삭제
        // note: 응답 = MealRecordResult {mealId, message: "오늘 점심 기록 완료!"}
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
        expect(res.body).toHaveProperty('recordedAt');
        expect(res.body).toHaveProperty('message', '오늘 점심 기록 완료!');
      });

    });

  });

});

describe('추천·이력 서비스 - 식사 기록 취소 (UFR-REC-080, 30초 이내)', () => {

  it('30초 이내 취소 시 식사 기록을 삭제하고 cancelled: true를 반환한다', async () => {
    // note: 기록 삭제 후 캐시 무효화 취소 불가 — 새 추천 요청 시 재생성
    const res = await request(BASE_URL)
      .delete('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020')
      .set('Authorization', 'Bearer test-jwt-token')
      .expect(200);

    expect(res.body).toHaveProperty('message');
  });

  it('30초 초과 시 CANCEL_TIMEOUT 예외와 이력 화면 수정 안내를 반환한다', async () => {
    // note: ConflictException {code: CANCEL_TIMEOUT, message: "이력 화면에서 수정할 수 있어요"}
    const res = await request(BASE_URL)
      .delete('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020')
      .set('Authorization', 'Bearer test-jwt-token')
      .expect(409);

    expect(res.body).toHaveProperty('error', 'CANCEL_TIMEOUT');
    expect(res.body).toHaveProperty('message', '이력 화면에서 수정할 수 있어요.');
    expect(res.body).toHaveProperty('timestamp');
  });

});
