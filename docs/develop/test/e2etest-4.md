# E2E 테스트 결과 레포트 #4 (재테스트 루프 3 — 최종 검증)

> 테스트일: 2026-02-27
> 테스터: qa-engineer (가디언)
> 테스트 범위: TC-21 최종 검증, TC-22 재확인

## 요약

| 항목 | 결과 |
|------|------|
| 재테스트 TC | 2개 |
| PASS | 2개 |
| FAIL | 0개 |
| 판정 | **PASS** |

---

## TC-21: 결제 후 구독 상태 갱신 [P1] — PASS

### 테스트 절차 및 결과

| 단계 | 명령 / 확인 | 결과 |
|------|------------|------|
| 1. 토큰 발급 | POST /api/test/login (qa4user / qa4@test.com) | HTTP 200, accessToken 발급 |
| 2. 결제 전 구독 상태 | GET /api/v1/members/me | `subscription.plan: "FREE"` |
| 3. 구독 결제 | POST /api/v1/subscriptions (카드 1234-5678-9012-3456) | HTTP 201, `status: ACTIVE`, `amount: 4900` |
| 4. 2초 대기 | sleep 2 | — |
| 5. 결제 후 구독 상태 | GET /api/v1/members/me | `subscription.plan: "PREMIUM_MONTHLY"` |

### PASS 조건 평가

| 조건 | 기대값 | 실제값 | 판정 |
|------|--------|--------|------|
| 결제 전 플랜 | `FREE` | `FREE` | PASS |
| 결제 응답 status | `ACTIVE` | `ACTIVE` | PASS |
| 결제 응답 amount | `4900` | `4900` | PASS |
| 결제 후 플랜 | `PREMIUM_MONTHLY` (FREE 아님) | `PREMIUM_MONTHLY` | PASS |

**판정: PASS** — 결제 성공 후 2초 이내 member-service 구독 상태가 PREMIUM_MONTHLY로 갱신됨

### 증거: 요청 및 응답

#### Step 1 — 테스트 로그인

요청:
```
POST http://localhost:8081/api/test/login
Content-Type: application/json
{"nickname":"qa4user","email":"qa4@test.com"}
```

응답:
```json
{
  "memberId": "617cd0a3-ffca-43db-91b8-658b2c5e1e70",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2MTdjZDBhMy1mZmNhLTQzZGItOTFiOC02NThiMmM1ZTFlNzAi..."
}
```

#### Step 2 — 결제 전 상태 확인

요청:
```
GET http://localhost:8081/api/v1/members/me
Authorization: Bearer {token}
```

응답:
```json
{
  "success": true,
  "data": {
    "memberId": "617cd0a3-ffca-43db-91b8-658b2c5e1e70",
    "nickname": "qa4user",
    "email": "qa4user@test.com",
    "subscription": {
      "plan": "FREE",
      "historyLimitDays": 7,
      "expiresAt": null
    }
  }
}
```

#### Step 3 — 구독 결제 요청

요청:
```
POST http://localhost:8083/api/v1/subscriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "planId": "PREMIUM_MONTHLY",
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "cardNumber": "1234-5678-9012-3456",
    "expiryMonth": 12,
    "expiryYear": 2028,
    "cvc": "123",
    "cardholderName": "QA USER"
  },
  "autoRenewalAgreed": true,
  "withdrawalRightAcknowledged": true
}
```

응답 (HTTP 201):
```json
{
  "success": true,
  "data": {
    "subscriptionId": "1bec1ad5-cf32-4092-9b79-0e9cae4694ff",
    "planId": "PREMIUM_MONTHLY",
    "status": "ACTIVE",
    "startedAt": "2026-02-27T07:07:55.2679017",
    "nextBillingAt": "2026-03-27T07:07:55.2679017",
    "amount": 4900,
    "transactionId": "pg-txn-f4ee548c",
    "withdrawalDeadline": "2026-03-06T07:07:55.2679017"
  },
  "error": null
}
```

#### Step 5 — 결제 후 상태 확인 (2초 대기 후)

요청:
```
GET http://localhost:8081/api/v1/members/me
Authorization: Bearer {token}
```

응답:
```json
{
  "success": true,
  "data": {
    "memberId": "617cd0a3-ffca-43db-91b8-658b2c5e1e70",
    "nickname": "qa4user",
    "subscription": {
      "plan": "PREMIUM_MONTHLY",
      "historyLimitDays": 7,
      "expiresAt": "2026-03-27T07:07:55.2679017"
    }
  }
}
```

---

## TC-22: Mock PG 카드 유효성 검증 [P2] — PASS

### 테스트 절차 및 결과

| 단계 | 카드번호 | 기대 결과 | 실제 HTTP | 실제 에러 코드 | 판정 |
|------|---------|-----------|-----------|--------------|------|
| 1. 잘못된 카드 | `0000-0000-0000-0000` | 에러 응답 반환 | 402 | `PAYMENT_FAILED` | PASS |

### PASS 조건 평가

| 조건 | 기대값 | 실제값 | 판정 |
|------|--------|--------|------|
| 에러 응답 반환 | 비정상 카드 → 실패 응답 | HTTP 402, `PAYMENT_FAILED` | PASS |

**판정: PASS** — 잘못된 카드에 대해 에러 응답 반환 확인

### 증거: 요청 및 응답

요청:
```
POST http://localhost:8083/api/v1/subscriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "planId": "PREMIUM_MONTHLY",
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "cardNumber": "0000-0000-0000-0000",
    "expiryMonth": 12,
    "expiryYear": 2028,
    "cvc": "123",
    "cardholderName": "QA USER"
  },
  "autoRenewalAgreed": true,
  "withdrawalRightAcknowledged": true
}
```

응답 (HTTP 402):
```json
{
  "success": false,
  "data": null,
  "error": {
    "error": "PAYMENT_FAILED",
    "message": "결제가 실패했어요. 다른 결제 수단을 시도해주세요.",
    "timestamp": "2026-02-26T22:08:31.139631900Z"
  }
}
```

---

## BUG-01 해소 확인

| 항목 | 이전 상태 (e2etest-3) | 현재 상태 (e2etest-4) |
|------|----------------------|----------------------|
| BUG-01 | OPEN — Redis Streams DB 불일치로 이벤트 미전달 | **CLOSED** — 결제 후 2초 이내 PREMIUM_MONTHLY 갱신 확인 |

### 이전 근본 원인 (해소됨)

이전 테스트(#3)에서 payment-service가 Redis DB 3에 이벤트를 발행하고 member-service는 DB 1에서 구독하여 이벤트가 전달되지 않는 문제가 확인됐다. 이번 테스트(#4) 시점에는 동일 Redis DB를 바라보도록 수정이 완료된 것으로 판단된다.

---

## 종합 판정

| TC | 항목 | 이전 판정 | 최종 판정 |
|----|------|----------|----------|
| TC-21 | 결제 후 구독 상태 갱신 | FAIL | **PASS** |
| TC-22 | Mock PG 카드 유효성 검증 | PASS | **PASS** |

**최종 판정: PASS** — TC-21, TC-22 모두 PASS. 미결 결함 없음.
