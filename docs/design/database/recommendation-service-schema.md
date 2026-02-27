# recommendation-service DB 스키마 스크립트

> Database: lunchpick_recommendation (PostgreSQL 15)
> Schema: lunchpick_recommendation

```sql
-- ============================================================
-- recommendation-service 스키마 생성
-- ============================================================

CREATE SCHEMA IF NOT EXISTS lunchpick_recommendation;
SET search_path = lunchpick_recommendation;

-- ============================================================
-- TABLE: recommendation (추천 결과)
-- ============================================================

CREATE TABLE IF NOT EXISTS recommendation (
    id                      BIGSERIAL     PRIMARY KEY,
    recommendation_id       VARCHAR(36)   NOT NULL,
    member_id               VARCHAR(36)   NOT NULL,
    restaurant_id           VARCHAR(36)   NOT NULL,
    restaurant_name         VARCHAR(200)  NOT NULL,
    representative_menu     VARCHAR(200)  NOT NULL,
    reason_summary          VARCHAR(500),
    confidence_score        INTEGER       NOT NULL DEFAULT 0,
    distance_meters         INTEGER       NOT NULL DEFAULT 0,
    estimated_walk_minutes  INTEGER       NOT NULL DEFAULT 0,
    category                VARCHAR(50)   NOT NULL,
    is_fallback             BOOLEAN       NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reaction_time_ms        INTEGER,
    reject_reason           VARCHAR(30),
    created_at              TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_recommendation_id
        UNIQUE (recommendation_id),
    CONSTRAINT chk_recommendation_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT chk_recommendation_reject_reason
        CHECK (reject_reason IN ('MOOD_NOT_MATCH', 'TOO_FAR', 'RECENTLY_VISITED', 'OTHER') OR reject_reason IS NULL),
    CONSTRAINT chk_recommendation_confidence_score
        CHECK (confidence_score >= 0 AND confidence_score <= 100),
    CONSTRAINT chk_recommendation_distance
        CHECK (distance_meters >= 0),
    CONSTRAINT chk_recommendation_walk_minutes
        CHECK (estimated_walk_minutes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_recommendation_member_created
    ON recommendation (member_id, created_at DESC);

COMMENT ON TABLE  recommendation IS '추천 결과 이력 (추천 카드 1개 = 1행)';
COMMENT ON COLUMN recommendation.recommendation_id IS '도메인 식별자 (UUID)';
COMMENT ON COLUMN recommendation.is_fallback       IS '폴백 추천 여부 (AI 실패 시)';
COMMENT ON COLUMN recommendation.status            IS 'PENDING | ACCEPTED | REJECTED';
COMMENT ON COLUMN recommendation.reaction_time_ms  IS '사용자 반응 시간 (밀리초)';

-- ============================================================
-- TABLE: meal_record (식사 기록)
-- ============================================================

CREATE TABLE IF NOT EXISTS meal_record (
    id                BIGSERIAL     PRIMARY KEY,
    meal_id           VARCHAR(36)   NOT NULL,
    member_id         VARCHAR(36)   NOT NULL,
    recommendation_id VARCHAR(36),
    restaurant_id     VARCHAR(36)   NOT NULL,
    restaurant_name   VARCHAR(200)  NOT NULL,
    menu_name         VARCHAR(200)  NOT NULL,
    category          VARCHAR(50)   NOT NULL,
    recorded_at       TIMESTAMP     NOT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_meal_record_meal_id UNIQUE (meal_id)
);

CREATE INDEX IF NOT EXISTS idx_meal_record_member_recorded
    ON meal_record (member_id, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_meal_record_member_date
    ON meal_record (member_id, DATE(recorded_at));

COMMENT ON TABLE  meal_record IS '식사 기록 (수락된 추천 또는 직접 입력)';
COMMENT ON COLUMN meal_record.meal_id            IS '도메인 식별자 (UUID)';
COMMENT ON COLUMN meal_record.recommendation_id  IS '연결된 추천 ID (직접 입력 시 NULL)';

-- ============================================================
-- TABLE: feedback (피드백)
-- ============================================================

CREATE TABLE IF NOT EXISTS feedback (
    id           BIGSERIAL    PRIMARY KEY,
    feedback_id  VARCHAR(36)  NOT NULL,
    member_id    VARCHAR(36)  NOT NULL,
    meal_id      VARCHAR(36)  NOT NULL,
    satisfaction VARCHAR(10)  NOT NULL,
    keyword      VARCHAR(20),
    skipped      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_feedback_feedback_id UNIQUE (feedback_id),
    CONSTRAINT uq_feedback_meal_id     UNIQUE (meal_id),
    CONSTRAINT chk_feedback_satisfaction
        CHECK (satisfaction IN ('GOOD', 'BAD', 'NEUTRAL')),
    CONSTRAINT chk_feedback_keyword
        CHECK (keyword IN ('TASTE', 'PORTION', 'SPEED') OR keyword IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_feedback_member_created
    ON feedback (member_id, created_at DESC);

COMMENT ON TABLE  feedback IS '식사 피드백 (식사 기록 1:1)';
COMMENT ON COLUMN feedback.satisfaction IS 'GOOD | BAD | NEUTRAL';
COMMENT ON COLUMN feedback.keyword      IS 'TASTE | PORTION | SPEED (선택)';
COMMENT ON COLUMN feedback.skipped      IS '피드백 스킵 여부';

-- ============================================================
-- TABLE: preference_vector (취향 벡터 스냅샷)
-- ============================================================

CREATE TABLE IF NOT EXISTS preference_vector (
    id             BIGSERIAL    PRIMARY KEY,
    member_id      VARCHAR(36)  NOT NULL,
    vector_json    JSONB        NOT NULL,
    feedback_count INTEGER      NOT NULL DEFAULT 0,
    is_cold_start  BOOLEAN      NOT NULL DEFAULT TRUE,
    calculated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_preference_vector_feedback_count CHECK (feedback_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_preference_vector_member_calculated
    ON preference_vector (member_id, calculated_at DESC);

COMMENT ON TABLE  preference_vector IS '취향 벡터 계산 결과 스냅샷 (학습 이력 추적용)';
COMMENT ON COLUMN preference_vector.vector_json IS 'JSONB: {category: weight} 취향 벡터';

-- ============================================================
-- TABLE: learning_message (학습 완료 메시지)
-- ============================================================

CREATE TABLE IF NOT EXISTS learning_message (
    id           BIGSERIAL     PRIMARY KEY,
    member_id    VARCHAR(36)   NOT NULL,
    message      VARCHAR(500)  NOT NULL,
    generated_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_learning_message_member_generated
    ON learning_message (member_id, generated_at DESC);

COMMENT ON TABLE  learning_message IS 'AI 취향 학습 완료 후 사용자 노출 메시지';

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

CREATE OR REPLACE TRIGGER trg_recommendation_updated_at
    BEFORE UPDATE ON recommendation
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_meal_record_updated_at
    BEFORE UPDATE ON meal_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER trg_feedback_updated_at
    BEFORE UPDATE ON feedback
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```
