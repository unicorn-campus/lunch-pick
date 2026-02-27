# payment-service DB 스키마 스크립트

> Database: lunchpick_payment (PostgreSQL 15)
> Schema: lunchpick_payment

```sql
-- ============================================================
-- payment-service 스키마 생성
-- ============================================================

CREATE SCHEMA IF NOT EXISTS lunchpick_payment;
SET search_path = lunchpick_payment;

-- ============================================================
-- TABLE: subscription (구독 정보)
-- ============================================================

CREATE TABLE IF NOT EXISTS subscription (
    id                      BIGSERIAL     PRIMARY KEY,
    subscription_id         VARCHAR(36)   NOT NULL,
    member_id               VARCHAR(36)   NOT NULL,
    plan_id                 VARCHAR(30)   NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    started_at              TIMESTAMP     NOT NULL,
    next_billing_at         TIMESTAMP,
    current_period_ends_at  TIMESTAMP,
    trial_extension_used    BOOLEAN       NOT NULL DEFAULT FALSE,
    cancel_reason           VARCHAR(20),
    cancel_reason_detail    VARCHAR(500),
    cancelled_at            TIMESTAMP,
    created_at              TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_subscription_subscription_id UNIQUE (subscription_id),
    CONSTRAINT chk_subscription_plan_id
        CHECK (plan_id IN ('FREE', 'PREMIUM_MONTHLY', 'PREMIUM_ANNUAL')),
    CONSTRAINT chk_subscription_status
        CHECK (status IN ('ACTIVE', 'PENDING_CANCEL', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_subscription_cancel_reason
        CHECK (cancel_reason IN ('COST', 'NOT_USING', 'QUALITY', 'OTHER') OR cancel_reason IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_subscription_member_status
    ON subscription (member_id, status);

-- 자동 갱신 배치 처리용 인덱스
CREATE INDEX IF NOT EXISTS idx_subscription_status_next_billing
    ON subscription (status, next_billing_at)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE  subscription IS '회원 구독 정보 (현재 상태 관리)';
COMMENT ON COLUMN subscription.subscription_id        IS '도메인 식별자 (UUID)';
COMMENT ON COLUMN subscription.plan_id                IS 'FREE | PREMIUM_MONTHLY | PREMIUM_ANNUAL';
COMMENT ON COLUMN subscription.status                 IS 'ACTIVE | PENDING_CANCEL | CANCELLED | EXPIRED';
COMMENT ON COLUMN subscription.trial_extension_used   IS '무료 체험 기간 연장 사용 여부 (1회 한정)';
COMMENT ON COLUMN subscription.cancel_reason          IS 'COST | NOT_USING | QUALITY | OTHER';
COMMENT ON COLUMN subscription.current_period_ends_at IS '취소 후에도 서비스 이용 가능한 기간 종료일';

-- ============================================================
-- TABLE: payment_history (결제 이력)
-- ============================================================

CREATE TABLE IF NOT EXISTS payment_history (
    id                             BIGSERIAL     PRIMARY KEY,
    payment_id                     VARCHAR(36)   NOT NULL,
    member_id                      VARCHAR(36)   NOT NULL,
    subscription_id                VARCHAR(36)   NOT NULL,
    plan_id                        VARCHAR(30)   NOT NULL,
    amount                         INTEGER       NOT NULL,
    status                         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    pg_transaction_id              VARCHAR(100),
    error_code                     VARCHAR(50),
    auto_renewal_agreed            BOOLEAN       NOT NULL DEFAULT FALSE,
    withdrawal_right_acknowledged  BOOLEAN       NOT NULL DEFAULT FALSE,
    withdrawal_deadline            TIMESTAMP,
    requested_at                   TIMESTAMP     NOT NULL,
    approved_at                    TIMESTAMP,
    created_at                     TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_payment_history_payment_id UNIQUE (payment_id),
    CONSTRAINT chk_payment_history_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_payment_history_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_payment_history_plan_id
        CHECK (plan_id IN ('FREE', 'PREMIUM_MONTHLY', 'PREMIUM_ANNUAL'))
);

CREATE INDEX IF NOT EXISTS idx_payment_history_member_requested
    ON payment_history (member_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_history_subscription_id
    ON payment_history (subscription_id);

-- 정산/배치용 인덱스
CREATE INDEX IF NOT EXISTS idx_payment_history_status_requested
    ON payment_history (status, requested_at);

COMMENT ON TABLE  payment_history IS '결제 이력 (INSERT ONLY - 전자상거래법 5년 보존)';
COMMENT ON COLUMN payment_history.payment_id                     IS '도메인 식별자 (UUID)';
COMMENT ON COLUMN payment_history.pg_transaction_id              IS 'PG사 거래 고유 ID';
COMMENT ON COLUMN payment_history.auto_renewal_agreed            IS '자동 갱신 동의 여부 (법적 필수 기록)';
COMMENT ON COLUMN payment_history.withdrawal_right_acknowledged  IS '청약 철회 권리 인지 여부 (법적 필수 기록)';
COMMENT ON COLUMN payment_history.withdrawal_deadline            IS '청약 철회 가능 기한 (결제 승인 후 7일)';

-- ============================================================
-- INSERT ONLY 보호 정책
-- (애플리케이션 레벨 + DB 레벨 이중 보호)
-- ============================================================

-- payment_history UPDATE 방지 규칙
CREATE OR REPLACE RULE payment_history_no_update AS
    ON UPDATE TO payment_history DO INSTEAD NOTHING;

-- payment_history DELETE 방지 규칙
CREATE OR REPLACE RULE payment_history_no_delete AS
    ON DELETE TO payment_history DO INSTEAD NOTHING;

-- ============================================================
-- 자동 updated_at 갱신 트리거 (subscription만)
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_subscription_updated_at
    BEFORE UPDATE ON subscription
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```
