# Jenkins CI 파이프라인 - Frontend 구성 결과서

## 개요

| 항목 | 값 |
|------|-----|
| 서비스명 | frontend |
| CI 도구 | Jenkins (Kubernetes Agent on AKS) |
| 파이프라인 파일 | `deployment/cicd/Jenkinsfile-frontend` |
| Dockerfile | `deployment/container/Dockerfile-frontend` |
| 이미지 레지스트리 | DockerHub (`docker.io/hiondal/lunchpick-frontend`) |
| Manifest 저장소 | https://github.com/hiondal/lunchpick-manifest.git |
| Jenkins Job 이름 | `lunchpick-frontend` |
| 소스 저장소 | https://github.com/hiondal/lunch-menu-recommender |
| 프레임워크 | React (Next.js 15) / Node.js 20 |

## Jenkins Job 생성 결과

| 항목 | 결과 |
|------|------|
| Job 이름 | lunchpick-frontend |
| Job 유형 | Pipeline (flow-definition) |
| 생성 방법 | Jenkins REST API (createItem) |
| HTTP 응답 | 200 OK |
| Job URL | http://myjenkins.io/job/lunchpick-frontend/ |

## 파이프라인 스테이지 구성

```
Get Source -> Build & Test -> SonarQube Analysis -> Build & Push Images -> Update Manifest -> Pipeline Complete
```

### 1. Get Source
- SCM에서 소스코드를 체크아웃한다.

### 2. Build & Test
- `node:20-slim` 컨테이너에서 실행한다.
- `npm ci`로 의존성을 설치한다.
- `NODE_OPTIONS="--max-old-space-size=3072" npm run build`로 Next.js 프로덕션 빌드를 수행한다.
- `npm run lint || true`로 ESLint 검사를 실행한다 (실패해도 파이프라인 계속 진행).

### 3. SonarQube Analysis & Quality Gate
- `SKIP_SONARQUBE` 파라미터가 `true`이면 스킵한다.
- `sonar-scanner-cli` 컨테이너에서 SonarQube 정적 분석을 수행한다.
- Quality Gate 결과가 실패해도 파이프라인을 중단하지 않는다.
- SonarQube 연결 실패 시에도 파이프라인을 계속 진행한다.

### 4. Build & Push Images
- `podman` 컨테이너에서 컨테이너 이미지를 빌드한다.
- Dockerfile: `deployment/container/Dockerfile-frontend`
- 빌드 인자: `PROJECT_FOLDER="frontend"`, `BUILD_FOLDER="deployment/container"`
- 이미지 태그 형식: `docker.io/hiondal/lunchpick-frontend:{environment}-{yyyyMMddHHmmss}`
- DockerHub 인증 후 이미지를 푸시한다.

### 5. Update Manifest Repository
- `alpine/git` 컨테이너에서 매니페스트 저장소를 클론한다.
- kustomize를 다운로드하여 `frontend/kustomize/overlays/{environment}` 디렉토리에서 이미지 태그를 업데이트한다.
- 변경사항을 커밋하고 푸시한다.
- ArgoCD가 변경된 매니페스트를 감지하여 자동 배포를 수행한다.

### 6. Pipeline Complete
- 파이프라인 실행 결과를 로깅한다.

## Kubernetes Agent Pod 구성

| 컨테이너 | 이미지 | 용도 | CPU (req/limit) | Memory (req/limit) |
|-----------|--------|------|-----------------|-------------------|
| node | `node:20-slim` | npm 설치, 빌드, 린트 | 400m / 2000m | 1Gi / 4Gi |
| podman | `mgoltzsche/podman` | 컨테이너 이미지 빌드 및 푸시 | 400m / 2000m | 2Gi / 4Gi |
| git | `alpine/git:latest` | 매니페스트 저장소 업데이트 | 100m / 300m | 256Mi / 512Mi |
| sonar-scanner | `sonarsource/sonar-scanner-cli:latest` | SonarQube 정적 분석 | 200m / 1000m | 512Mi / 2Gi |

- Kubernetes Cloud: `aks-ondal`
- Service Account: `jenkins`
- Pod Retention: `never()` (빌드 완료 후 즉시 삭제)
- Active Deadline: 3600초 (1시간)

## Jenkins Credentials 요구사항

| Credential ID | 유형 | 용도 |
|---------------|------|------|
| `imagereg-credentials` | Username/Password | 이미지 레지스트리 로그인 |
| `dockerhub-credentials` | Username/Password | DockerHub 로그인 |
| `github-credentials` | Username/Password | 소스/매니페스트 저장소 접근 |

## 파라미터

| 파라미터 | 유형 | 기본값 | 설명 |
|----------|------|--------|------|
| BRANCH | String | main | 빌드 대상 브랜치 |
| ENVIRONMENT | Choice | dev | 배포 환경 (dev / staging / prod) |
| SKIP_SONARQUBE | Choice | false | SonarQube 분석 스킵 여부 (false / true) |

## CI/CD 흐름도

```
[개발자 Push] -> [Jenkins 파이프라인 트리거]
     -> Get Source (checkout scm)
     -> Build & Test (npm ci, next build, eslint)
     -> SonarQube Analysis (선택적)
     -> Build & Push Images (podman build & push to DockerHub)
     -> Update Manifest Repository (kustomize edit set image)
          -> [ArgoCD 자동 감지 & 배포]
```

## 주의사항
1. CI 파이프라인에서 `kubectl apply`를 직접 수행하지 않는다 (GitOps 원칙 준수).
2. 매니페스트 저장소에 `frontend/kustomize/overlays/{environment}` 디렉토리가 사전 생성되어 있어야 한다.
3. Jenkins에 `imagereg-credentials`, `dockerhub-credentials`, `github-credentials` Credentials가 사전 등록되어 있어야 한다.
4. Kubernetes cloud `aks-ondal`이 Jenkins에 설정되어 있어야 한다.
5. SonarQube 서버가 Jenkins에 `SonarQube`라는 이름으로 설정되어 있어야 SonarQube 분석이 동작한다.
