# AI 서비스 컨테이너 실행 결과서

## 구성 환경
- 환경: docker run (VM 컨테이너 배포)
- VM: gcp (34.64.192.123)
- 실행 일시: 2026-02-28

## VM 접속 방법
```
ssh gcp
```

## 실행된 컨테이너

| 항목 | 값 |
|------|---|
| 서비스명 | ai-pipeline-service |
| 이미지 | asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/ai-pipeline-service:latest |
| 포트 매핑 | 8084:8000 |
| 네트워크 | lunch-menu-recommender_default |
| 상태 | Running |

## 환경변수 설정
| 변수 | 값 |
|------|---|
| APP_ENV | development |
| APP_PORT | 8000 |
| PRIMARY_MODEL_ID | openai/gpt-oss-120b |
| PRIMARY_MODEL_PROVIDER | openai |
| OPENAI_API_BASE | https://api.groq.com/openai/v1 |
| REDIS_HOST | redis |
| REDIS_PORT | 6379 |
| REDIS_DB | 4 |

## Health Check 결과
- [x] `docker ps | grep ai-pipeline-service` 확인: 컨테이너 실행 중
- [x] `{"status":"ok","circuit_breaker":{"state":"CLOSED","failure_count":0},"redis":{"connected":true,"db":4},"llm_provider":"anthropic","uptime_seconds":124}`

## 실행 명령어
```bash
REGISTRY=asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick
docker run -d --name ai-pipeline-service --rm --network lunch-menu-recommender_default \
  -p 8084:8000 \
  -e APP_ENV=development -e APP_PORT=8000 -e LOG_LEVEL=INFO \
  -e PRIMARY_MODEL_ID=openai/gpt-oss-120b \
  -e PRIMARY_MODEL_PROVIDER=openai \
  -e PRIMARY_MODEL_TEMPERATURE=0.3 -e PRIMARY_MODEL_MAX_TOKENS=1024 \
  -e COLDSTART_MODEL_ID=openai/gpt-oss-120b \
  -e COLDSTART_MODEL_PROVIDER=openai \
  -e COLDSTART_MODEL_TEMPERATURE=0.4 -e COLDSTART_MODEL_MAX_TOKENS=1024 \
  -e REASON_MODEL_ID=openai/gpt-oss-120b \
  -e REASON_MODEL_PROVIDER=openai \
  -e REASON_MODEL_TEMPERATURE=0.5 -e REASON_MODEL_MAX_TOKENS=512 \
  -e OPENAI_API_KEY=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 \
  -e OPENAI_API_BASE=https://api.groq.com/openai/v1 \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 -e REDIS_DB=4 \
  -e CB_FAILURE_THRESHOLD=5 -e CB_RECOVERY_TIMEOUT=60.0 \
  -e RATE_LIMIT_GLOBAL_PER_MINUTE=500 \
  ${REGISTRY}/ai-pipeline-service:latest
```
