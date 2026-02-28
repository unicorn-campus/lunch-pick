# 백엔드 서비스 컨테이너 실행 결과

## 실행 일시
2026-02-28

## 환경
- VM: Azure (20.249.211.13)
- 사용자: azureuser
- 프로젝트 경로: ~/workspace/lunchpick
- 네트워크: lunchpick_default (docker-compose 네트워크)

## 이슈 및 해결

### Dockerfile HEALTHCHECK 포트 하드코딩 문제
- **증상**: 컨테이너 상태가 `(unhealthy)`로 표시됨
- **원인**: `Dockerfile-backend`의 HEALTHCHECK가 포트 8080 하드코딩
  ```
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health
  ```
- **해결**: `SERVER_PORT` 환경변수를 참조하도록 Dockerfile 수정
  ```dockerfile
  ENV SERVER_PORT=8080
  EXPOSE ${SERVER_PORT}
  HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1
  ```
- **파일**: `deployment/container/Dockerfile-backend`

## Step 1: Docker 이미지 빌드

### 실행 명령어
```bash
ssh azure 'cd ~/workspace/lunchpick && for svc in member-service recommendation-service payment-service; do
  docker build --platform linux/amd64 \
    --build-arg BUILD_LIB_DIR=${svc}/build/libs \
    --build-arg ARTIFACTORY_FILE=${svc}.jar \
    -f deployment/container/Dockerfile-backend \
    -t ${svc}:latest .
done'
```

### 빌드 결과
| 서비스 | 결과 | 소요시간 |
|--------|------|----------|
| member-service | 성공 (Exit: 0) | 5.3s |
| recommendation-service | 성공 (Exit: 0) | 11.7s |
| payment-service | 성공 (Exit: 0) | 5.4s |

## Step 2: 컨테이너 실행

### member-service
```bash
docker run -d --name member-service --rm --network lunchpick_default \
  -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=member \
  -e DB_USER=lunchpick \
  -e "DB_PASSWORD=P@ssw0rd$" \
  -e DDL_AUTO=update \
  -e SHOW_SQL=true \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e REDIS_DATABASE=1 \
  -e KAFKA_BROKERS=redis:6379 \
  -e MQ_SUBSCRIPTION_TOPIC=subscription-events \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e KAKAO_CLIENT_ID=d34722dff8545446e14b2616bb62c6b0 \
  -e KAKAO_CLIENT_SECRET=W6R3Gn0QyNaLj8ugyS7gQVxhJq7GRwUv \
  -e KAKAO_REDIRECT_URI=http://20.249.211.13:3000/login \
  -e "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://20.249.211.13:3000" \
  -e LOG_LEVEL_ROOT=INFO \
  -e LOG_LEVEL_APP=DEBUG \
  -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG \
  -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/member-service.log \
  member-service:latest
```
- Container ID: `3451ed1e4252...`

### recommendation-service
```bash
docker run -d --name recommendation-service --rm --network lunchpick_default \
  -p 8082:8082 \
  -e SERVER_PORT=8082 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=recommendation \
  -e DB_USER=lunchpick \
  -e "DB_PASSWORD=P@ssw0rd$" \
  -e DDL_AUTO=update \
  -e SHOW_SQL=true \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e REDIS_DATABASE=2 \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e MEMBER_SERVICE_URL=http://member-service:8081 \
  -e AI_PIPELINE_SERVICE_URL=http://ai-pipeline-service:8084 \
  -e WEATHER_API_URL=https://api.openweathermap.org \
  -e WEATHER_API_KEY=1aa5bfca079a20586915b56f29235cc0 \
  -e KAKAO_API_KEY=b588f91e780efc914b282ad7e3688e01 \
  -e "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://20.249.211.13:3000" \
  -e LOG_LEVEL_ROOT=INFO \
  -e LOG_LEVEL_APP=DEBUG \
  -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG \
  -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/recommendation-service.log \
  recommendation-service:latest
```
- Container ID: `dcd5ae045e12...`

### payment-service
```bash
docker run -d --name payment-service --rm --network lunchpick_default \
  -p 8083:8083 \
  -e SERVER_PORT=8083 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_KIND=postgresql \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=payment \
  -e DB_USER=lunchpick \
  -e "DB_PASSWORD=P@ssw0rd$" \
  -e DDL_AUTO=update \
  -e SHOW_SQL=true \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e REDIS_DATABASE=3 \
  -e KAFKA_BROKERS=redis:6379 \
  -e MQ_SUBSCRIPTION_TOPIC=subscription-events \
  -e JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256 \
  -e JWT_ACCESS_TOKEN_VALIDITY=1800 \
  -e JWT_REFRESH_TOKEN_VALIDITY=86400 \
  -e "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://20.249.211.13:3000" \
  -e LOG_LEVEL_ROOT=INFO \
  -e LOG_LEVEL_APP=DEBUG \
  -e LOG_LEVEL_WEB=INFO \
  -e LOG_LEVEL_SQL=DEBUG \
  -e LOG_LEVEL_SQL_TYPE=TRACE \
  -e LOG_FILE_PATH=logs/payment-service.log \
  payment-service:latest
```
- Container ID: `6c98a5f81c58...`

## Step 3: 실행 확인

### docker ps 결과
```
NAMES                    STATUS                    PORTS
payment-service          Up About a minute (healthy)   8080/tcp, 0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp
recommendation-service   Up About a minute (healthy)   8080/tcp, 0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
member-service           Up About a minute (healthy)   8080/tcp, 0.0.0.0:8081->8081/tcp, [::]:8081->8081/tcp
```

### actuator/health 응답

**member-service (8081)**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP", "details": { "version": "7.4.8" } },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**recommendation-service (8082)**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP", "details": { "version": "7.4.8" } },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**payment-service (8083)**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP", "details": { "version": "7.4.8" } },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

## 최종 결과 요약

| 서비스 | 포트 | Docker 상태 | Actuator 상태 | DB | Redis |
|--------|------|-------------|---------------|----|-------|
| member-service | 8081 | healthy | UP | UP | UP |
| recommendation-service | 8082 | healthy | UP | UP | UP |
| payment-service | 8083 | healthy | UP | UP | UP |
