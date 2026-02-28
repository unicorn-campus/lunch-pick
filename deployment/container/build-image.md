# 컨테이너 이미지 빌드 가이드

## 이미지 레지스트리 정보
- 레지스트리: AWS ECR
- REGISTRY_URL: `851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick`
- 리전: ap-northeast-2

## 서비스별 이미지

| 서비스 | Dockerfile | 이미지 경로 |
|--------|-----------|------------|
| member-service | Dockerfile-backend | `lunchpick/member-service:latest` |
| recommendation-service | Dockerfile-backend | `lunchpick/recommendation-service:latest` |
| payment-service | Dockerfile-backend | `lunchpick/payment-service:latest` |
| frontend | Dockerfile-frontend | `lunchpick/frontend:latest` |
| ai-pipeline-service | Dockerfile-ai | `lunchpick/ai-pipeline-service:latest` |

## 빌드 절차

### 1. ECR 로그인
```bash
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin \
    851725211153.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. 백엔드 JAR 빌드
```bash
./gradlew clean bootJar -x test
```

### 3. 백엔드 이미지 빌드 (서비스별)
```bash
DOCKER_FILE=deployment/container/Dockerfile-backend
service={서비스명}  # member-service, recommendation-service, payment-service

docker build \
  --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="${service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="${service}.jar" \
  -f ${DOCKER_FILE} \
  -t ${service}:latest .
```

### 4. 프론트엔드 이미지 빌드
```bash
DOCKER_FILE=deployment/container/Dockerfile-frontend

docker build \
  --platform linux/amd64 \
  --build-arg PROJECT_FOLDER="frontend" \
  --build-arg BUILD_FOLDER="deployment/container" \
  -f ${DOCKER_FILE} \
  -t frontend:latest .
```

### 5. AI 서비스 이미지 빌드
```bash
DOCKER_FILE=deployment/container/Dockerfile-ai

docker build \
  --platform linux/amd64 \
  --build-arg PROJECT_FOLDER="ai-pipeline-service" \
  --build-arg EXPORT_PORT="8000" \
  -f ${DOCKER_FILE} \
  -t ai-pipeline-service:latest .
```

### 6. ECR 리포지토리 생성 (최초 1회)
```bash
for svc in member-service recommendation-service payment-service frontend ai-pipeline-service; do
  aws ecr describe-repositories \
    --repository-names "lunchpick/${svc}" \
    --region ap-northeast-2 2>/dev/null \
  || aws ecr create-repository \
    --repository-name "lunchpick/${svc}" \
    --region ap-northeast-2 \
    --image-scanning-configuration scanOnPush=true
done
```

### 7. 이미지 태그 & 푸시
```bash
REGISTRY=851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick

for svc in member-service recommendation-service payment-service frontend ai-pipeline-service; do
  docker tag ${svc}:latest ${REGISTRY}/${svc}:latest
  docker push ${REGISTRY}/${svc}:latest
done
```

## Dockerfile 설계 요약

### 백엔드 (Dockerfile-backend)
- 멀티스테이지 빌드: amazoncorretto:21-alpine3.21-jdk
- ARG: BUILD_LIB_DIR, ARTIFACTORY_FILE
- 비루트 사용자: appuser
- HEALTHCHECK: /actuator/health (8080)

### 프론트엔드 (Dockerfile-frontend)
- 멀티스테이지 빌드: node:20-alpine → Next.js standalone
- ARG: PROJECT_FOLDER, BUILD_FOLDER
- 비루트 사용자: nextjs
- HEALTHCHECK: / (3000)
- next.config.ts에 output: 'standalone' 필수

### AI 서비스 (Dockerfile-ai)
- 멀티스테이지 빌드: python:3.12-slim
- ARG: PROJECT_FOLDER, EXPORT_PORT
- 비루트 사용자: k8s
- HEALTHCHECK: /health (8000)
