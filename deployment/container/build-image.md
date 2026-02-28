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

---

## GCR (Google Artifact Registry) 빌드 수행 기록

### 수행 일시
2026-02-28

### 레지스트리 정보
- 레지스트리: GCP Artifact Registry
- REGISTRY_URL: `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick`
- 리전: asia-northeast3 (서울)
- 프로젝트: travel-planner-b7120

### 사전 준비 (최초 1회)

#### 1. Artifact Registry API 활성화
```bash
gcloud services enable artifactregistry.googleapis.com --project=travel-planner-b7120
```

#### 2. 저장소 생성
```bash
gcloud artifacts repositories create lunchpick \
  --repository-format=docker \
  --location=asia-northeast3 \
  --project=travel-planner-b7120 \
  --description="LunchPick container images"
```

### 빌드 및 푸시 절차

#### 1. GCR Docker 인증
```bash
gcloud auth configure-docker asia-northeast3-docker.pkg.dev --quiet
```

#### 2. 프론트엔드 이미지 빌드 (로컬에서 수행)
```bash
cd ~/workspace/lunch-menu-recommender
docker build \
  --platform linux/amd64 \
  --build-arg PROJECT_FOLDER="frontend" \
  --build-arg BUILD_FOLDER="deployment/container" \
  -f deployment/container/Dockerfile-frontend \
  -t asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/frontend:latest .
```

#### 3. GCR 푸시
```bash
docker push asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/frontend:latest
```

### 결과
| 항목 | 값 |
|------|-----|
| 이미지 경로 | `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/frontend:latest` |
| 다이제스트 | `sha256:db01137011ebd675887a0ad534b49986150024e3f97ff51ca203e8be61692e46` |
| 이미지 크기 | 약 73MB |
| 빌드 방식 | Next.js standalone (node:20-alpine 멀티스테이지) |
| 푸시 일시 | 2026-02-28T13:58:10 |

### 트러블슈팅 기록
- **GCP VM scope 부족**: VM의 Compute Engine 서비스 계정에 `cloud-platform` scope가 없어 VM에서 직접 푸시 불가 → 로컬에서 빌드 및 푸시로 전환
- **Artifact Registry API 비활성화**: 프로젝트에서 API가 비활성화 상태 → `gcloud services enable` 로 활성화 후 저장소 생성
- **prebuild 스크립트 우회**: `package.json`의 `prebuild`가 `bash ../tools/generate-runtime-env.sh`를 호출하지만 Docker 컨텍스트에서 상위 디렉토리 접근 불가 → `npx next build` 직접 호출로 prebuild 우회

---

## GCR 백엔드 이미지 빌드 및 푸시 수행 기록

### 수행 일시
2026-02-28

### 레지스트리 정보
- 레지스트리: GCP Artifact Registry
- REGISTRY_URL: `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick`
- 리전: asia-northeast3 (서울)
- 프로젝트: travel-planner-b7120

### 수행 절차

#### 1. bootJar 설정 확인
3개 서비스(member-service, recommendation-service, payment-service) 모두 build.gradle에 `bootJar { archiveFileName = '{service}.jar' }` 설정 확인 완료.

#### 2. Dockerfile-backend 확인
`deployment/container/Dockerfile-backend` 파일이 이미 올바른 내용으로 존재함 (amazoncorretto:21-alpine3.21-jdk 멀티스테이지 빌드).

#### 3. VM에서 JAR 빌드
```bash
ssh gcp 'cd ~/workspace/lunch-menu-recommender && chmod +x gradlew && ./gradlew clean bootJar -x test'
```
- 결과: BUILD SUCCESSFUL (1m 15s)
- 생성 파일: member-service/build/libs/member-service.jar, recommendation-service/build/libs/recommendation-service.jar, payment-service/build/libs/payment-service.jar

#### 4. VM에서 Docker 이미지 빌드
```bash
# 각 서비스별 실행
docker build --platform linux/amd64 \
  --build-arg BUILD_LIB_DIR="{service}/build/libs" \
  --build-arg ARTIFACTORY_FILE="{service}.jar" \
  -f deployment/container/Dockerfile-backend \
  -t {service}:latest .
```
- member-service: 빌드 성공
- recommendation-service: 빌드 성공 (병렬)
- payment-service: 빌드 성공 (병렬)

#### 5. VM → 로컬 이미지 전송
VM의 GCE 서비스 계정에 Artifact Registry scope 부족으로 VM에서 직접 push 불가.
`docker save | docker load` 파이프라인으로 로컬에 이미지 전송.
```bash
ssh gcp 'docker save {service}:latest' | docker load
```

#### 6. 로컬에서 GCR 인증 및 태그
```bash
gcloud auth configure-docker asia-northeast3-docker.pkg.dev --quiet
docker tag {service}:latest asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/{service}:latest
```

#### 7. GCR 푸시
```bash
docker push asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/{service}:latest
```

### 결과
| 서비스 | 이미지 경로 | 다이제스트 | 이미지 크기 |
|--------|-----------|-----------|-----------|
| member-service | `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/member-service:latest` | `sha256:39f8273d1ac7e10c276cf50f83e094692e0086a4be810f9a75934e8813ba5b3c` | 809MB |
| recommendation-service | `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/recommendation-service:latest` | `sha256:070de726dc4d9047e076b780c6f2c55045844deef6d0acd2080723f655ff18ee` | 824MB |
| payment-service | `asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/payment-service:latest` | `sha256:a6ab4852a7f4d1f61fcdd849ca19408c25b188d3d966abe193810580adefa3a1` | 809MB |

### 트러블슈팅 기록
- **GCP VM scope 부족 (push)**: VM의 GCE 서비스 계정에 `cloud-platform` scope가 없어 `gcloud artifacts` 명령 및 `docker push` 모두 `ACCESS_TOKEN_SCOPE_INSUFFICIENT` 오류 → VM에서 이미지 빌드 후 `docker save | docker load`로 로컬 전송, 로컬에서 `gcloud auth configure-docker` 후 push

---

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
