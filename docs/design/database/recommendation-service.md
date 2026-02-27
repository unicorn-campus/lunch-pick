# recommendation-service 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스명 | recommendation-service |
| DBMS | PostgreSQL 15 |
| 스키마 | lunchpick_recommendation |
| 테이블 수 | 5 |
| 캐시 DB | Redis DB 2 (추천 결과 캐시) |
| 아키텍처 패턴 | Layered |
| 데이터 독립성 | 서비스 전용 독립 DB, 타 서비스 FK 없음 |

---

## 1. 설계 원칙

- **데이터 소유권**: recommendation-service가 추천·이력·피드백 데이터 완전 소유
- **크로스 서비스 조인 금지**: member DB, payment DB 직접 접근 불가
- **이벤트 기반 동기화**: 피드백 저장 시 Redis 이벤트로 취향 학습 트리거
- **캐시 활용**: 추천 결과는 Redis DB 2에 캐싱하여 AI 파이프라인 호출 최소화

---

## 2. 테이블 목록

| 테이블명 | 설명 | 비고 |
|---------|------|------|
| recommendation | 추천 결과 이력 | 추천 카드 1개 = 1행 |
| meal_record | 식사 기록 | 수락된 추천 식사 기록 |
| feedback | 피드백 | meal_record와 1:1 연결 |
| preference_vector | 취향 벡터 스냅샷 | 학습 이력 추적용 |
| learning_message | 학습 완료 메시지 | 사용자 노출용 |

---

## 3. 테이블 상세 설계

### 3.1 recommendation (추천 결과)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| recommendation_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| restaurant_id | VARCHAR(36) | NOT NULL | - | 식당 식별자 |
| restaurant_name | VARCHAR(200) | NOT NULL | - | 식당명 |
| representative_menu | VARCHAR(200) | NOT NULL | - | 대표 메뉴 |
| reason_summary | VARCHAR(500) | NULL | - | 추천 이유 요약 |
| confidence_score | INTEGER | NOT NULL | 0 | 신뢰도 점수 (0~100) |
| distance_meters | INTEGER | NOT NULL | 0 | 거리 (미터) |
| estimated_walk_minutes | INTEGER | NOT NULL | 0 | 도보 소요 시간 (분) |
| category | VARCHAR(50) | NOT NULL | - | 음식 카테고리 |
| is_fallback | BOOLEAN | NOT NULL | FALSE | 폴백 추천 여부 |
| status | VARCHAR(20) | NOT NULL | 'PENDING' | 추천 상태 |
| reaction_time_ms | INTEGER | NULL | - | 사용자 반응 시간 (ms) |
| reject_reason | VARCHAR(30) | NULL | - | 거절 사유 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 수정일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `recommendation_id`
- INDEX: `member_id, created_at DESC`

**제약 조건**:
- `status` CHECK: `('PENDING', 'ACCEPTED', 'REJECTED')`
- `reject_reason` CHECK: `('MOOD_NOT_MATCH', 'TOO_FAR', 'RECENTLY_VISITED', 'OTHER', NULL)`
- `confidence_score` CHECK: `(0 <= confidence_score AND confidence_score <= 100)`

---

### 3.2 meal_record (식사 기록)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| meal_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| recommendation_id | VARCHAR(36) | NULL | - | 연결된 추천 ID (논리적 연결) |
| restaurant_id | VARCHAR(36) | NOT NULL | - | 식당 식별자 |
| restaurant_name | VARCHAR(200) | NOT NULL | - | 식당명 |
| menu_name | VARCHAR(200) | NOT NULL | - | 메뉴명 |
| category | VARCHAR(50) | NOT NULL | - | 음식 카테고리 |
| recorded_at | TIMESTAMP | NOT NULL | - | 식사 일시 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 수정일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `meal_id`
- INDEX: `member_id, recorded_at DESC`
- INDEX: `member_id, DATE(recorded_at)`

---

### 3.3 feedback (피드백)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| feedback_id | VARCHAR(36) | NOT NULL | - | 도메인 식별자 (UUID) |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| meal_id | VARCHAR(36) | NOT NULL | - | 식사 기록 식별자 (논리적 연결) |
| satisfaction | VARCHAR(10) | NOT NULL | - | 만족도 (GOOD/BAD/NEUTRAL) |
| keyword | VARCHAR(20) | NULL | - | 피드백 키워드 |
| skipped | BOOLEAN | NOT NULL | FALSE | 피드백 스킵 여부 |
| created_at | TIMESTAMP | NOT NULL | NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | NOW() | 수정일시 |

**인덱스**:
- PK: `id`
- UNIQUE: `feedback_id`
- UNIQUE: `meal_id`
- INDEX: `member_id, created_at DESC`

**제약 조건**:
- `satisfaction` CHECK: `('GOOD', 'BAD', 'NEUTRAL')`
- `keyword` CHECK: `('TASTE', 'PORTION', 'SPEED', NULL)`

---

### 3.4 preference_vector (취향 벡터 스냅샷)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| vector_json | JSONB | NOT NULL | - | 취향 벡터 JSON |
| feedback_count | INTEGER | NOT NULL | 0 | 벡터 계산 시점 피드백 수 |
| is_cold_start | BOOLEAN | NOT NULL | TRUE | 콜드스타트 여부 |
| calculated_at | TIMESTAMP | NOT NULL | NOW() | 계산 일시 |

**인덱스**:
- PK: `id`
- INDEX: `member_id, calculated_at DESC`

---

### 3.5 learning_message (학습 메시지)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| id | BIGSERIAL | NOT NULL | auto | 내부 PK |
| member_id | VARCHAR(36) | NOT NULL | - | 회원 식별자 (논리적 연결) |
| message | VARCHAR(500) | NOT NULL | - | 학습 완료 메시지 |
| generated_at | TIMESTAMP | NOT NULL | NOW() | 생성 일시 |

**인덱스**:
- PK: `id`
- INDEX: `member_id, generated_at DESC`

---

## 4. 캐시 설계

### Redis DB 2 — recommendation-service 전용 캐시

| 캐시 키 패턴 | TTL | 설명 | 무효화 트리거 |
|-------------|-----|------|-------------|
| `rec:{member_id}:{location_grid}:{weather_code}:{weekday}` | 당일 13:00까지 | 추천 결과 캐시 | 피드백 저장, 식사 기록, 취향 벡터 갱신 |
| `rec:reason:{recommendation_id}` | 1시간 | 추천 이유 캐시 | 자동 만료 |
| `rec:stale:{member_id}` | 24시간 | Stale 추천 캐시 (SWR 패턴) | 취향 벡터 갱신 시 |

---

## 5. 데이터 흐름

```
[추천 요청] → recommendation 행 생성 (status=PENDING)
     ↓
[수락] → recommendation status=ACCEPTED, meal_record 생성
     ↓
[피드백] → feedback 생성 → 취향 벡터 갱신 이벤트
     ↓
[일일 학습] → preference_vector 스냅샷 저장, learning_message 생성
```

---

## 6. 데이터 독립성 준수 사항

- `member_id`는 VARCHAR(36) UUID 값으로만 참조 (FK 없음)
- `recommendation_id`, `meal_id` 모두 서비스 내 논리적 연결
- member-service의 취향 프로파일은 Internal API로만 조회
