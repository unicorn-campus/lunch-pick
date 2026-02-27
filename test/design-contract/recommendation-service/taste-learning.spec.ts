import request from 'supertest';

/**
 * 설계 계약 테스트: 추천·이력 서비스 - 취향 학습 (UFR-REC-100)
 * 원본 설계서: docs/design/sequence/inner/recommendation-service-취향학습.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 취향 학습은 매일 03:00 배치로 실행된다.
 * BatchScheduler → TasteLearningService → 회원 서비스(E) / AI Pipeline(E) 호출 포함.
 */

const BASE_URL = process.env.RECOMMENDATION_SERVICE_URL || 'http://localhost:8082';

describe('추천·이력 서비스 - 취향 학습 (UFR-REC-100)', () => {

  describe('매일 03:00 배치 실행', () => {

    describe('사용자별 취향 벡터 갱신 (loop)', () => {

      it('누적 피드백 5건 이상(정상 학습) 시 취향 벡터를 갱신하고 회원 서비스에 반영한다', async () => {
        /**
         * 정상 학습 흐름:
         * 1. 최근 피드백 이력 조회 (최근 가중치 > 과거 가중치)
         * 2. VectorCalculator: 시간 가중치 적용 → 카테고리별 선호도 재계산 → 수락률/만족 비율 반영
         * 3. 회원 서비스 PUT /internal/members/{memberId}/taste-vector 호출 → 200 OK
         * 4. AI Pipeline POST /internal/ai/learning-message 호출 → 학습 반영 메시지 수신
         * 5. 학습 반영 메시지 DB 저장 (다음 앱 진입 시 표시용)
         * 6. Redis 추천 캐시 전체 무효화 (rec:{memberId}:*)
         */
        // 배치 트리거 엔드포인트가 있다고 가정 (내부 관리 API)
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25', // 전일 피드백 대상
          })
          .expect(200);

        expect(res.body).toHaveProperty('processedCount');
        expect(res.body).toHaveProperty('successCount');
        expect(res.body).toHaveProperty('failureCount');
      });

      it('누적 피드백 5건 미만(콜드스타트 안전망) 시 콜드스타트 상태를 갱신한다', async () => {
        /**
         * 콜드스타트 안전망 유지:
         * - 직군 클러스터 Prior + 온보딩 데이터 유지
         * - DB UPDATE 취향 학습 상태 {memberId, coldStart: true}
         * - 취향 벡터 갱신은 수행하지 않음
         */
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25',
          })
          .expect(200);

        // 배치 결과에 콜드스타트 처리 사용자 수가 포함되어야 함
        expect(res.body).toHaveProperty('processedCount');
        expect(res.body).toHaveProperty('successCount');
        expect(res.body).toHaveProperty('failureCount');
      });

      it('취향 벡터 갱신 실패 시 이전 취향 벡터를 유지하고 오류를 로깅한다', async () => {
        /**
         * 갱신 실패 처리:
         * - 이전 취향 벡터 그대로 유지 (서비스 영향 없음)
         * - 오류 로깅만 수행
         * - 배치 전체가 중단되지 않고 다음 사용자로 계속 진행
         */
        const res = await request(BASE_URL)
          .post('/internal/batch/taste-learning')
          .set('Authorization', 'Bearer internal-service-token')
          .send({
            targetDate: '2026-02-25',
          })
          .expect(200);

        // 실패가 있어도 배치 자체는 200으로 완료되어야 함
        expect(res.body).toHaveProperty('failureCount');
        // 배치 로그에 완료 상태 기록
        expect(res.body).toHaveProperty('batchLogId');
        expect(res.body).toHaveProperty('status', 'COMPLETED');
      });

    });

  });

});
