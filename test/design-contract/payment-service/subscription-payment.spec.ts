/**
 * 설계 계약 테스트 - 결제 서비스 구독 결제 (UFR-PAY-020)
 *
 * 원본 시퀀스: docs/design/sequence/inner/payment-service-구독결제.puml
 * API 명세:   docs/design/api/payment-service-api.yaml
 *
 * 이 파일은 직접 실행하지 않으며 백엔드 구현 시 행위 참고 자료로 활용된다.
 * alt/else 분기 수 = it() 케이스 수 (1:1 대응)
 * 주의: PG 결제에는 Retry 미적용 (이중결제 방지)
 */

import request from 'supertest';

const BASE_URL = process.env.PAYMENT_SERVICE_URL || 'http://localhost:8083';

describe('결제 서비스 - 구독 결제 (UFR-PAY-020)', () => {

  // ── 공통 픽스처 ────────────────────────────────────────────────────────
  const validPaymentBody = {
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

  const authHeader = { Authorization: 'Bearer valid-jwt-token' };

  // ── 구독 결제 요청 ────────────────────────────────────────────────────
  describe('구독 결제 요청', () => {

    describe('결제 정보 유효성 검증', () => {

      it('검증 실패 시 400과 INVALID_PAYMENT_INFO 코드를 반환한다', async () => {
        const invalidBody = {
          ...validPaymentBody,
          paymentMethod: {
            ...validPaymentBody.paymentMethod,
            cardNumber: 'INVALID-CARD',   // Luhn 알고리즘 실패
            expiryYear: 2020,             // 만료된 유효기간
            cvc: 'XX',                    // CVC 형식 오류
          },
        };

        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions')
          .set(authHeader)
          .send(invalidBody);

        expect(res.status).toBe(400);
        expect(res.body).toHaveProperty('error', 'INVALID_PAYMENT_INFO');
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('timestamp');
      });

      it('검증 통과 시 결제 처리 단계로 진행한다 (중복 확인으로 분기)', async () => {
        // 검증 통과 자체는 400이 아닌 후속 처리 결과(201/402/409)로 확인한다.
        // 이 케이스는 "검증 통과" 경로가 실행되는 것을 보장하는 smoke 역할이다.
        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions')
          .set(authHeader)
          .send(validPaymentBody);

        expect(res.status).not.toBe(400);
      });
    });

    describe('진행 중 결제 중복 확인', () => {

      it('중복 결제 진행 중(PENDING) 시 409와 PAYMENT_IN_PROGRESS 코드를 반환한다', async () => {
        // 사전 조건: 동일 회원에 대해 PENDING 상태 결제가 존재하는 상황을 가정한다.
        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions')
          .set(authHeader)
          .send(validPaymentBody);

        // 중복 진행 중 시나리오에서는 409 Conflict 응답이 와야 한다.
        expect(res.status).toBe(409);
        expect(res.body).toHaveProperty('error');
        expect(res.body).toHaveProperty('message');
        expect(res.body).toHaveProperty('timestamp');
      });

    });
  });

  // ── PG 결제 승인 요청 (Retry 미적용 — 이중결제 방지) ─────────────────
  describe('PG 결제 승인 요청 (Retry 미적용 — 이중결제 방지)', () => {

    describe('Circuit Breaker 상태 확인', () => {

      describe('Circuit Breaker Closed (정상)', () => {

        describe('PG 결제 승인 결과', () => {

          it('결제 승인 성공 시 201과 구독 활성화 정보를 반환한다', async () => {
            const res = await request(BASE_URL)
              .post('/api/v1/subscriptions')
              .set(authHeader)
              .send(validPaymentBody);

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

          describe('결제 승인 실패', () => {

            it('3회 미만 실패 시 402와 PAYMENT_FAILED 코드를 반환한다', async () => {
              // PG 승인 실패 + 연속 실패 횟수 3회 미만 시나리오
              const res = await request(BASE_URL)
                .post('/api/v1/subscriptions')
                .set(authHeader)
                .send(validPaymentBody);

              expect(res.status).toBe(402);
              expect(res.body).toHaveProperty('error', 'PAYMENT_FAILED');
              expect(res.body).toHaveProperty('message', '결제가 실패했어요. 다른 결제 수단을 시도해주세요.');
              expect(res.body).toHaveProperty('timestamp');
            });

            it('3회 연속 실패 시 402와 PAYMENT_FAILED_MAX_RETRY 코드 및 7일 유예 정보를 반환한다', async () => {
              // PG 승인 실패 + 연속 실패 횟수 3회 이상 시나리오
              const res = await request(BASE_URL)
                .post('/api/v1/subscriptions')
                .set(authHeader)
                .send(validPaymentBody);

              expect(res.status).toBe(402);
              expect(res.body).toHaveProperty('error', 'PAYMENT_FAILED_MAX_RETRY');
              expect(res.body).toHaveProperty('message');
              expect(res.body).toHaveProperty('dataGracePeriodDays', 7);
            });

          });
        });

      });

      it('Circuit Breaker Open (PG 장애) 시 503과 PG_UNAVAILABLE 코드를 반환한다', async () => {
        // LLM Circuit Breaker가 Open 상태인 시나리오 (PG 연속 장애로 Open 전환)
        const res = await request(BASE_URL)
          .post('/api/v1/subscriptions')
          .set(authHeader)
          .send(validPaymentBody);

        expect(res.status).toBe(503);
        expect(res.body).toHaveProperty('error', 'PG_UNAVAILABLE');
        expect(res.body).toHaveProperty('message', '결제 서비스가 일시적으로 불가합니다');
        expect(res.body).toHaveProperty('timestamp');
      });

    });
  });

});
