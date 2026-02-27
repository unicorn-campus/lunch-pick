-- payment-service 테스트 데이터 seed
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실행 DB: payment (jdbc:postgresql://localhost:15432/payment)
-- 주의: SubscriptionEntity는 schema = "lunchpick_payment" 사용

-- 스키마 생성 (없으면 생성)
CREATE SCHEMA IF NOT EXISTS lunchpick_payment;

-- 테이블 초기화
TRUNCATE TABLE lunchpick_payment.payment_history RESTART IDENTITY CASCADE;
TRUNCATE TABLE lunchpick_payment.subscription RESTART IDENTITY CASCADE;

-- 테스트 구독 데이터 (ACTIVE 상태 — cancel/extend-trial 테스트용)
INSERT INTO lunchpick_payment.subscription (subscription_id, member_id, plan_id, status, started_at, next_billing_at, current_period_ends_at, trial_extension_used, created_at, updated_at)
VALUES (
    'sub-uuid-0001',
    'test-member-uuid-0001',
    'PREMIUM_MONTHLY',
    'ACTIVE',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '15 days',
    NOW() + INTERVAL '15 days',
    false,
    NOW() - INTERVAL '15 days',
    NOW()
);
