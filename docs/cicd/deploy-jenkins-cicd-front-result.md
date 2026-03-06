# frontend 프론트엔드 Jenkins CI 파이프라인 결과서

## 1. 실행 환경 정보

| 항목 | 값 |
|------|-----|
| CLOUD | GCP |
| CI 도구 | Jenkins (Kubernetes Agent on GKE) |
| IMG_REG | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| IMG_NAME | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/frontend |
| JENKINS_CLOUD_NAME | gke-ondal |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| JENKINS_GIT_CREDENTIALS | github-credentials |
| 소스 저장소 | https://github.com/hiondal/lunch-menu-recommender |
| 작성일 | 2026-03-06 |

## 2. 서비스 정보

| 항목 | 값 |
|------|-----|
| FRONTEND_FRAMEWORK | React (Next.js 15) |
| FRONTEND_SERVICE | frontend |
| NODE_VERSION | 20 |

## 3. 생성/수정 파일

| 파일 | 설명 |
|------|------|
| `deployment/cicd/Jenkinsfile-frontend` | Jenkins 파이프라인 스크립트 |

## 4. Jenkinsfile 수정 내역

| 항목 | 변경 전 | 변경 후 | 사유 |
|------|---------|---------|------|
| git 이미지 | `alpine/git:latest` | `alpine/git:2.47.2` | GKE `:latest` 태그 차단 정책 준수 |
| sonar-scanner 이미지 | `sonarsource/sonar-scanner-cli:latest` | `sonarsource/sonar-scanner-cli:11` | GKE `:latest` 태그 차단 정책 준수 |
| Kaniko GCR 인증 | echo JSON (config.json 직접 구성) | GOOGLE_APPLICATION_CREDENTIALS 파일 방식 | GCR SA JSON 키 특수문자 파싱 오류 해결 |
| ephemeral-storage (kaniko) | 미설정 (기본 1Gi) | request 5Gi / limit 20Gi | Next.js Docker 이미지 빌드 시 Pod Evicted 방지 |
| ephemeral-storage (node) | 미설정 (기본 1Gi) | request 2Gi / limit 10Gi | npm ci + Next.js 빌드 시 임시 저장소 부족 방지 |
| PIPELINE_ID | `${env.BUILD_NUMBER}` | `frontend-${env.BUILD_NUMBER}` | Pod 라벨 고유화 (서비스 간 충돌 방지) |

## 5. 파이프라인 구성

### 5.1 실행 환경 (Pod Template)

| 컨테이너 | 이미지 | 용도 | CPU (req/limit) | Memory (req/limit) | Ephemeral Storage (req/limit) |
|-----------|--------|------|-----------------|-------------------|-------------------------------|
| node | `node:20-slim` | npm 설치, 빌드, 린트 | 400m / 2000m | 1Gi / 4Gi | 2Gi / 10Gi |
| kaniko | `gcr.io/kaniko-project/executor:debug` | 컨테이너 이미지 빌드 및 GCR 푸시 | 400m / 2000m | 2Gi / 4Gi | 5Gi / 20Gi |
| git | `alpine/git:2.47.2` | 매니페스트 저장소 업데이트 | 100m / 300m | 256Mi / 512Mi | - |
| sonar-scanner | `sonarsource/sonar-scanner-cli:11` | SonarQube 정적 분석 | 200m / 1000m | 512Mi / 2Gi | - |

- **Jenkins Cloud**: gke-ondal
- **PIPELINE_ID**: frontend-${BUILD_NUMBER} (Job 간 Pod 라벨 충돌 방지)
- **Service Account**: jenkins
- **Pod Retention**: `never()` (빌드 완료 후 즉시 삭제)
- **Active Deadline**: 3600초 (1시간)
- **Namespace**: jenkins

### 5.2 파이프라인 스테이지

```
Get Source -> Build & Test -> SonarQube Analysis -> Build & Push Images -> Update Manifest -> Pipeline Complete
```

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|----------|-----------|------|
| 1 | Get Source | jnlp | 소스 코드 체크아웃 |
| 2 | Build & Test | node | cd frontend && npm ci && npm run build && lint |
| 3 | SonarQube Analysis & Quality Gate | sonar-scanner | 프론트엔드 코드 품질 분석 (스킵 가능) |
| 4 | Build & Push Images | kaniko | Kaniko로 컨테이너 이미지 빌드 및 GCR 푸시 |
| 5 | Update Manifest Repository | git | Kustomize 이미지 태그 업데이트 후 푸시 |
| 6 | Pipeline Complete | - | 완료 상태 알림 |

### 5.3 빌드 파라미터

| 파라미터 | 타입 | 기본값 | 옵션 | 설명 |
|----------|------|--------|------|------|
| BRANCH | String | main | 자유 입력 | 빌드 대상 브랜치 |
| ENVIRONMENT | Choice | dev | dev, staging, prod | 배포 환경 |
| SKIP_SONARQUBE | Choice | false | false, true | SonarQube 분석 스킵 여부 |

## 6. Jenkins Job 생성 결과

| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-frontend |
| Job 유형 | Pipeline (CpsScmFlowDefinition) |
| Job URL | http://myjenkins.io/job/lunchpick-frontend/ |
| SCM | Git (https://github.com/hiondal/lunch-menu-recommender) |
| Credentials | github-credentials |
| Script Path | deployment/cicd/Jenkinsfile-frontend |
| 생성/업데이트 방법 | Jenkins REST API (config.xml POST) |
| HTTP 응답 | 200 OK |

## 7. 빌드 실행 결과

| 항목 | 값 |
|------|-----|
| 빌드 번호 | #12 |
| 빌드 결과 | **SUCCESS** |
| 소요 시간 | 308초 (약 5분 9초) |
| 빌드 URL | http://myjenkins.io/job/lunchpick-frontend/12/ |
| 빌드 파라미터 | BRANCH=main, ENVIRONMENT=dev, SKIP_SONARQUBE=true |
| 생성된 이미지 | `asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/frontend:dev-20260306024039` |
| 매니페스트 업데이트 | `frontend/kustomize/overlays/dev` 이미지 태그 업데이트 완료 |

### 7.1 스테이지별 실행 확인

| 단계 | 상태 | 비고 |
|------|------|------|
| Get Source | 완료 | 소스 체크아웃 성공 |
| Build & Test | 완료 | npm ci + Next.js 빌드 + ESLint 완료 (lint warning 있으나 통과) |
| SonarQube Analysis | 스킵 | SKIP_SONARQUBE=true 설정 |
| Build & Push Images | 완료 | Kaniko로 이미지 빌드 후 GCR 푸시 성공 |
| Update Manifest | 완료 | kustomize edit set image 후 매니페스트 저장소 푸시 완료 |
| Pipeline Complete | 완료 | "Pipeline completed successfully!" |

## 8. GCP IAM 수정 사항

| 항목 | 내용 |
|------|------|
| 서비스 계정 | `sa-artifact-registry@lunchpick-489007.iam.gserviceaccount.com` |
| 추가 역할 | `roles/artifactregistry.writer` |
| 사유 | 서비스 계정 재생성 후 IAM 바인딩 누락으로 Artifact Registry 푸시 권한 부재 |

## 9. Jenkins Credentials 요구사항

| Credential ID | 유형 | 용도 |
|---------------|------|------|
| `github-credentials` | Username/Password | 소스/매니페스트 저장소 접근 |
| `imagereg-credentials` | Username/Password | GCP Artifact Registry 로그인 (username: `_json_key`, password: SA JSON 키) |
| `dockerhub-credentials` | Username/Password | DockerHub 로그인 (base image pull rate limit 회피) |

## 10. CI/CD 흐름도

```
[개발자 Push] -> [Jenkins 파이프라인 트리거]
     -> Get Source (checkout scm)
     -> Build & Test (npm ci, next build, eslint)
     -> SonarQube Analysis (선택적)
     -> Build & Push Images (Kaniko build & push to GCR)
     -> Update Manifest Repository (kustomize edit set image)
          -> [ArgoCD 자동 감지 & 배포]
```

## 11. 트러블슈팅 이력

### 11.1 Kaniko config.json JSON 파싱 오류
- **증상**: `parsing config file (/kaniko/.docker/config.json): invalid character 't' after object key:value pair`
- **원인**: GCR 서비스 계정 JSON 키에 포함된 특수문자(쌍따옴표, 개행, 백슬래시)가 shell echo 방식으로 config.json 구성 시 JSON 구조를 깨뜨림
- **해결**: `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 파일 방식으로 GCR 인증 전환

### 11.2 GCP IAM 권한 부재
- **증상**: `DENIED: Permission 'artifactregistry.repositories.uploadArtifacts' denied on resource`
- **원인**: 서비스 계정 `sa-artifact-registry` 재생성 후 `roles/artifactregistry.writer` IAM 바인딩 누락
- **해결**: `gcloud projects add-iam-policy-binding` 명령으로 역할 재할당

### 11.3 Kaniko Pod Evicted (ephemeral-storage 초과)
- **증상**: `Container kaniko exceeded its local ephemeral storage limit`
- **원인**: Next.js Docker 이미지 multi-stage 빌드 시 대량 임시 파일 생성으로 기본 ephemeral-storage 한도 초과
- **해결**: Pod YAML에 kaniko ephemeral-storage request 5Gi / limit 20Gi, node request 2Gi / limit 10Gi 설정

## 12. 주의사항

1. CI 파이프라인에서 `kubectl apply`를 직접 수행하지 않는다 (GitOps 원칙 준수).
2. 매니페스트 저장소에 `frontend/kustomize/overlays/{environment}` 디렉토리가 사전 생성되어 있어야 한다.
3. Jenkins에 `imagereg-credentials`, `dockerhub-credentials`, `github-credentials` Credentials가 사전 등록되어 있어야 한다.
4. Kubernetes cloud `gke-ondal`이 Jenkins에 설정되어 있어야 한다.
5. GCP 서비스 계정 `sa-artifact-registry`에 `roles/artifactregistry.writer` 역할이 부여되어 있어야 한다.
6. GKE 환경에서는 `:latest` 태그 사용을 금지하며, 명시적 버전 태그를 사용해야 한다.
7. Next.js 빌드는 메모리 소모가 크므로 node 컨테이너 memory limit을 4Gi 이상으로 설정해야 한다.
8. Kaniko 빌드는 ephemeral-storage 소모가 크므로 5Gi 이상의 request를 설정해야 한다.
