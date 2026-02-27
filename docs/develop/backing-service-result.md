# 백킹서비스 설치 결과서

> 작성자: 강도윤 (데브-백) / 백엔드 개발자
> 작성일: 2026-02-26
> 단계: Step 2-2. 백킹서비스 + Mock 서버 로컬 구성

---

## 구성 환경

- 환경: docker-compose (로컬 개발)
- 기동 일시: 2026-02-26
- Docker 버전: 27.5.1
- 활성화 프로파일: (기본) postgres + redis / mock 프로파일 추가 시 Prism 4개

## 포트 충돌 해결 내역

로컬 머신에 PostgreSQL(5432)과 Redis(6379)가 이미 실행 중이어 `.env`에서 포트를 변경하였다.

| 서비스 | 기본 포트 | 변경 포트 | 사유 |
|--------|---------|---------|------|
| PostgreSQL | 5432 | 15432 | 로컬 PostgreSQL 실행 중 |
| Redis | 6379 | 16379 | 로컬 Redis 실행 중 |

> `.env` 파일에서 `DB_PORT=15432`, `REDIS_PORT=16379`으로 설정됨.
> `.env.example`은 기본값(5432, 6379) 유지.

---

## 서비스 연결 정보

### PostgreSQL 16

| 항목 | 값 |
|------|---|
| Host | localhost |
| Port | 15432 (로컬 충돌로 변경, 기본값 5432) |
| User | lunchpick |
| Password | P@ssw0rd$ |
| 기본 DB | lunchpick (POSTGRES_USER 동명 자동 생성) |

#### 서비스별 Database

| Database | 용도 | JDBC URL |
|---------|------|----------|
| `member` | 회원 정보, 취향 프로파일, 식이제한, 위치동의 이력 | `jdbc:postgresql://localhost:15432/member` |
| `recommendation` | 추천 이력, 식사 기록, 피드백, 취향벡터 스냅샷 | `jdbc:postgresql://localhost:15432/recommendation` |
| `payment` | 구독 정보, 결제 이력 (INSERT ONLY) | `jdbc:postgresql://localhost:15432/payment` |

> 테이블은 생성하지 않음 — JPA `ddl-auto=update`로 애플리케이션 기동 시 자동 생성.
> ai-pipeline-service는 Stateless, 영속 DB 없음 (Redis DB 4 전용).

### Redis 7.x

| 항목 | 값 |
|------|---|
| Host | localhost |
| Port | 16379 (로컬 충돌로 변경, 기본값 6379) |
| Connection | `redis://localhost:16379` |
| maxmemory-policy | allkeys-lru |

#### Redis DB 용도 분리

| DB 번호 | 용도 | TTL | 사용 서비스 |
|---------|------|-----|-----------|
| DB 0 | 세션, JWT 블랙리스트, **Redis Streams MQ** (subscription-events) | 세션 1h, JWT 토큰 만료까지 | 공통 |
| DB 1 | 취향 프로파일 캐시, 회원 프로파일 캐시 | 취향 30분, 프로파일 10분 | member-service |
| DB 2 | 추천 결과 캐시, 추천 이유 캐시, Stale 캐시 | 당일 13:00, 이유 1h, Stale 24h | recommendation-service |
| DB 3 | 구독 플랜 캐시, 활성 구독 캐시, 중복 결제 Lock | 플랜 1h, 구독 10분, Lock 30초 | payment-service |
| DB 4 | AI 추천 결과 캐시, AI 추천 이유 캐시, LLM 응답 해시 캐시 | 당일 13:00, 이유 1h, 해시 30분 | ai-pipeline-service |

### Redis Streams (MQ)

별도 MQ 서비스(RabbitMQ/Kafka) 없음. Redis DB 0의 Streams 기능으로 대체.

| 항목 | 값 |
|------|---|
| 토픽 | `subscription-events` |
| 발행자 | payment-service |
| 소비자 | member-service |
| 이벤트 종류 | 구독 활성화, 해지 예약, 7일 연장 (3종) |

### Prism Mock 서버 (mock 프로파일)

