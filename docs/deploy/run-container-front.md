# 프론트엔드 컨테이너 실행 결과

## 실행 환경
- VM: Azure (azureuser@20.249.211.13)
- 실행일: 2026-02-28
- 프레임워크: Next.js 15 (standalone mode)
- 이미지: frontend:latest

## Step 1: Docker 이미지 빌드

```bash
ssh azure "cd ~/workspace/lunchpick && \
  docker build --platform linux/amd64 \
    --build-arg PROJECT_FOLDER=frontend \
    --build-arg BUILD_FOLDER=deployment/container \
    -f deployment/container/Dockerfile-frontend \
    -t frontend:latest ."
```

### 결과
- 캐시된 레이어 활용으로 빠르게 완료
- 이미지 태그: `frontend:latest`
- 빌드 상태: 성공 (DONE)

## Step 2: runtime-env.js 준비

```bash
ssh azure "mkdir -p ~/frontend/public && cat > ~/frontend/public/runtime-env.js << 'ENVEOF'
window.__runtime_config__ = {
  APP_ENV: "production",
  API_GROUP: "/api/v1",
  MEMBER_HOST: "http://20.249.211.13:8081",
  RECOMMENDATION_HOST: "http://20.249.211.13:8082",
  PAYMENT_HOST: "http://20.249.211.13:8083",
  AI_HOST: "http://20.249.211.13:8084",
  KAKAO_CLIENT_ID: "d34722dff8545446e14b2616bb62c6b0",
  KAKAO_API_KEY: "b588f91e780efc914b282ad7e3688e01",
  KAKAO_JS_KEY: "298b6fef9e85e51d5ffbdbbc782fa3c5"
};
ENVEOF"
```

### 결과
- 파일 경로: `~/frontend/public/runtime-env.js`
- VM IP 기반 백엔드 호스트 설정 완료
- 생성 상태: 성공

## Step 3: 컨테이너 실행

```bash
ssh azure "docker rm -f frontend 2>/dev/null; \
  docker run -d --name frontend --network lunchpick_default \
  -p 3000:3000 \
  -v ~/frontend/public/runtime-env.js:/app/public/runtime-env.js \
  frontend:latest"
```

### 결과
- 컨테이너 ID: `a0c065ef2ac0dbc4d04aac93a0cb812a16e0a70f`
- 네트워크: `lunchpick_default`
- 포트 바인딩: `0.0.0.0:3000->3000/tcp`
- 볼륨 마운트: `~/frontend/public/runtime-env.js:/app/public/runtime-env.js`

## Step 4: 실행 확인

### 컨테이너 상태
```
NAMES      STATUS                    PORTS
frontend   Up 56 seconds (health: starting)   0.0.0.0:3000->3000/tcp, [::]:3000->3000/tcp
```

### 컨테이너 로그
```
▲ Next.js 16.1.6
- Local:         http://localhost:3000
- Network:       http://0.0.0.0:3000

✓ Starting...
✓ Ready in 103ms
```

### HTTP 헬스 체크
```bash
ssh azure "sleep 15 && curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/"
```
- 응답 코드: **200**
- 상태: 정상

## 최종 결과

| 항목 | 결과 |
|------|------|
| 이미지 빌드 | 성공 |
| runtime-env.js 생성 | 성공 |
| 컨테이너 실행 | 성공 |
| HTTP 응답 | 200 OK |
| 접속 URL | http://20.249.211.13:3000 |
