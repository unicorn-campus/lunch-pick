# frontend 프론트엔드 Jenkins CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | GCP |
| IMG_REG | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| IMG_NAME | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/frontend |
| JENKINS_CLOUD_NAME | gke-ondal |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| JENKINS_GIT_CREDENTIALS | github-credentials |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| FRONTEND_FRAMEWORK | React (Next.js 15) |
| FRONTEND_SERVICE | frontend |
| NODE_VERSION | 20 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `deployment/cicd/Jenkinsfile-frontend` | Jenkins 파이프라인 스크립트 |

## 파이프라인 구성

### 실행 환경 (Pod Template)

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| node | node:20-slim | Next.js 빌드 및 린트 |
| kaniko | gcr.io/kaniko-project/executor:debug | 컨테이너 이미지 빌드 및 GCR 푸시 |
| git | alpine/git:2.47.2 | 매니페스트 저장소 클론 및 업데이트 |
| sonar-scanner | sonarsource/sonar-scanner-cli:11 | SonarQube 코드 품질 분석 |

- **Jenkins Cloud**: gke-ondal
- **PIPELINE_ID**: frontend-${BUILD_NUMBER} (Job 간 Pod 라벨 충돌 방지)
- **Node 메모리**: limit 4Gi (Next.js OOMKilled 방지)
- **NODE_OPTIONS**: --max-old-space-size=3072

### 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|----------|-----------|------|
| 1 | Get Source | jnlp | 소스 코드 체크아웃 |
| 2 | Build & Test | node | cd frontend && npm ci && npm run build && lint |
| 3 | SonarQube Analysis & Quality Gate | sonar-scanner | 프론트엔드 코드 품질 분석 (스킵 가능) |
| 4 | Build & Push Images | kaniko | Kaniko로 컨테이너 이미지 빌드 및 GCR 푸시 |
| 5 | Update Manifest Repository | git | Kustomize 이미지 태그 업데이트 후 푸시 |
| 6 | Pipeline Complete | - | 완료 상태 알림 |

### 빌드 파라미터

| 파라미터 | 타입 | 기본값 | 옵션 |
|----------|------|--------|------|
| BRANCH | String | main | 자유 입력 |
| ENVIRONMENT | Choice | dev | dev, staging, prod |
| SKIP_SONARQUBE | Choice | true | false, true |

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {FRONTEND_SERVICE} | frontend |
| {NODE_VERSION} | 20 |
| {IMG_NAME} | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/frontend |
| {JENKINS_CLOUD_NAME} | gke-ondal |
| {JENKINS_GIT_CREDENTIALS} | github-credentials |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |

## Jenkins Job 정보
| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-frontend |
| Job URL | http://myjenkins.io/job/lunchpick-frontend/ |
| SCM | Git (https://github.com/hiondal/lunch-menu-recommender) |
| Script Path | deployment/cicd/Jenkinsfile-frontend |

## GKE 환경 적용 사항
- Kaniko 사용 (GKE에서 privileged 컨테이너 차단)
- alpine/git:2.47.2, sonarsource/sonar-scanner-cli:11 (GKE :latest 차단 정책)
- Kaniko 인증: base64 auth 방식으로 SA JSON 키 안전 처리
- CI/CD 분리: kubectl apply 없이 매니페스트 레포 image tag만 업데이트 (ArgoCD GitOps)
