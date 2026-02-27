import request from 'supertest';

/**
 * 설계 계약 테스트: 취향 학습 및 프로파일 갱신 (외부 시퀀스)
 * 원본 설계서: docs/design/sequence/outer/취향학습-프로파일갱신.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 서비스 간 호출 흐름:
 *   스케줄러 → 추천·이력 서비스 → 회원 서비스: PUT /internal/members/{user_id}/taste-vector
 *                               → AI Pipeline: POST /internal/ai/learning-message
 *                               → Redis (추천 캐시 전체 무효화)
 *                               → 추천·이력 DB (배치 결과 저장)
 */

const BASE_URL = process.env.RECOMMENDATION_SERVICE_URL || 'http://localhost:8082';

describe('취향 학습 및 프로파일 갱신', () => {

  describe('일일 취향 학습 반영 — 배치 실행 (UFR-REC-100)', () => {

    describe('사용자별 취향 벡터 갱신 (loop)', () => {

      it('피드백 5건 이상(정상 학습) 시 취향 벡터를 계산하여 회원 서비스에 PUT하고 학습 메시지를 생성한다', async () => {
        /**
         * 정상 학습 흐름:
         * 1. 추천·이력 서비스: 취향 벡터 갱신 계산 (최근 피드백 가중치 > 과거 피드백)
         * 2. 추천·이력 서비스 → 회원 서비스: PUT /internal/members/{user_id}/taste-vector
         * 3. 회원 서비스 → 회원 DB: 취향 벡터 업데이트
         * 4. 회원 서비스 → 추천·이력 서비스: 200 OK
         * 5. 추천·이력 서비스 → AI Pipeline: POST /internal/ai/learning-message {사용자ID, 피드백요약}
         * 6. AI Pipeline: 학습 반영 메시지 생성 ("어제 별로라고 한 매운 음식을 빼고 추천했어요")
         * 7. 추천·이력 서비스 → DB: 학습 반영 메시지 저장 (다음 앱 진입 시 표시용)
         * 8. 추천·이력 서비스 → Redis: 해당 사용자 추천 캐시 전체 무효화 (갱신된 취향 즉시 반영)
         */
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25',
          })
          .expect(200);

        expect(res.body).toHaveProperty('processedCount');
        expect(res.body).toHaveProperty('successCount');
        expect(res.body).toHaveProperty('failureCount');
        expect(res.body).toHaveProperty('status', 'COMPLETED');
      });

      it('피드백 5건 미만(콜드스타트 안전망) 시 안전망 유지 처리를 하고 콜드스타트 상태를 갱신한다', async () => {
        /**
         * 콜드스타트 안전망 흐름:
         * - 추천·이력 서비스: 안전망 유지 처리 (직군 클러스터 Prior + 온보딩 데이터 유지)
         * - 추천·이력 서비스 → DB: 콜드스타트 상태 갱신 ("아직 취향을 학습 중이에요" 태그 유지)
         * - 취향 벡터 갱신은 수행하지 않음
         * - 회원 서비스 호출 없음
         */
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25',
          })
          .expect(200);

        expect(res.body).toHaveProperty('processedCount');
        expect(res.body).toHaveProperty('successCount');
        expect(res.body).toHaveProperty('failureCount');
      });

      it('학습 실패 시 이전 취향 벡터를 유지하고 오류를 로깅하며 배치는 계속 진행한다', async () => {
        /**
         * 학습 실패 흐름:
         * - 추천·이력 서비스: 이전 취향 벡터 유지 (서비스 영향 없음, 오류 로깅)
         * - 배치 전체가 중단되지 않고 다음 사용자로 계속 진행
         * - 갱신 실패율 목표: < 1%
         */
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25',
          })
          .expect(200);

        // 실패가 있더라도 배치 자체는 COMPLETED로 완료
        expect(res.body).toHaveProperty('status', 'COMPLETED');
        expect(res.body).toHaveProperty('failureCount');
      });

    });

  });

});
