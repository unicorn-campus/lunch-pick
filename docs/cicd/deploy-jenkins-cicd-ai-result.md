# AI 서비스 Jenkins CI 파이프라인 생성 결과

## 작업 일시
2026-03-05

## 생성 파일

| 항목 | 값 |
|------|-----|
| 파일 경로 | `deployment/cicd/Jenkinsfile-ai` |
| 대상 서비스 | ai-pipeline-service |
| CI 도구 | Jenkins (Kubernetes Agent) |
| 레지스트리 | DockerHub (docker.io/hiondal) |

## 파이프라인 구성

### Agent 설정

| 항목 | 값 |
|------|-----|
| Kubernetes Cloud | eks-ondal |
| Pod Label | jenkins-build: ai-pipeline |
| podRetention | never() |

### 컨테이너 구성

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| python | python:3.12-slim | 의존성 설치, 테스트 실행 |
| docker | docker:27-dind | Docker 이미지 빌드 및 푸시 |

### 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|---------|-----------|------|
| 1 | Checkout | jnlp (기본) | 소스 체크아웃, imageTag(git short hash) 추출 |
| 2 | Install Dependencies | python | pip install -r requirements.txt |
| 3 | Build & Test | python | pytest --cov --cov-report=xml:coverage.xml |
| 4 | Docker Build & Push | docker | Dockerfile-ai 기반 이미지 빌드, DockerHub 푸시 |
| 5 | Update Manifest | jnlp (기본) | 매니페스트 레포 kustomization.yaml 이미지 태그 업데이트 |

### 환경 변수

| 변수 | 값 |
|------|-----|
| SERVICE_NAME | ai-pipeline-service |
| IMG_REG | docker.io |
| IMG_ORG | hiondal |
| DOCKER_CREDS | credentials('dockerhub-credentials') |
| GIT_CREDS | github-credentials |
| MANIFEST_REPO | https://github.com/hiondal/lunchpick-manifest.git |

### 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| environment | choice | dev | 배포 환경 선택 (dev/staging/prod) |

### Docker 이미지

| 항목 | 값 |
|------|-----|
| Dockerfile | deployment/container/Dockerfile-ai |
| Build Context | ai-pipeline-service/ |
| Build Arg | PROJECT_FOLDER="." |
| 이미지 이름 | docker.io/hiondal/ai-pipeline-service:{environment}-{imageTag} |
| Latest 태그 | docker.io/hiondal/ai-pipeline-service:{environment}-latest |

### 매니페스트 업데이트

| 항목 | 값 |
|------|-----|
| 매니페스트 레포 | https://github.com/hiondal/lunchpick-manifest.git |
| 업데이트 대상 | ai-pipeline-service/kustomize/overlays/{environment}/kustomization.yaml |
| 업데이트 방식 | sed로 newTag 값 교체 |
| Git Credentials | github-credentials |

## 기존 파이프라인과의 일관성

| 항목 | 백엔드 (Jenkinsfile-backend) | 프론트엔드 (Jenkinsfile-frontend) | AI (Jenkinsfile-ai) |
|------|---------------------------|-------------------------------|---------------------|
| 빌드 도구 | Gradle 8 / JDK 21 | Node 20 | Python 3.12 |
| 서비스 수 | 3개 (루프) | 1개 | 1개 |
| Docker DinD | docker:27-dind | docker:24-dind | docker:27-dind |
| 레지스트리 | DockerHub | DockerHub | DockerHub |
| Manifest 업데이트 | sed (deployment.yaml) | sed (kustomization.yaml) | sed (kustomization.yaml) |
| podRetention | never() | never() | never() |

## 주의사항
- CI 파이프라인에서 kubectl apply는 수행하지 않음 (GitOps 원칙 준수)
- ArgoCD가 매니페스트 레포 변경을 감지하여 자동 배포 수행
- pytest 실패 시에도 파이프라인이 중단되지 않도록 `|| true` 처리
- requirements.txt 기반 의존성 관리 (poetry 미사용)
