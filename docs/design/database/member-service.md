# member-service 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스명 | member-service |
| DBMS | PostgreSQL 15 |
| 스키마 | lunchpick_member |
| 테이블 수 | 4 |
| 캐시 DB | Redis DB 0 (공통: 세션/JWT), Redis DB 1 (취향 프로파일 캐시) |
| 아키텍처 패턴 | Layered |
| 데이터 독립성 | 서비스 전용 독립 DB, 타 서비스 FK 없음 |

---

## 1. 설계 원칙

- **데이터 소유권**: member-service가 회원 정보 완전 소유
- **크로스 서비스 조인 금지**: recommendation-service, payment-service에서 member DB 직접 접근 불가
- **이벤트 기반 동기화**: 취향 벡터 갱신 시 Redis 이벤트로 타 서비스에 알림
- **캐시 활용**: 타 서비스는 Internal API를 통해 회원 정보 조회 후 자체 캐시에 보관

---

## 2. 테이블 목록

| 테이블명 | 설명 | 비고 |
|---------|------|------|
| member | 회원 기본 정보 | 핵심 엔티티 |
| taste_profile | 취향 프로파일 (벡터 기반) | memberId로 1:1 연결 |
| dietary_restriction | 식이 제한 정보 | memberId로 1:1 연결 |
| location_consent | 위치 정보 동의 이력 | memberId로 N개 가능 |

---

## 3. 테이블 상세 설계

### 3.1 member (회원)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| kakao_id | VARCHAR(50) | NOT NULL | - | 카카오 OAuth ID |
| email | VARCHAR(200) | NULL | - | 이메일 (카카오 제공) |
| nickname | VARCHAR(50) | NOT NULL | - | 닉네임 |
| onboarding_completed | BOOLEAN | NOT NULL | FALSE | 온보딩 완료 여부 |
| location_enabled | BOOLEAN | NOT NULL | FALSE | 위치 정보 사용 동의 |
| recommendation_alert | BOOLEAN | NOT NULL | TRUE | 추천 알림 수신 여부 |
| feedback_reminder | BOOLEAN | NOT NULL | TRUE | 피드백 리마인더 여부 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 수정일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `member_id`
- UNIQUE: `kakao_id`
- INDEX: `email`

---

### 3.2 taste_profile (취향 프로파일)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (FK 없음, 논리적 연결) |
| taste_vector | JSONB | NULL | - | 취향 벡터 JSON (카테고리별 가중치) |
| feedback_count | INTEGER | NOT NULL | 0 | 누적 피드백 수 |
| is_cold_start | BOOLEAN | NOT NULL | TRUE | 콜드스타트 여부 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 갱신일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `member_id`

---

### 3.3 dietary_restriction (식이 제한)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| allergens | JSONB | NULL | '[]' | 알레르기 목록 (시스템 제공) |
| custom_allergens | JSONB | NULL | '[]' | 사용자 직접 입력 알레르기 |
| diet_type | VARCHAR(20) | NOT NULL | '일반' | 식단 유형 (DietType Enum) |
| health_info_consent_given | BOOLEAN | NOT NULL | FALSE | 건강 정보 수집 동의 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 갱신일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `member_id`

**제약 조건**:
- `diet_type` CHECK: `('일반', '채식', '비건', '할랄', '기타')`

---

### 3.4 location_consent (위치 동의 이력)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| consented | BOOLEAN | NOT NULL | - | 동의 여부 |
| consented_at | TIMESTAMP | NULL | - | 동의 일시 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 레코드 생성일시 |

**인덱스**:
- PK: `id`
- INDEX: `member_id`

---

## 4. 캐시 설계

### Redis DB 0 — 공통 영역 (세션/인증)

| 캐시 키 패턴 | TTL | 설명 | 무효화 트리거 |
|-------------|-----|------|-------------|
| `session:{member_id}` | 1시간 | 세션 정보 | 로그아웃 시 |
| `jwt:blacklist:{jti}` | 토큰 만료 시간 | JWT 블랙리스트 | 자동 만료 |

### Redis DB 1 — member-service 전용 캐시

| 캐시 키 패턴 | TTL | 설명 | 무효화 트리거 |
|-------------|-----|------|-------------|
| `member:taste_profile:{member_id}` | 30분 | 취향 프로파일 캐시 | 온보딩 완료, 취향 벡터 갱신 시 |
| `member:profile:{member_id}` | 10분 | 회원 프로파일 캐시 | 프로파일 업데이트 시 |

---

## 5. 데이터 흐름

```
[카카오 OAuth] → member 테이블 upsert
     ↓
[온보딩 완료] → taste_profile 최초 생성
     ↓
[식이제한 설정] → dietary_restriction upsert
     ↓
[위치 동의] → location_consent 이력 insert
```

---

## 6. 데이터 독립성 준수 사항

- recommendation-service, payment-service는 `member_id` (VARCHAR UUID) 를 키로만 사용
- 타 서비스에서 회원 정보 필요 시 `/internal/members/{memberId}` API 호출
- 취향 벡터 갱신 시 Redis Pub/Sub 이벤트로 ai-pipeline-service에 통보
