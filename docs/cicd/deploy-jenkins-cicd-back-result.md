# lunchpick 백엔드 Jenkins CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | GCP |
| IMG_REG | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| JENKINS_CLOUD_NAME | gke-ondal |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| JENKINS_GIT_CREDENTIALS | github-credentials |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| SYSTEM_NAME | lunchpick |
| SERVICE_NAMES | member-service, recommendation-service, payment-service |
| JDK_VERSION | 21 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `deployment/cicd/Jenkinsfile-backend` | Jenkins 파이프라인 스크립트 |

## 파이프라인 구성

### 실행 환경 (Pod Template)

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| kaniko | gcr.io/kaniko-project/executor:debug | 컨테이너 이미지 빌드 및 GCR 푸시 |
| gradle | gradle:jdk21 | Java 21 기반 Gradle 빌드, SonarQube 분석 |
| git | alpine/git:2.47.2 | 매니페스트 저장소 클론 및 업데이트 |

- **Jenkins Cloud**: gke-ondal
- **ServiceAccount**: jenkins
- **Pod Retention**: never() (빌드 완료 후 즉시 삭제)
- **PIPELINE_ID**: backend-${BUILD_NUMBER} (Job 간 Pod 라벨 충돌 방지)

### 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|----------|-----------|------|
| 1 | Get Source | jnlp | 소스 코드 체크아웃 |
| 2 | Build | gradle | Gradle 빌드 (테스트 제외) |
| 3 | SonarQube Analysis & Quality Gate | gradle | 코드 품질 분석 (스킵 가능) |
| 4 | Build & Push Images | kaniko | Kaniko로 컨테이너 이미지 빌드 및 GCR 푸시 |
| 5 | Update Manifest Repository | git | Kustomize 이미지 태그 업데이트 후 푸시 |
| 6 | Pipeline Complete | - | 완료 상태 알림 |

### 빌드 파라미터

| 파라미터 | 타입 | 기본값 | 옵션 |
|----------|------|--------|------|
| SERVICE | Choice | all | all, member-service, recommendation-service, payment-service |
| BRANCH | String | main | 자유 입력 |
| ENVIRONMENT | Choice | dev | dev, staging, prod |
| SKIP_SONARQUBE | Choice | true | false, true |

### 이미지 매핑

| 서비스 | 이미지 |
|--------|--------|
| member-service | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/member-service |
| recommendation-service | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/recommendation-service |
| payment-service | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/payment-service |

- **이미지 태그 형식**: `{environment}-{yyyyMMddHHmmss}`

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {SYSTEM_NAME} | lunchpick |
| {SERVICE_NAMES} | member-service, recommendation-service, payment-service |
| {JDK_VERSION} | 21 |
| {IMG_REG} | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| {JENKINS_CLOUD_NAME} | gke-ondal |
| {JENKINS_GIT_CREDENTIALS} | github-credentials |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |

## Jenkins Job 정보
| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-backend |
| Job URL | http://myjenkins.io/job/lunchpick-backend/ |
| SCM | Git (https://github.com/hiondal/lunch-menu-recommender) |
| Script Path | deployment/cicd/Jenkinsfile-backend |

## GKE 환경 적용 사항
- Kaniko 사용 (GKE에서 privileged 컨테이너 차단)
- alpine/git:2.47.2 (GKE :latest 태그 차단 정책)
- Kaniko GCR 인증: SA JSON을 파일로 저장 후 `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 방식 사용
- DockerHub 인증: config.json에 printf로 안전하게 생성
- 모든 Kaniko 빌드를 단일 sh 블록에서 실행 (executor 파일시스템 변경 문제 방지)
- CI/CD 분리: kubectl apply 없이 매니페스트 레포 image tag만 업데이트 (ArgoCD GitOps)

## Jenkins Job 생성 및 빌드 실행 결과
| 항목 | 결과 |
|------|------|
| Job 생성 | 이미 존재 (HTTP 200, 생성 스킵) |
| 빌드 #1 | FAILURE - config.json 파싱 오류 (echo 방식 SA JSON 특수문자 문제) |
| 빌드 #2 | FAILURE - printf 방식으로 변경했으나 SA JSON 내부 쌍따옴표 미이스케이프 |
| 빌드 #3 | **SUCCESS** - GOOGLE_APPLICATION_CREDENTIALS 방식으로 GCR 인증 해결 |

### 빌드 #3 검증 로그
- Gradle 빌드: member-service, recommendation-service, payment-service 전체 빌드 성공
- SonarQube: SKIP_SONARQUBE=true로 스킵
- Kaniko 이미지 빌드 및 GCR 푸시: 3개 서비스 모두 성공 (태그: `dev-20260306022133`)
- 매니페스트 업데이트: lunchpick-manifest.git에 kustomize set image 반영 후 push 완료
- 최종 결과: **Finished: SUCCESS**

## 수정 이력
1. **alpine/git:latest -> alpine/git:2.47.2**: GKE에서 `:latest` 태그 차단 정책 대응
2. **Kaniko GCR 인증 방식 변경**: echo로 config.json에 SA JSON을 직접 삽입하는 방식에서, SA JSON을 파일(`/kaniko/sa.json`)로 저장 후 `GOOGLE_APPLICATION_CREDENTIALS` 환경변수로 GCR 인증하는 방식으로 변경. SA JSON 내부 특수문자(쌍따옴표, 줄바꿈)로 인한 config.json 파싱 오류 해결
3. **PIPELINE_ID 접두사 추가**: `backend-${env.BUILD_NUMBER}` 형태로 Pod 라벨 고유화
