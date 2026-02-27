/**
 * 설계 계약 테스트 - 구독 전환 (외부 시퀀스)
 *
 * 원본 시퀀스: docs/design/sequence/outer/구독전환.puml
 * API 명세:   docs/design/api/payment-service-api.yaml
 *
 * 이 파일은 직접 실행하지 않으며 백엔드 구현 시 행위 참고 자료로 활용된다.
 * 커버 범위: UFR-PAY-010 (구독 플랜 조회) / UFR-PAY-020 (구독 결제) / UFR-PAY-030 (구독 해지)
 */

import request from 'supertest';

const BASE_URL = process.env.PAYMENT_SERVICE_URL || 'http://localhost:8083';

describe('구독 전환', () => {

  const authHeader = { Authorization: 'Bearer valid-jwt-token' };

  const validSubscriptionBody = {
    planId: 'PREMIUM_MONTHLY',
    paymentMethod: {
      type: 'CREDIT_CARD',
      cardNumber: '1234-5678-9012-3456',
      expiryMonth: 12,
      expiryYear: 2028,
      cvc: '123',
      cardholderName: '김성한',
    },
    autoRenewalAgreed: true,
    withdrawalRightAcknowledged: true,
  };

  const subscriptionId = 'sub-550e8400-e29b-41d4-a716-446655440050';

  // ── 구독 플랜 조회 (UFR-PAY-010) ──────────────────────────────────────
  describe('구독 플랜 조회 (UFR-PAY-010)', () => {

    describe('플랜 정보 캐시 조회', () => {

      it('캐시 히트 시 200과 캐시된 플랜 정보를 즉시 반환한다', async () => {
        // Redis 캐시(plan:list, TTL:1시간)에 데이터가 존재하는 시나리오
        const res = await request(BASE_URL)
          .get('/api/v1/subscriptions/plans')
          .set(authHeader);

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('plans');
        expect(res.body).toHaveProperty('currentPlan');
        expect(Array.isArray(res.body.plans)).toBe(true);
      });

      it('캐시 미스 시 DB에서 조회 후 캐싱하고 200과 플랜 정보를 반환한다', async () => {
        // 캐시 없음 → DB 조회 → Redis 캐싱(TTL:1시간) → 응답
        const res = await request(BASE_URL)
          .get('/api/v1/subscriptions/plans')
          .set(authHeader);

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('plans');
        expect(res.body).toHaveProperty('currentPlan');
        expect(res.body).toHaveProperty('promotionMessage');
        expect(Array.isArray(res.body.plans)).toBe(true);
      });

    });

    it('30일 제한 도달 시 promotionMessage 필드에 전환 트리거 메시지를 포함한다', async () => {
      // 30일 기억 제한 도달 사용자: promotionMessage 비null
      const res = await request(BASE_URL)
        .get('/api/v1/subscriptions/plans')
        .set(authHeader);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('promotionMessage');
      expect(res.body.promotionMessage).not.toBeNull();
    });

  });

  // ── 구독 결제 (UFR-PAY-020) ───────────────────────────────────────────
  describe('구독 결제 (UFR-PAY-020)', () => {

    it('결제 승인 성공 시 201과 프리미엄 활성화 정보를 반환한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/subscriptions')
        .set(authHeader)
        .send(validSubscriptionBody);

      expect(res.status).toBe(201);
      expect(res.body).toHaveProperty('subscriptionId');
      expect(res.body).toHaveProperty('planId');
      expect(res.body).toHaveProperty('status', 'ACTIVE');
      expect(res.body).toHaveProperty('startedAt');
      expect(res.body).toHaveProperty('nextBillingAt');
      expect(res.body).toHaveProperty('amount');
      expect(res.body).toHaveProperty('transactionId');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('withdrawalDeadline');
    });

    it('결제 승인 실패 시 402와 결제 실패 안내 메시지를 반환한다', async () => {
      // PG 승인 실패 시나리오
      const res = await request(BASE_URL)
        .post('/api/v1/subscriptions')
        .set(authHeader)
        .send(validSubscriptionBody);

      expect(res.status).toBe(402);
      expect(res.body).toHaveProperty('error', 'PAYMENT_FAILED');
      expect(res.body).toHaveProperty('message', '결제가 실패했어요. 다른 결제 수단을 시도해주세요.');
      expect(res.body).toHaveProperty('timestamp');
    });

    it('3회 연속 실패 시 402와 고객 지원 연결 안내 및 7일 유예 정보를 반환한다', async () => {
      // 3회 연속 결제 실패 시나리오: dataGracePeriodDays=7 포함
      const res = await request(BASE_URL)
        .post('/api/v1/subscriptions')
        .set(authHeader)
        .send(validSubscriptionBody);

      expect(res.status).toBe(402);
      expect(res.body).toHaveProperty('error', 'PAYMENT_FAILED_MAX_RETRY');
      expect(res.body).toHaveProperty('dataGracePeriodDays', 7);
    });

  });

  // ── 구독 해지 (UFR-PAY-030) ───────────────────────────────────────────
  describe('구독 해지 (UFR-PAY-030)', () => {

    it('7일 무료 연장 수락 시 200과 연장된 만료일을 반환한다', async () => {
      // 해지 화면 진입 → 7일 무료 연장 수락 경로
      const res = await request(BASE_URL)
        .post('/api/v1/subscriptions/extend-trial')
        .set(authHeader);

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('newExpiresAt');
    });

    it('해지 진행 시 200과 해지 예약 완료 정보(남은 기간, 무료 전환 예정일)를 반환한다', async () => {
      // 해지 확인 + 해지 사유 선택 후 해지 예약 경로
      const res = await request(BASE_URL)
        .delete(`/api/v1/subscriptions/${subscriptionId}`)
        .set(authHeader)
        .send({ cancelReason: 'COST', cancelReasonDetail: '가격이 부담돼요.' });

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('subscriptionId');
      expect(res.body).toHaveProperty('status', 'PENDING_CANCEL');
      expect(res.body).toHaveProperty('currentPeriodEndsAt');
      expect(res.body).toHaveProperty('freePlanStartsAt');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('dataWarningMessage');
    });

  });

});
