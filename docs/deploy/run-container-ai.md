# AI Pipeline Service 컨테이너 실행 결과

## 실행 환경
- VM: Azure (azureuser@20.249.211.13)
- 서비스: ai-pipeline-service (FastAPI, Python 3.12)
- 포트: 8084
- 네트워크: lunchpick_default

## Step 1. Docker 이미지 빌드

```bash
ssh azure "cd ~/workspace/lunchpick && \
  docker build --platform linux/amd64 \
    --build-arg PROJECT_FOLDER=ai-pipeline-service \
    --build-arg EXPORT_PORT=8084 \
    -f deployment/container/Dockerfile-ai \
    -t ai-pipeline-service:latest ."
```

### 빌드 결과
```
#8 [builder 4/5] RUN pip install --no-cache-dir --prefix=/install -r requirements.txt
#8 CACHED
#9 [builder 5/5] COPY ai-pipeline-service .
#9 CACHED
#14 [stage-1 6/6] RUN chown -R k8s:k8s /home/k8s/app
#14 DONE 0.7s
#15 exporting to image
#15 DONE 6.8s
```
- 빌드 성공: `ai-pipeline-service:latest`
- 이미지 ID: sha256:e98bc1f2541af15175978679758ddc983ed4cf0868946fd6feb6cc7772fdc7ec

## Step 2. .env.ai 파일 준비 (호스트 치환)

```bash
ssh azure "cp ~/workspace/lunchpick/.env ~/workspace/lunchpick/.env.ai && \
  sed -i 's/DB_HOST=.*/DB_HOST=postgres/' ~/workspace/lunchpick/.env.ai && \
  sed -i 's/REDIS_HOST=.*/REDIS_HOST=redis/' ~/workspace/lunchpick/.env.ai && \
  sed -i 's/DB_PORT=.*/DB_PORT=5432/' ~/workspace/lunchpick/.env.ai && \
  sed -i 's/REDIS_PORT=.*/REDIS_PORT=6379/' ~/workspace/lunchpick/.env.ai && \
  grep -q '^APP_PORT=' ~/workspace/lunchpick/.env.ai || echo 'APP_PORT=8084' >> ~/workspace/lunchpick/.env.ai"
```

### 치환 결과
| 항목 | 원본 | 치환 후 |
|------|------|---------|
| DB_HOST | localhost | postgres |
| DB_PORT | 15432 | 5432 |
| REDIS_HOST | localhost | redis |
| REDIS_PORT | 16379 | 6379 |
| APP_PORT | (없음) | 8084 (추가) |

## Step 3. 컨테이너 실행

```bash
ssh azure "docker rm -f ai-pipeline-service 2>/dev/null; \
  docker run -d --name ai-pipeline-service --network lunchpick_default \
  -p 8084:8084 \
  --env-file ~/workspace/lunchpick/.env.ai \
  ai-pipeline-service:latest"
```

### 실행 결과
```
106b75de17544559b60601fc7b74318511fa5f12a0e44cb8d751a31cb63575bc
```

## Step 4. 실행 확인

```bash
ssh azure "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep ai-pipeline"
```

```
ai-pipeline-service   Up About a minute (health: starting)   0.0.0.0:8084->8084/tcp, [::]:8084->8084/tcp
```

## Step 5. Health Check

```bash
ssh azure "sleep 20 && curl -s http://localhost:8084/health"
```

### 응답
```json
{
  "status": "ok",
  "circuit_breaker": {
    "state": "CLOSED",
    "failure_count": 0
  },
  "redis": {
    "connected": true,
    "db": 4
  },
  "llm_provider": "anthropic",
  "uptime_seconds": 44
}
```

## Step 6. 서비스 로그 확인

```bash
ssh azure "docker logs ai-pipeline-service --tail 20"
```

```
INFO:     Started server process [7]
INFO:     Waiting for application startup.
LLM API 키 미설정 → Mock LLM 엔진 모드
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8084 (Press CTRL+C to quit)
INFO:     172.19.0.1:60852 - "GET /health HTTP/1.1" 200 OK
```

## 결과 요약

| 항목 | 결과 |
|------|------|
| 이미지 빌드 | 성공 |
| 컨테이너 기동 | 성공 |
| 네트워크 연결 | lunchpick_default 참여 |
| Redis 연결 | 성공 (DB 4) |
| Health Check | 200 OK (`status: ok`) |
| LLM 모드 | Mock LLM (API 키 미설정) |

> 비고: LLM API 키가 .env에 미설정되어 Mock LLM 엔진 모드로 동작 중.
> 실제 AI 추천 기능 활성화를 위해서는 ANTHROPIC_API_KEY 또는 OPENAI_API_KEY 환경변수 설정 필요.
