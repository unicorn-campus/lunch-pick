# 백엔드 컨테이너 실행 결과서

## 구성 환경
- 환경: docker run (VM 컨테이너 배포)
- VM: gcp (34.64.192.123)
- 실행 일시: 2026-02-28

## VM 접속 방법
```
ssh gcp
```

## 실행된 컨테이너

| 서비스명 | 이미지 | 포트 매핑 | 네트워크 | 상태 |
|---------|--------|----------|---------|------|
| member-service | asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/member-service:latest | 8081:8081 | lunch-menu-recommender_default | Running |
| recommendation-service | asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/recommendation-service:latest | 8082:8082 | lunch-menu-recommender_default | Running |
| payment-service | asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/payment-service:latest | 8083:8083 | lunch-menu-recommender_default | Running |

## Health Check 결과
- [x] member-service: `{"status":"UP"}` — DB(PostgreSQL) UP, Redis UP (v7.4.8)
- [x] recommendation-service: `{"status":"UP"}` — DB(PostgreSQL) UP, Redis UP (v7.4.8)
- [x] payment-service: `{"status":"UP"}` — DB(PostgreSQL) UP, Redis UP (v7.4.8)

> Docker HEALTHCHECK가 기본 포트 8080을 검사하여 unhealthy로 표시되지만, 실제 서비스는 커스텀 포트(8081~8083)에서 정상 동작 중.

## 환경변수 설정 (공통)

| 변수 | 값 | 비고 |
|------|---|------|
| DB_HOST | postgres | docker-compose 서비스명 |
| DB_PORT | 5432 | docker-compose 내부 포트 |
| DB_USER | lunchpick | |
| REDIS_HOST | redis | docker-compose 서비스명 |
| REDIS_PORT | 6379 | docker-compose 내부 포트 |
| CORS_ALLOWED_ORIGINS | http://localhost:3000,http://34.64.192.123:3000 | VM 프론트엔드 주소 추가 |

### 서비스별 차이점

| 서비스 | DB_NAME | REDIS_DATABASE | 추가 설정 |
|--------|---------|---------------|----------|
| member-service | member | 1 | KAFKA_BROKERS=redis:6379 (Redis Streams 소비) |
| recommendation-service | recommendation | 2 | MEMBER_SERVICE_URL=http://member-service:8081, AI_PIPELINE_SERVICE_URL=http://ai-pipeline-service:8000 |
| payment-service | payment | 3 | KAFKA_BROKERS=redis:6379 (Redis Streams 발행) |

## 실행 명령어

### member-service
```bash
REGISTRY=asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick
docker run -d --name member-service --rm --network lunch-menu-recommender_default \
  -p 8081:8081 \
  -e SERVER_PORT=8081 -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql -e DB_HOST=postgres -e DB_PORT=5432 \
  -e DB_NAME=member -e DB_USER=lunchpick -e 'DB_PASSWORD=P@ssw0rd$' \
  -e DDL_AUTO=update -e SHOW_SQL=true \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 -e REDIS_DATABASE=1 \
  -e KAFKA_BROKERS=redis:6379 -e MQ_SUBSCRIPTION_TOPIC=subscription-events \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e KAKAO_CLIENT_ID=d34722dff8545446e14b2616bb62c6b0 \
  -e KAKAO_CLIENT_SECRET=W6R3Gn0QyNaLj8ugyS7gQVxhJq7GRwUv \
  -e KAKAO_REDIRECT_URI=http://34.64.192.123:3000/login \
  -e 'CORS_ALLOWED_ORIGINS=http://localhost:3000,http://34.64.192.123:3000' \
  -e LOG_LEVEL_ROOT=INFO -e LOG_LEVEL_APP=DEBUG -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/member-service.log \
  ${REGISTRY}/member-service:latest
```

### recommendation-service
```bash
REGISTRY=asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick
docker run -d --name recommendation-service --rm --network lunch-menu-recommender_default \
  -p 8082:8082 \
  -e SERVER_PORT=8082 -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql -e DB_HOST=postgres -e DB_PORT=5432 \
  -e DB_NAME=recommendation -e DB_USER=lunchpick -e 'DB_PASSWORD=P@ssw0rd$' \
  -e DDL_AUTO=update -e SHOW_SQL=true \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 -e REDIS_DATABASE=2 \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e MEMBER_SERVICE_URL=http://member-service:8081 \
  -e AI_PIPELINE_SERVICE_URL=http://ai-pipeline-service:8000 \
  -e WEATHER_API_URL=https://api.openweathermap.org \
  -e WEATHER_API_KEY=1aa5bfca079a20586915b56f29235cc0 \
  -e KAKAO_API_KEY=b588f91e780efc914b282ad7e3688e01 \
  -e 'CORS_ALLOWED_ORIGINS=http://localhost:3000,http://34.64.192.123:3000' \
  -e LOG_LEVEL_ROOT=INFO -e LOG_LEVEL_APP=DEBUG -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/recommendation-service.log \
  ${REGISTRY}/recommendation-service:latest
```

### payment-service
```bash
REGISTRY=asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick
docker run -d --name payment-service --rm --network lunch-menu-recommender_default \
  -p 8083:8083 \
  -e SERVER_PORT=8083 -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql -e DB_HOST=postgres -e DB_PORT=5432 \
  -e DB_NAME=payment -e DB_USER=lunchpick -e 'DB_PASSWORD=P@ssw0rd$' \
  -e DDL_AUTO=update -e SHOW_SQL=true \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 -e REDIS_DATABASE=3 \
  -e KAFKA_BROKERS=redis:6379 -e MQ_SUBSCRIPTION_TOPIC=subscription-events \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e 'CORS_ALLOWED_ORIGINS=http://localhost:3000,http://34.64.192.123:3000' \
  -e LOG_LEVEL_ROOT=INFO -e LOG_LEVEL_APP=DEBUG -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/payment-service.log \
  ${REGISTRY}/payment-service:latest
```
