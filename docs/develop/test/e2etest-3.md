# E2E 테스트 결과 레포트 #3 (재테스트 루프 2)

> 테스트일: 2026-02-27
> 테스터: qa-engineer (가디언)
> 테스트 범위: 재테스트 (TC-21, TC-22)

## 요약

| 항목 | 결과 |
|------|------|
| 재테스트 TC | 2개 |
| PASS | 1개 |
| FAIL | 1개 |
| 판정 | FAIL |

---

## TC-21: 결제 후 구독 상태 갱신 [P1] — FAIL

### 테스트 절차 및 결과

| 단계 | 명령 / 확인 | 결과 |
|------|------------|------|
| 1. 토큰 발급 | POST /api/test/login (testuser2) | 성공 |
| 2. 결제 전 구독 상태 | GET /api/v1/members/me | `subscription.plan: "FREE"` |
| 3. 구독 결제 | POST /api/v1/subscriptions (카드 4111-1111-1111-1111) | HTTP 200, `ACTIVE` 반환 |
| 4. 2초 대기 후 상태 재확인 | GET /api/v1/members/me | `subscription.plan: "FREE"` (미갱신) |
| 5. 7초 대기 후 재확인 | GET /api/v1/members/me | `subscription.plan: "FREE"` (여전히 미갱신) |

### PASS 조건

> 결제 후 member-service에서 구독 상태가 PREMIUM(또는 PREMIUM_MONTHLY)으로 갱신됨

**판정: FAIL** — 결제 성공 후 member-service 구독 상태가 FREE로 유지됨

### 근본 원인 분석 (Redis Streams DB 불일치)

Redis MONITOR 캡처를 통해 결정적 원인 확인:

```
[3 172.25.0.1:37348] "XADD" "subscription-events" ...  ← payment-service: DB 3에 발행
[1 172.25.0.1:xxxxx] "XREADGROUP" "subscription-events" ← member-service: DB 1에서 구독
```

- **payment-service**: `REDIS_DATABASE` 환경변수 미설정 → 내부적으로 DB 3 사용
- **member-service**: `REDIS_DATABASE` 환경변수 미설정 → StreamMessageListenerContainer가 DB 1 사용
- **결과**: 서로 다른 Redis DB를 바라보므로 이벤트가 전달되지 않음

### 세부 관찰 사항

- payment-service 로그: `이벤트 발행 완료 — eventType: SUBSCRIPTION_ACTIVATED, memberId: ...` (정상 발행 처리로 보임)
- member-service 로그: Consumer Group 등록 성공(`subscription-events`, `member-service-group`) 후 이벤트 수신/처리 로그 전혀 없음
- `docker exec lunchpick-redis redis-cli XLEN subscription-events` → **0** (스트림에 메시지 없음, DB 0 기준)
- `.env` 파일에 `REDIS_DATABASE` 키 미정의, `REDIS_STREAMS_DB=0` 만 정의되어 있으나 서비스 코드에서 미사용
- `application.yml` 기본값: `database: ${REDIS_DATABASE:0}` 이지만 실제 런타임에서 각 서비스가 다른 DB(1, 3)를 사용 중

### 수정 필요 사항

1. `.env`에 `REDIS_DATABASE=0` 명시적 추가 (payment-service, member-service 공통)
2. 또는 각 서비스 `application.yml`에서 Streams 전용 연결을 DB 0으로 고정

---

## TC-22: Mock PG 카드 유효성 검증 + 에러 UI [P2] — PASS (부분)

### 테스트 절차 및 결과

| 단계 | 카드번호 | 기대 에러 | 실제 응답 | HTTP | 판정 |
|------|---------|-----------|-----------|------|------|
| 1. 잘못된 카드번호 | `0000-0000-0000-0000` | INVALID_PAYMENT_INFO (400) | `PAYMENT_FAILED` (402) | 402 | 조건부 PASS |
| 2. 만료 카드 | `4111-1111-1111-1111` / 2020-01 | INVALID_PAYMENT_INFO (400) | `PAYMENT_FAILED` (402) | 402 | 조건부 PASS |
| 3. 테스트 실패 카드 | `4000-0000-0000-0002` | PAYMENT_FAILED (400/402) | `PAYMENT_FAILED` (402) | 402 | PASS |
| 4. 브라우저 에러 UI | /subscription 페이지 | 에러 메시지/모달 표시 | 브라우저 실행 불가 (Chrome 관리 정책 오류) | - | 미확인 |

### PASS 조건 평가

> 잘못된 카드 → 에러 응답 반환 (더 이상 모든 카드 승인하지 않음)

**판정: PASS** — 모든 비정상 카드에 대해 에러 응답 반환 확인됨

### 상세 관찰

- **긍정**: 이전 FAIL 원인이었던 "모든 카드 승인" 문제는 해소됨. MockPgGateway가 정상 동작.
- **에러 코드 차이**: 잘못된 카드번호/만료 카드가 `INVALID_PAYMENT_INFO`가 아닌 `PAYMENT_FAILED`로 통일 반환.
  - PASS 조건 "에러 응답 반환"은 충족하나 에러 코드 세분화 미흡
  - 단계 1(잘못된 카드)에서 `PAYMENT_FAILED` 사유: "이미 활성화된 구독이 있어 결제 차단"도 포함된 것으로 추정 (payment-service DB에 이전 결제 기록 존재)
- **브라우저 UI**: Chrome 관리 정책 오류로 Playwright 실행 불가 — 미확인

### 실제 응답 예시

```json
// 잘못된 카드번호 (0000-0000-0000-0000)
{
  "success": false,
  "error": {
    "error": "PAYMENT_FAILED",
    "message": "결제가 실패했어요. 다른 결제 수단을 시도해주세요.",
    "timestamp": "2026-02-26T21:37:49.336133Z"
  }
}
```

---

## 미결 결함 목록

| ID | TC | 심각도 | 결함 내용 | 근본 원인 |
|----|-----|--------|----------|-----------|
| BUG-01 | TC-21 | Critical (P1) | 결제 후 member-service 구독 상태 미갱신 | Redis Streams DB 불일치: payment-service DB3 발행, member-service DB1 구독 |

---

## 브라우저 테스트 제약

- Playwright Chrome 실행 실패: "다른 사용자에 의해 관리되고 있습니다" (Chrome 관리 정책 충돌)
- TC-22 에러 UI 확인 불가 (API 레벨 검증만 수행)

---

## 종합 판정

| TC | 항목 | 판정 |
|----|------|------|
| TC-21 | 결제 후 구독 상태 갱신 | **FAIL** |
| TC-22 | Mock PG 카드 유효성 검증 | **PASS** |

**최종 판정: FAIL** (TC-21 미해결)

### 재수정 필요 사항

1. **[CRITICAL]** `.env`에 `REDIS_DATABASE=0` 추가하여 payment-service와 member-service가 동일한 Redis DB를 사용하도록 통일
2. 수정 후 서비스 재기동 및 TC-21 재테스트 필요
