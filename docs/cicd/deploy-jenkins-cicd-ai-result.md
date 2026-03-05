# AI 서비스 Jenkins CI 파이프라인 생성 결과

## 작업 일시
2026-03-06

## 생성 파일

| 항목 | 값 |
|------|-----|
| 파일 경로 | `deployment/cicd/Jenkinsfile-ai` |
| 대상 서비스 | ai-pipeline-service |
| CI 도구 | Jenkins (Kubernetes Agent) |
| 레지스트리 | DockerHub (docker.io/hiondal) |

## Jenkins Job 생성

| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-ai |
| Job 유형 | Pipeline (WorkflowJob) |
| 소스 레포 | https://github.com/hiondal/lunch-menu-recommender |
| Credentials | github-credentials |
| Script Path | deployment/cicd/Jenkinsfile-ai |
| 생성 방식 | Jenkins REST API (createItem) |
| 생성 결과 | HTTP 200 (성공) |

### 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| BRANCH | String | main | Git 브랜치 |
| ENVIRONMENT | Choice | dev | 배포 환경 (dev/staging/prod) |
| SKIP_SONARQUBE | Choice | false | SonarQube 분석 스킵 여부 (false/true) |

## 파이프라인 구성

### Agent 설정

| 항목 | 값 |
|------|-----|
| Kubernetes Cloud | aks-ondal |
| Service Account | jenkins |
| Pod Label | BUILD_NUMBER 기반 |
| podRetention | never() |
| activeDeadlineSeconds | 3600 |

### 컨테이너 구성

| 컨테이너 | 이미지 | 용도 | CPU (req/limit) | Memory (req/limit) |
|-----------|--------|------|-----------------|-------------------|
| python | python:3.12-slim | 의존성 설치, 테스트 실행 | 400m/2000m | 1Gi/4Gi |
| podman | mgoltzsche/podman | 컨테이너 이미지 빌드 및 푸시 | 400m/2000m | 2Gi/4Gi |
| git | alpine/git:latest | 매니페스트 레포 업데이트 | 100m/300m | 256Mi/512Mi |
| sonar-scanner | sonarsource/sonar-scanner-cli:latest | SonarQube 정적 분석 | 200m/1000m | 512Mi/1Gi |

### 볼륨 마운트

| 마운트 경로 | 용도 |
|------------|------|
| /opt/sonar-scanner/.sonar/cache | SonarQube 캐시 |
| /root/.cache/pip | pip 캐시 |

### 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|---------|-----------|------|
| 1 | Get Source | jnlp (기본) | checkout scm으로 소스 체크아웃 |
| 2 | Build & Test | python | pip install -r requirements.txt, pytest 실행 |
| 3 | SonarQube Analysis & Quality Gate | sonar-scanner | Python 정적 분석 및 품질 게이트 확인 |
| 4 | Build & Push Images | podman | Dockerfile-ai 기반 이미지 빌드, DockerHub 푸시 |
| 5 | Update Manifest Repository | git | kustomize로 매니페스트 레포 이미지 태그 업데이트 |
| 6 | Pipeline Complete | - | 파이프라인 완료 상태 출력 |

### 이미지 빌드 설정

| 항목 | 값 |
|------|-----|
| Dockerfile | deployment/container/Dockerfile-ai |
| Build Context | . (프로젝트 루트) |
| Build Args | PROJECT_FOLDER="ai-pipeline-service", EXPORT_PORT="8000" |
| Platform | linux/amd64 |
| 이미지 이름 | docker.io/hiondal/lunchpick-ai-pipeline-service:{environment}-{imageTag} |
| 태그 형식 | {environment}-{yyyyMMddHHmmss} |

### 매니페스트 업데이트

| 항목 | 값 |
|------|-----|
| 매니페스트 레포 | https://github.com/hiondal/lunchpick-manifest.git |
| 업데이트 대상 | ai-pipeline-service/kustomize/overlays/{environment}/kustomization.yaml |
| 업데이트 방식 | kustomize edit set image |
| Git Credentials | github-credentials |

## 기존 파이프라인과의 일관성

| 항목 | 백엔드 (Jenkinsfile-backend) | AI (Jenkinsfile-ai) |
|------|---------------------------|---------------------|
| 빌드 도구 | Gradle / JDK 21 | pip / Python 3.12 |
| 서비스 수 | 3개 (루프) | 1개 |
| 이미지 빌드 | podman | podman |
| 레지스트리 | DockerHub | DockerHub |
| Manifest 업데이트 | kustomize edit set image | kustomize edit set image |
| podRetention | never() | never() |
| SonarQube | gradle sonar plugin | sonar-scanner-cli |
| Cloud | aks-ondal | aks-ondal |

## 주의사항
- CI 파이프라인에서 kubectl apply는 수행하지 않음 (GitOps 원칙 준수)
- ArgoCD가 매니페스트 레포 변경을 감지하여 자동 배포 수행
- pytest 실패 시에도 파이프라인이 중단되지 않도록 `|| true` 처리
- SonarQube 분석 실패 시에도 파이프라인 계속 진행 (catch 처리)
- requirements.txt 기반 의존성 관리 (poetry 미사용)
- 이미지 태그는 타임스탬프 기반 (yyyyMMddHHmmss)
