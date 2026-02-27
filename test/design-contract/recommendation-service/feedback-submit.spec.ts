import request from 'supertest';

/**
 * 설계 계약 테스트: 추천·이력 서비스 - 피드백 제출 (UFR-REC-090)
 * 원본 설계서: docs/design/sequence/inner/recommendation-service-피드백제출.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 */

const BASE_URL = process.env.RECOMMENDATION_SERVICE_URL || 'http://localhost:8082';

describe('추천·이력 서비스 - 피드백 제출 (UFR-REC-090)', () => {

  describe('피드백 제출', () => {

    it('소유자 불일치 시 MEAL_NOT_OWNED ForbiddenException을 반환한다', async () => {
      // note: ForbiddenException {code: MEAL_NOT_OWNED}
      // note: MealRepository에서 mealId + memberId로 소유자 검증
      const res = await request(BASE_URL)
        .post('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020/feedback')
        .set('Authorization', 'Bearer other-member-jwt-token')
        .send({
          satisfaction: 'GOOD',
          keyword: 'TASTE',
        })
        .expect(403);

      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
    });

    describe('소유자 확인 완료', () => {

      it('이미 피드백 제출됨 시 기존 피드백을 수정하고 완료 메시지를 반환한다', async () => {
        // note: UPDATE 피드백 {satisfaction, keyword, 수정 시각}
        // note: 피드백 수정도 동일 엔드포인트로 처리 (Upsert 의미)
        const res = await request(BASE_URL)
          .post('/api/v1/meals/meal-550e8400-e29b-41d4-a716-446655440020/feedback')
          .set('Authorization', 'Bearer test-jwt-token')
          .send({
            satisfaction: 'BAD',
            keyword: 'PORTION',
          })
          .expect(200);

        expect(res.body).toHaveProperty('message', '피드백 감사해요!');
        expect(res.body).toHaveProperty('reflectionMessage', '내일 추천에 반영할게요.');
        expect(res.body).toHaveProperty('totalFeedbackCount');
      });

      it('신규 피드백 시 피드백을 저장하고 누적 횟수와 반영 메시지를 반환한다', async () => {
        // note: INSERT 피드백 {feedbackId, memberId, mealId, satisfaction, keyword, 제출 시각}
        // note: 응답 = FeedbackResult {message: "피드백 감사해요!", totalFeedbackCount: N, "내일 추천에 반영할게요"}
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

    });

  });

  describe('피드백 스킵 시 기본값 처리', () => {

    it('피드백 스킵 시 중립(NEUTRAL) 피드백을 저장하고 skipped: true를 반환한다', async () => {
      // note: INSERT 피드백 {feedbackId, memberId, mealId, satisfaction: NEUTRAL, skipped: true}
      // note: 응답 = DefaultFeedbackResult {skipped: true}
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
