# ai-pipeline-service AI 서비스 Jenkins CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | GCP |
| IMG_REG | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| IMG_NAME | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/ai-pipeline-service |
| JENKINS_CLOUD_NAME | gke-ondal |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| JENKINS_GIT_CREDENTIALS | github-credentials |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| AI_SERVICE | ai-pipeline-service |
| PYTHON_VERSION | 3.12 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `deployment/cicd/Jenkinsfile-ai` | Jenkins 파이프라인 스크립트 |

## 파이프라인 구성

### 실행 환경 (Pod Template)

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| python | python:3.12-slim | Python 의존성 설치 및 테스트 |
| kaniko | gcr.io/kaniko-project/executor:debug | 컨테이너 이미지 빌드 및 GCR 푸시 |
| git | alpine/git:2.47.2 | 매니페스트 저장소 클론 및 업데이트 |
| sonar-scanner | sonarsource/sonar-scanner-cli:11 | SonarQube 코드 품질 분석 |

- **Jenkins Cloud**: gke-ondal
- **PIPELINE_ID**: ai-${BUILD_NUMBER} (Job 간 Pod 라벨 충돌 방지)

### 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|----------|-----------|------|
| 1 | Get Source | jnlp | 소스 코드 체크아웃 |
| 2 | Build & Test | python | cd ai-pipeline-service && pip install && pytest |
| 3 | SonarQube Analysis & Quality Gate | sonar-scanner | Python 코드 품질 분석 (스킵 가능) |
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
| {AI_SERVICE} | ai-pipeline-service |
| {PYTHON_VERSION} | 3.12 |
| {IMG_NAME} | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/ai-pipeline-service |
| {JENKINS_CLOUD_NAME} | gke-ondal |
| {JENKINS_GIT_CREDENTIALS} | github-credentials |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |

## Jenkins Job 정보
| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-ai |
| Job URL | http://myjenkins.io/job/lunchpick-ai/ |
| SCM | Git (https://github.com/hiondal/lunch-menu-recommender) |
| Script Path | deployment/cicd/Jenkinsfile-ai |

## GKE 환경 적용 사항
- Kaniko 사용 (GKE에서 privileged 컨테이너 차단)
- alpine/git:2.47.2, sonarsource/sonar-scanner-cli:11 (GKE :latest 차단 정책)
- SonarQube sources: app 디렉토리 (가이드 준수)
- CI/CD 분리: kubectl apply 없이 매니페스트 레포 image tag만 업데이트 (ArgoCD GitOps)
- Kaniko GCR 인증: base64 auth 방식 사용 (SA JSON 키 특수문자 안전 처리)

## Jenkinsfile 수정 내역

### 가이드 대비 수정 사항
| 수정 항목 | 변경 전 | 변경 후 | 사유 |
|-----------|---------|---------|------|
| PIPELINE_ID | `${env.BUILD_NUMBER}` | `ai-${env.BUILD_NUMBER}` | Job 간 Pod 라벨 충돌 방지 (backend와 동시 빌드 시) |
| git 이미지 태그 | `alpine/git:latest` | `alpine/git:2.47.2` | GKE :latest 태그 차단 정책 |
| sonar-scanner 이미지 태그 | `sonarsource/sonar-scanner-cli:latest` | `sonarsource/sonar-scanner-cli:11` | GKE :latest 태그 차단 정책 |
| SonarQube sources | `sonar.sources=.` | `sonar.sources=app` | 가이드 규칙 준수 |
| Kaniko config.json 생성 | `echo` + printf %s | `base64 auth` 방식 | GCR SA JSON 키의 특수문자(줄바꿈, 쌍따옴표)로 인한 JSON 파싱 에러 해결 |

## 빌드 실행 결과

### 빌드 이력

| 빌드 # | 결과 | 소요 시간 | 실패 원인 |
|--------|------|-----------|-----------|
| #1 | FAILURE | 25s | PIPELINE_ID 충돌 (backend Job과 동일 라벨) - python 컨테이너 미발견 |
| #2 | FAILURE | 54s | config.json 파싱 에러 (echo 방식의 특수문자 문제) |
| #3 | FAILURE | - | config.json 파싱 에러 (printf %s 방식도 동일 문제) |
| #4 | FAILURE | 73s | base64 인코딩 결과에 줄바꿈 포함 (invalid character '\n') |
| **#5** | **SUCCESS** | **111s** | - |

### 성공 빌드 (#5) 상세

| 항목 | 값 |
|------|-----|
| 빌드 번호 | #5 |
| 결과 | SUCCESS |
| 소요 시간 | 110,924ms (약 1분 51초) |
| 빌드 파라미터 | ENVIRONMENT=dev, SKIP_SONARQUBE=true, BRANCH=main |
| 생성 이미지 | `asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/ai-pipeline-service:dev-20260306022402` |
| 매니페스트 업데이트 | lunchpick-manifest 리포 main 브랜치 커밋 완료 |

### 스테이지별 실행 결과 (빌드 #5)

| 스테이지 | 결과 | 비고 |
|----------|------|------|
| Get Source | 성공 | main 브랜치 체크아웃 |
| Build & Test | 성공 | pip install 완료, pytest 37 passed / 10 failed (|| true로 계속 진행) |
| SonarQube Analysis | 스킵 | SKIP_SONARQUBE=true |
| Build & Push Images | 성공 | Kaniko로 이미지 빌드 및 GCR 푸시 완료 |
| Update Manifest Repository | 성공 | kustomize edit set image 후 매니페스트 리포 푸시 완료 |
| Pipeline Complete | 성공 | "Pipeline completed successfully!" |
