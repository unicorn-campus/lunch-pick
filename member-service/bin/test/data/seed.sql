-- member-service 테스트 데이터 seed
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실행 DB: member (jdbc:postgresql://localhost:15432/member)

-- 테이블 초기화 (외래키 없으므로 순서 무관)
TRUNCATE TABLE location_consent RESTART IDENTITY CASCADE;
TRUNCATE TABLE dietary_restriction RESTART IDENTITY CASCADE;
TRUNCATE TABLE taste_profile RESTART IDENTITY CASCADE;
TRUNCATE TABLE member RESTART IDENTITY CASCADE;

-- 테스트 회원 1: 온보딩 완료, 위치 동의, 일반 식단
INSERT INTO member (member_id, kakao_id, email, nickname, onboarding_completed, location_enabled, recommendation_alert, feedback_reminder, created_at, updated_at)
VALUES (
    'test-member-uuid-0001',
    'kakao-test-001',
    'test01@lunchpick.com',
    '테스트런치왕',
    true,
    true,
    true,
    true,
    NOW(),
    NOW()
);

-- 테스트 회원 2: 온보딩 미완료 (콜드스타트)
INSERT INTO member (member_id, kakao_id, email, nickname, onboarding_completed, location_enabled, recommendation_alert, feedback_reminder, created_at, updated_at)
VALUES (
    'test-member-uuid-0002',
    'kakao-test-002',
    'test02@lunchpick.com',
    '신규가입자',
    false,
    false,
    true,
    true,
    NOW(),
    NOW()
);

-- 취향 프로파일 (테스트 회원 1용 — 피드백 5건 이상으로 콜드스타트 해제)
INSERT INTO taste_profile (member_id, taste_vector, feedback_count, is_cold_start, updated_at)
VALUES (
    'test-member-uuid-0001',
    '{"한식": 0.85, "일식": 0.70, "중식": 0.40, "양식": 0.30, "분식": 0.60}',
    10,
    false,
    NOW()
);

-- 취향 프로파일 (테스트 회원 2용 — 콜드스타트)
INSERT INTO taste_profile (member_id, taste_vector, feedback_count, is_cold_start, updated_at)
VALUES (
    'test-member-uuid-0002',
    '{}',
    0,
    true,
    NOW()
);

-- 식이 제한 (테스트 회원 1용)
INSERT INTO dietary_restriction (member_id, allergens, custom_allergens, diet_type, health_info_consent_given, updated_at)
VALUES (
    'test-member-uuid-0001',
    '["땅콩", "갑각류"]',
    '[]',
    '일반',
    true,
    NOW()
);

-- 위치 동의 이력 (테스트 회원 1용)
INSERT INTO location_consent (member_id, consented, consented_at, created_at)
VALUES (
    'test-member-uuid-0001',
    true,
    NOW(),
    NOW()
);
