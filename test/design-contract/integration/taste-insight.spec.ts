import request from 'supertest';

/**
 * 설계 계약 테스트: 취향 인사이트 조회 (외부 시퀀스)
 * 원본 설계서: docs/design/sequence/outer/취향인사이트조회.puml
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기가 it() 케이스로 1:1 매핑되어 있다.
 *
 * 서비스 간 호출 흐름:
 *   API Gateway → 추천·이력 서비스 → Redis (인사이트 캐시)
 *                                   → 추천·이력 DB (이력/인사이트 집계)
 */

const BASE_URL = process.env.API_GATEWAY_URL || 'http://localhost:8080';

describe('취향 인사이트 조회', () => {

  describe('식사 이력 타임라인 조회 (UFR-REC-110)', () => {

    it('이력 데이터 존재 시 일별 식사 기록 목록과 카테고리 색상 태그를 반환한다', async () => {
      /**
       * 이력 존재 흐름:
       * - 추천·이력 서비스: 구독 상태 확인 (무료: 30일 제한 / 프리미엄: 무제한)
       * - 추천·이력 서비스 → DB: 식사 이력 조회 (회원ID + 날짜 범위 + 카테고리 태그)
       * - 200 OK {일별 식사 기록 목록, 카테고리 색상 태그}
       */
      const res = await request(BASE_URL)
        .get('/api/v1/history/timeline')
        .query({ startDate: '2026-01-27', endDate: '2026-02-26' })
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('meals');
      expect(Array.isArray(res.body.meals)).toBe(true);
      expect(res.body).toHaveProperty('totalCount');
      expect(res.body.meals.length).toBeGreaterThan(0);
      // 각 이력 항목 필드 검증
      expect(res.body.meals[0]).toHaveProperty('mealId');
      expect(res.body.meals[0]).toHaveProperty('date');
      expect(res.body.meals[0]).toHaveProperty('restaurantName');
      expect(res.body.meals[0]).toHaveProperty('category');
      expect(res.body.meals[0]).toHaveProperty('categoryColor');
      expect(res.body.meals[0]).toHaveProperty('satisfaction');
      expect(res.body.meals[0]).toHaveProperty('recordedAt');
    });

    it('이력 없음 시 빈 목록과 "아직 기록이 없어요. 첫 식사를 기록해보세요!" 안내를 반환한다', async () => {
      /**
       * 이력 없음 흐름:
       * - 200 OK {빈 목록, "아직 기록이 없어요. 첫 식사를 기록해보세요!" 안내}
       */
      const res = await request(BASE_URL)
        .get('/api/v1/history/timeline')
        .set('Authorization', 'Bearer new-member-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('meals');
      expect(Array.isArray(res.body.meals)).toBe(true);
      expect(res.body.meals.length).toBe(0);
      expect(res.body).toHaveProperty('message');
      expect(res.body.message).toBeTruthy();
    });

  });

  describe('무료 사용자 30일 초과 접근 시', () => {

    it('무료 사용자이고 30일 초과 이력 요청 시 403 Forbidden과 프리미엄 업그레이드 안내를 반환한다', async () => {
      /**
       * 접근 제한 흐름:
       * - 추천·이력 서비스: 구독 상태 확인 → 무료 사용자 + 30일 초과 요청
       * - 403 Forbidden {"프리미엄에서 전체 이력을 확인하세요"}
       */
      const res = await request(BASE_URL)
        .get('/api/v1/history/timeline')
        .query({ startDate: '2025-01-01', endDate: '2026-02-26' }) // 30일 초과 범위
        .set('Authorization', 'Bearer free-member-jwt-token')
        .expect(403);

      expect(res.body).toHaveProperty('error', 'PREMIUM_REQUIRED');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
    });

  });

  describe('취향 인사이트 리포트 조회 (UFR-REC-120)', () => {

    it('기록 10건 이상(의미 있는 인사이트) 시 카테고리 분포, 주간 패턴, 만족도 추이, 주간 요약을 반환한다', async () => {
      /**
       * 인사이트 충분 흐름:
       * - 추천·이력 서비스 → DB: 취향 인사이트 집계 (선호 카테고리 Top 5, 주간 패턴, 만족도 변화)
       * - 추천·이력 서비스: 주간 요약 문장 생성 ("이번 주 당신의 점심 패턴")
       * - 추천·이력 서비스: 30끼 마일스톤 달성 확인
       * - 200 OK {카테고리 분포, 주간 패턴, 만족도 추이, 주간 요약}
       */
      const res = await request(BASE_URL)
        .get('/api/v1/insights')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('hasEnoughData', true);
      expect(res.body).toHaveProperty('currentRecordCount');
      expect(res.body).toHaveProperty('requiredRecordCount', 10);
      expect(res.body).toHaveProperty('topCategories');
      expect(res.body).toHaveProperty('weeklyPattern');
      expect(res.body).toHaveProperty('satisfactionTrend');
      expect(res.body).toHaveProperty('weeklySummary');
      expect(res.body).toHaveProperty('milestone');
      expect(Array.isArray(res.body.topCategories)).toBe(true);
      expect(Array.isArray(res.body.weeklyPattern)).toBe(true);
      expect(Array.isArray(res.body.satisfactionTrend)).toBe(true);
      // topCategories 각 항목 필드 검증
      if (res.body.topCategories.length > 0) {
        expect(res.body.topCategories[0]).toHaveProperty('category');
        expect(res.body.topCategories[0]).toHaveProperty('count');
        expect(res.body.topCategories[0]).toHaveProperty('percentage');
        expect(res.body.topCategories[0]).toHaveProperty('color');
      }
    });

    it('기록 10건 미만 시 "10끼 이상 기록하면 취향 인사이트가 열려요!" 안내와 현재 기록 수를 반환한다', async () => {
      /**
       * 인사이트 데이터 부족 흐름:
       * - 200 OK {"10끼 이상 기록하면 취향 인사이트가 열려요!" 안내, 현재 기록 수}
       * - hasEnoughData: false
       */
      const res = await request(BASE_URL)
        .get('/api/v1/insights')
        .set('Authorization', 'Bearer new-member-jwt-token')
        .expect(200);

      expect(res.body).toHaveProperty('hasEnoughData', false);
      expect(res.body).toHaveProperty('currentRecordCount');
      expect(res.body).toHaveProperty('requiredRecordCount', 10);
      expect(res.body).toHaveProperty('message');
      expect(res.body.message).toBeTruthy();
      expect(res.body).toHaveProperty('topCategories');
      expect(res.body.topCategories).toHaveLength(0);
    });

  });

  describe('30끼 마일스톤 달성 알림', () => {

    it('30끼 달성 시 인사이트 응답에 마일스톤 달성 정보와 정확도 향상률을 포함한다', async () => {
      /**
       * 30끼 마일스톤 흐름:
       * - 추천·이력 서비스: 30끼 마일스톤 달성 확인
       * - milestone.achieved = true, 달성 메시지, 정확도 향상률 포함
       * - Frontend에서 축하 메시지 표시
       */
      const res = await request(BASE_URL)
        .get('/api/v1/insights')
        .set('Authorization', 'Bearer test-jwt-token')
        .expect(200);

      // 30끼 달성 시나리오: milestone 필드 검증
      if (res.body.milestone) {
        expect(res.body.milestone).toHaveProperty('achieved', true);
        expect(res.body.milestone).toHaveProperty('count');
        expect(res.body.milestone).toHaveProperty('message');
        expect(res.body.milestone).toHaveProperty('accuracyImprovement');
      }
    });

  });

});