| 서비스 | 포트 | OpenAPI 명세 | Base URL |
|--------|------|------------|----------|
| prism-member | 4010 | `docs/design/api/member-service-api.yaml` | `http://localhost:4010` |
| prism-recommendation | 4011 | `docs/design/api/recommendation-service-api.yaml` | `http://localhost:4011` |
| prism-payment | 4012 | `docs/design/api/payment-service-api.yaml` | `http://localhost:4012` |
| prism-ai | 4013 | `docs/design/api/ai-pipeline-api.yaml` | `http://localhost:4013` |

---

## 기동 명령어

```bash
# 기본 서비스만 기동 (PostgreSQL + Redis)
docker compose up -d

# Mock 서버 포함 기동 (+ Prism 4개)
docker compose --profile mock up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f

# 서비스 중지
docker compose down

# 볼륨까지 초기화
docker compose down -v
```

---

## 연결 확인 결과

### 기본 서비스

| 서비스 | 확인 명령 | 결과 |
|--------|----------|------|
| PostgreSQL | `docker compose exec postgres pg_isready -U lunchpick` | `/var/run/postgresql:5432 - accepting connections` |
| Redis | `docker compose exec redis redis-cli ping` | `PONG` |

### Database 생성 확인

`docker compose exec postgres psql -U lunchpick -c "\l"` 실행 결과:

```
      Name      |   Owner
----------------+-----------
 lunchpick      | lunchpick   ← 기본 DB (POSTGRES_USER 동명)
 member         | lunchpick   ← member-service 전용
 payment        | lunchpick   ← payment-service 전용
 recommendation | lunchpick   ← recommendation-service 전용
```

### Prism Mock 응답 확인

| 서비스 | 요청 | HTTP 상태 | 비고 |
|--------|------|---------|------|
| prism-member | `GET /api/v1/members/profile` (Bearer test) | 200 | 회원 프로파일 JSON 반환 |
| prism-recommendation | `GET /api/v1/recommendations/today` (Bearer test) | 400 | 유효성 검사 — Prism 정상 동작 |
| prism-payment | `GET /api/v1/subscriptions/plans` (Bearer test) | 200 | 구독 플랜 3개 JSON 반환 |
| prism-ai | `POST /api/v1/ai/recommendations` | 400 | 유효성 검사 — Prism 정상 동작 |

**prism-payment 응답 예시 (`GET /api/v1/subscriptions/plans`):**

```json
{
  "plans": [
    {"planId": "FREE", "planName": "무료", "pricePerMonth": 0, ...},
    {"planId": "PREMIUM_MONTHLY", "planName": "프리미엄 월간", "pricePerMonth": 4900, ...},
    {"planId": "PREMIUM_ANNUAL", "planName": "프리미엄 연간", "pricePerMonth": 3900, "discountRate": 20, ...}
  ],
  "currentPlan": "FREE"
}
```

**prism-member 응답 예시 (`GET /api/v1/members/profile`):**

```json
{
  "memberId": "550e8400-e29b-41d4-a716-446655440001",
  "nickname": "런치왕김과장",
  "email": "kimkwajang@example.com",
  "dietType": "일반",
  "allergens": ["땅콩"],
  "locationEnabled": true,
  "onboardingCompleted": true
}
```

---

## 품질 기준 체크리스트

- [x] 모든 설정값이 환경변수(`${VAR}`)로 처리됨
- [x] 민감 정보(비밀번호)가 docker-compose.yml에 하드코딩되지 않음
- [x] `.env.example`이 작성되고 `.env`가 `.gitignore`에 등록됨
- [x] 서비스별 database가 분리 구성됨 (단일 PostgreSQL 인스턴스, 3개 database)
- [x] MQ 서비스 미포함 (Redis Streams 사용 — 별도 MQ 불필요)
- [x] `docker compose ps`로 모든 기본 서비스가 `healthy` 상태 확인
- [x] Prism Mock 서버 4개가 OpenAPI yaml 기반으로 정상 응답 반환
- [x] `docs/develop/backing-service-result.md` 작성 완료

---

## 산출물 목록

| 파일 | 설명 |
|------|------|
| `./docker-compose.yml` | 기본 서비스 + mock 프로파일 정의 |
| `./.env.example` | 환경변수 템플릿 (형상관리 대상) |
| `./.env` | 실행용 환경변수 (gitignore, 포트 충돌 반영) |
| `./docker/postgres/init/01-create-databases.sql` | DB 초기화 스크립트 |
| `./docs/develop/backing-service-result.md` | 본 결과서 |
