# member-service DB 스키마 스크립트

> Database: lunchpick_member (PostgreSQL 15)
> Schema: lunchpick_member

```sql
-- ============================================================
-- member-service 스키마 생성
-- ============================================================

CREATE SCHEMA IF NOT EXISTS lunchpick_member;
SET search_path = lunchpick_member;

-- ============================================================
-- ENUM 타입
-- ============================================================

DO $$ BEGIN
    CREATE TYPE diet_type_enum AS ENUM ('일반', '채식', '비건', '할랄', '기타');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- ============================================================
-- TABLE: member (회원)
-- ============================================================

CREATE TABLE IF NOT EXISTS member (
    id                    BIGSERIAL       PRIMARY KEY,
    member_id             VARCHAR(36)     NOT NULL,
    kakao_id              VARCHAR(50)     NOT NULL,
    email                 VARCHAR(200),
    nickname              VARCHAR(50)     NOT NULL,
    onboarding_completed  BOOLEAN         NOT NULL DEFAULT FALSE,
    location_enabled      BOOLEAN         NOT NULL DEFAULT FALSE,
    recommendation_alert  BOOLEAN         NOT NULL DEFAULT TRUE,
    feedback_reminder     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_member_member_id UNIQUE (member_id),
    CONSTRAINT uq_member_kakao_id  UNIQUE (kakao_id)
);

CREATE INDEX IF NOT EXISTS idx_member_email ON member (email);

COMMENT ON TABLE  member IS '회원 기본 정보';
COMMENT ON COLUMN member.member_id            IS '도메인 식별자 (UUID)';
COMMENT ON COLUMN member.kakao_id             IS '카카오 OAuth 사용자 ID';
COMMENT ON COLUMN member.onboarding_completed IS '온보딩 완료 여부';
COMMENT ON COLUMN member.location_enabled     IS '위치 정보 사용 동의 여부';

-- ============================================================
-- TABLE: taste_profile (취향 프로파일)
-- ============================================================

CREATE TABLE IF NOT EXISTS taste_profile (
    id             BIGSERIAL    PRIMARY KEY,
    member_id      VARCHAR(36)  NOT NULL,
    taste_vector   JSONB,
    feedback_count INTEGER      NOT NULL DEFAULT 0,
    is_cold_start  BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_taste_profile_member_id UNIQUE (member_id),
    CONSTRAINT chk_taste_profile_feedback_count CHECK (feedback_count >= 0)
);

COMMENT ON TABLE  taste_profile IS '회원 취향 프로파일 (카테고리별 가중치 벡터)';
COMMENT ON COLUMN taste_profile.taste_vector   IS 'JSONB: {category: weight} 형태의 취향 벡터';
COMMENT ON COLUMN taste_profile.is_cold_start  IS '피드백 부족으로 콜드스타트 상태 여부';

-- ============================================================
-- TABLE: dietary_restriction (식이 제한)
-- ============================================================

CREATE TABLE IF NOT EXISTS dietary_restriction (
    id                        BIGSERIAL    PRIMARY KEY,
    member_id                 VARCHAR(36)  NOT NULL,
    allergens                 JSONB                 DEFAULT '[]'::JSONB,
    custom_allergens          JSONB                 DEFAULT '[]'::JSONB,
    diet_type                 VARCHAR(20)  NOT NULL DEFAULT '일반',
    health_info_consent_given BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_dietary_restriction_member_id UNIQUE (member_id),
    CONSTRAINT chk_dietary_restriction_diet_type
        CHECK (diet_type IN ('일반', '채식', '비건', '할랄', '기타'))
);

COMMENT ON TABLE  dietary_restriction IS '회원 식이 제한 정보';
COMMENT ON COLUMN dietary_restriction.allergens                 IS 'JSONB: 시스템 제공 알레르기 목록';
COMMENT ON COLUMN dietary_restriction.custom_allergens          IS 'JSONB: 사용자 직접 입력 알레르기';
COMMENT ON COLUMN dietary_restriction.health_info_consent_given IS '건강 정보 수집 동의 여부';

-- ============================================================
-- TABLE: location_consent (위치 동의 이력)
-- ============================================================

CREATE TABLE IF NOT EXISTS location_consent (
    id           BIGSERIAL    PRIMARY KEY,
    member_id    VARCHAR(36)  NOT NULL,
    consented    BOOLEAN      NOT NULL,
    consented_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_location_consent_member_id
    ON location_consent (member_id);

COMMENT ON TABLE  location_consent IS '위치 정보 동의 변경 이력 (전체 보관)';
COMMENT ON COLUMN location_consent.consented    IS '동의(TRUE) / 철회(FALSE)';
COMMENT ON COLUMN location_consent.consented_at IS '동의/철회 일시';

-- ============================================================
-- 자동 updated_at 갱신 트리거
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_member_updated_at
    BEFORE UPDATE ON member
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_taste_profile_updated_at
    BEFORE UPDATE ON taste_profile
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_dietary_restriction_updated_at
    BEFORE UPDATE ON dietary_restriction
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```
