# payment-service 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스명 | payment-service |
| DBMS | PostgreSQL 15 |
| 스키마 | lunchpick_payment |
| 테이블 수 | 2 |
| 캐시 DB | Redis DB 3 (구독 플랜 목록 캐시) |
| 아키텍처 패턴 | Layered |
| 데이터 독립성 | 서비스 전용 독립 DB, 타 서비스 FK 없음 |

---

## 1. 설계 원칙

- **데이터 소유권**: payment-service가 구독·결제 데이터 완전 소유
- **크로스 서비스 조인 금지**: member DB, recommendation DB 직접 접근 불가
- **법적 요구사항 준수**: 전자상거래법에 따른 결제 이력 보존 (5년)
- **이벤트 기반 동기화**: 구독 활성화/취소 시 Redis 이벤트로 타 서비스에 플랜 정보 전파
- **불변 이력**: payment_history는 INSERT ONLY (수정 불가)

---

## 2. 테이블 목록

| 테이블명 | 설명 | 비고 |
|---------|------|------|
| subscription | 구독 정보 | 현재 구독 상태 관리 |
| payment_history | 결제 이력 | INSERT ONLY, 법적 보존 5년 |

---

## 3. 테이블 상세 설계

### 3.1 subscription (구독 정보)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| subscription_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| plan_id | VARCHAR(30) | NOT NULL | - | 플랜 식별자 |
| status | VARCHAR(20) | NOT NULL | 'ACTIVE' | 구독 상태 |
| started_at | TIMESTAMP | NOT NULL | - | 구독 시작일시 |
| next_billing_at | TIMESTAMP | NULL | - | 다음 청구일시 |
| current_period_ends_at | TIMESTAMP | NULL | - | 현재 구독 기간 종료일 |
| trial_extension_used | BOOLEAN | NOT NULL | FALSE | 체험 연장 사용 여부 |
| cancel_reason | VARCHAR(20) | NULL | - | 취소 사유 |
| cancel_reason_detail | VARCHAR(500) | NULL | - | 취소 사유 상세 |
| cancelled_at | TIMESTAMP | NULL | - | 취소 일시 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 수정일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `subscription_id`
- INDEX: `member_id, status`
- INDEX: `status, next_billing_at` (자동 갱신 배치용)

**제약 조건**:
- `plan_id` CHECK: `('FREE', 'PREMIUM_MONTHLY', 'PREMIUM_ANNUAL')`
- `status` CHECK: `('ACTIVE', 'PENDING_CANCEL', 'CANCELLED', 'EXPIRED')`
- `cancel_reason` CHECK: `('COST', 'NOT_USING', 'QUALITY', 'OTHER', NULL)`

---

### 3.2 payment_history (결제 이력)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| payment_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| subscription_id | VARCHAR(36) | NOT NULL | - | 구독 식별자 (논리적 연결) |
| plan_id | VARCHAR(30) | NOT NULL | - | 플랜 식별자 |
| amount | INTEGER | NOT NULL | - | 결제 금액 (원) |
| status | VARCHAR(20) | NOT NULL | 'PENDING' | 결제 상태 |
| pg_transaction_id | VARCHAR(100) | NULL | - | PG사 거래 ID |
| error_code | VARCHAR(50) | NULL | - | 오류 코드 |
| auto_renewal_agreed | BOOLEAN | NOT NULL | FALSE | 자동 갱신 동의 |
| withdrawal_right_acknowledged | BOOLEAN | NOT NULL | FALSE | 청약 철회 권리 인지 |
| withdrawal_deadline | TIMESTAMP | NULL | - | 청약 철회 기한 |
| requested_at | TIMESTAMP | NOT NULL | - | 결제 요청 일시 |
| approved_at | TIMESTAMP | NULL | - | 결제 승인 일시 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `payment_id`
- INDEX: `member_id, requested_at DESC`
- INDEX: `subscription_id`
- INDEX: `status, requested_at` (배치/정산용)

**제약 조건**:
- `status` CHECK: `('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')`
- `amount` CHECK: `(amount >= 0)`
- **INSERT ONLY**: 애플리케이션 레벨에서 UPDATE/DELETE 금지 (법적 보존)

---

## 4. 캐시 설계

### Redis DB 3 — payment-service 전용 캐시

| 캐시 키 패턴 | TTL | 설명 | 무효화 트리거 |
|-------------|-----|------|-------------|
| `plan:list` | 1시간 | 구독 플랜 목록 캐시 | 플랜 변경 시 관리자 수동 무효화 |
| `subscription:active:{member_id}` | 10분 | 활성 구독 정보 캐시 | 구독 상태 변경 시 |
| `payment:lock:{member_id}` | 30초 | 중복 결제 방지 잠금 | 결제 완료/실패 시 |

---

## 5. 데이터 흐름

```
[구독 신청] → payment_history 생성 (status=PENDING)
     ↓
[PG 결제 요청] → PG사 응답
     ↓
[결제 성공] → payment_history status=SUCCESS, subscription 생성
     ↓
[구독 취소 신청] → subscription status=PENDING_CANCEL
     ↓
[기간 만료] → subscription status=CANCELLED
```

---

## 6. 데이터 독립성 준수 사항

- `member_id`는 VARCHAR(36) UUID 값으로만 참조 (FK 없음)
- 구독 상태 변경 시 `SubscriptionEventPublisher`로 Redis 이벤트 발행
- 타 서비스의 구독 상태 조회는 `/internal/payments/subscription/{memberId}` API 사용
- payment_history는 전자상거래법 준수를 위해 5년 보존, 삭제 금지
