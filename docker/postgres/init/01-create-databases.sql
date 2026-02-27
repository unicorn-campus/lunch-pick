-- ============================================================
-- 런치픽(LunchPick) PostgreSQL 서비스별 database 초기화 스크립트
-- 실행 시점: PostgreSQL 컨테이너 최초 기동 시 자동 실행
-- 주의: 테이블은 생성하지 않음 — JPA ddl-auto=update로 자동 생성
-- ============================================================

-- member-service 전용 database + schema
CREATE DATABASE member;
GRANT ALL PRIVILEGES ON DATABASE member TO lunchpick;
\c member
CREATE SCHEMA IF NOT EXISTS lunchpick_member AUTHORIZATION lunchpick;

-- recommendation-service 전용 database + schema
\c postgres
CREATE DATABASE recommendation;
GRANT ALL PRIVILEGES ON DATABASE recommendation TO lunchpick;
\c recommendation
CREATE SCHEMA IF NOT EXISTS lunchpick_recommendation AUTHORIZATION lunchpick;

-- payment-service 전용 database + schema (INSERT ONLY, 5년 보존)
\c postgres
CREATE DATABASE payment;
GRANT ALL PRIVILEGES ON DATABASE payment TO lunchpick;
\c payment
CREATE SCHEMA IF NOT EXISTS lunchpick_payment AUTHORIZATION lunchpick;

-- ai-pipeline-service: Stateless, 영속 DB 없음 (Redis DB 4만 사용)
