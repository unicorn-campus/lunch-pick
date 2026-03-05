# Jenkins CI 파이프라인 - Frontend 구성 결과서

## 개요

| 항목 | 값 |
|------|-----|
| 서비스명 | frontend |
| CI 도구 | Jenkins (Kubernetes Agent) |
| 파이프라인 파일 | `deployment/cicd/Jenkinsfile-frontend` |
| Dockerfile | `deployment/container/Dockerfile-frontend` |
| 이미지 레지스트리 | DockerHub (`docker.io/hiondal/frontend`) |
| Manifest 저장소 | https://github.com/hiondal/lunchpick-manifest.git |

## 파이프라인 스테이지 구성

```
Checkout → Install Dependencies → Lint → Build → Docker Build & Push → Update Manifest
```

### 1. Checkout
- SCM에서 소스코드를 체크아웃한다.
- `git rev-parse --short HEAD`로 이미지 태그용 커밋 해시를 추출한다.

### 2. Install Dependencies
- `node:20-slim` 컨테이너에서 `npm ci`로 의존성을 설치한다.
- 작업 디렉토리: `frontend/`

### 3. Lint
- ESLint를 실행하여 코드 품질을 검사한다.
- 린트 실패 시에도 파이프라인을 중단하지 않는다 (`|| true`).

### 4. Build
- `npx next build`로 Next.js 프로덕션 빌드를 수행한다.

### 5. Docker Build & Push
- `docker:24-dind` 컨테이너에서 Docker 이미지를 빌드한다.
- Dockerfile: `deployment/container/Dockerfile-frontend`
- 빌드 인자: `--build-arg PROJECT_FOLDER="." --build-arg BUILD_FOLDER="deployment/container"`
- 이미지 태그 형식: `docker.io/hiondal/frontend:{environment}-{commitHash}`
- latest 태그도 함께 푸시: `docker.io/hiondal/frontend:{environment}-latest`
- DockerHub 인증 후 푸시, 완료 후 로그아웃 처리

### 6. Update Manifest
- GitOps 패턴에 따라 Manifest 저장소의 이미지 태그를 업데이트한다.
- Kustomization 또는 Deployment YAML에서 이미지 태그를 교체한다.
- 변경사항이 있을 때만 커밋/푸시하여 불필요한 커밋을 방지한다.
- ArgoCD가 변경된 Manifest를 감지하여 자동 배포를 수행한다.

## Kubernetes Agent Pod 구성

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| node | `node:20-slim` | npm 의존성 설치, 린트, 빌드 |
| docker | `docker:24-dind` | Docker 이미지 빌드 및 푸시 |

- Kubernetes Cloud: `eks-ondal`
- Pod Retention: `never()` (빌드 완료 후 즉시 삭제)

## Jenkins Credentials 요구사항

| Credential ID | 유형 | 용도 |
|---------------|------|------|
| `dockerhub-credentials` | Username/Password | DockerHub 로그인 |
| `github-credentials` | Username/Password | Manifest 저장소 클론/푸시 |

## 파라미터

| 파라미터 | 유형 | 기본값 | 설명 |
|----------|------|--------|------|
| environment | Choice | dev | 배포 환경 (dev / staging / prod) |

## CI/CD 흐름도

```
[개발자 Push] → [Jenkins 파이프라인 트리거]
     → Checkout
     → npm ci (의존성 설치)
     → npm run lint (코드 품질)
     → next build (프로덕션 빌드)
     → Docker Build & Push (DockerHub)
     → Manifest Repo 이미지 태그 업데이트
          → [ArgoCD 자동 감지 & 배포]
```

## 주의사항
- CI 파이프라인에서 `kubectl apply`를 직접 수행하지 않는다 (GitOps 원칙 준수).
- Docker Build 단계에서 빌드 컨텍스트는 `frontend/` 디렉토리이다.
- Manifest 저장소 구조에 따라 `Update Manifest` 단계의 sed 경로를 조정해야 할 수 있다.
