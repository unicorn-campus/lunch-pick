# 백엔드 Jenkins CI 파이프라인 생성 결과

## 1. 개요

| 항목 | 값 |
|------|-----|
| 시스템명 | lunchpick |
| CI 도구 | Jenkins |
| 대상 서비스 | member-service, recommendation-service, payment-service |
| 소스 저장소 | https://github.com/hiondal/lunch-menu-recommender |
| 매니페스트 저장소 | https://github.com/hiondal/lunchpick-manifest.git |
| 이미지 레지스트리 | docker.io/hiondal |
| 작성일 | 2026-03-06 |

## 2. 생성 파일

| 파일 | 경로 | 설명 |
|------|------|------|
| Jenkinsfile-backend | deployment/cicd/Jenkinsfile-backend | 백엔드 멀티 서비스 CI 파이프라인 |

## 3. 파이프라인 구성

### 3.1 실행 환경 (Pod Template)

| 컨테이너 | 이미지 | 용도 |
|-----------|--------|------|
| podman | mgoltzsche/podman | 컨테이너 이미지 빌드 및 푸시 (privileged) |
| gradle | gradle:jdk21 | Java 21 기반 Gradle 빌드, SonarQube 분석 |
| git | alpine/git:latest | 매니페스트 저장소 클론 및 업데이트 |

- **Jenkins Cloud**: aks-ondal (Kubernetes Pod 기반 에이전트)
- **ServiceAccount**: jenkins
- **Pod Retention**: never (빌드 완료 후 즉시 삭제)
- **Active Deadline**: 3600초 (1시간)

### 3.2 파이프라인 스테이지

| 순서 | 스테이지 | 컨테이너 | 설명 |
|------|----------|-----------|------|
| 1 | Get Source | jnlp | 소스 코드 체크아웃 |
| 2 | Build | gradle | Gradle 빌드 (테스트 제외) |
| 3 | SonarQube Analysis & Quality Gate | gradle | 코드 품질 분석 및 Quality Gate 확인 (스킵 가능) |
| 4 | Build & Push Images | podman | Podman으로 컨테이너 이미지 빌드 및 DockerHub 푸시 |
| 5 | Update Manifest Repository | git | Kustomize 이미지 태그 업데이트 후 푸시 |
| 6 | Pipeline Complete | - | 파이프라인 완료 상태 알림 |

### 3.3 빌드 파라미터

| 파라미터 | 타입 | 기본값 | 옵션 |
|----------|------|--------|------|
| SERVICE | Choice | all | all, member-service, recommendation-service, payment-service |
| BRANCH | String | main | 자유 입력 |
| ENVIRONMENT | Choice | dev | dev, staging, prod |
| SKIP_SONARQUBE | Choice | false | false, true |

### 3.4 이미지 매핑

| 서비스 | 이미지 |
|--------|--------|
| member-service | docker.io/hiondal/lunchpick-member-service |
| recommendation-service | docker.io/hiondal/lunchpick-recommendation-service |
| payment-service | docker.io/hiondal/lunchpick-payment-service |

- **이미지 태그 형식**: `{environment}-{yyyyMMddHHmmss}`
- **예시**: `dev-20260306143022`

### 3.5 Dockerfile 빌드 인자

| 인자 | 값 | 설명 |
|------|-----|------|
| BUILD_LIB_DIR | {서비스명}/build/libs | Gradle 빌드 결과 경로 |
| ARTIFACTORY_FILE | {서비스명}.jar | JAR 파일명 |
| Dockerfile | deployment/container/Dockerfile-backend | 백엔드 공통 Dockerfile |
| Platform | linux/amd64 | 빌드 타겟 플랫폼 |

### 3.6 Manifest 업데이트

- **저장소**: https://github.com/hiondal/lunchpick-manifest.git
- **대상 경로**: `lunchpick/kustomize/overlays/{environment}/kustomization.yaml`
- **업데이트 방식**: Kustomize `edit set image` 명령으로 이미지 태그 치환
- **kubectl apply 없음**: GitOps 방식으로 매니페스트 저장소 업데이트만 수행

## 4. Jenkins Job 생성 결과

| 항목 | 값 |
|------|-----|
| Job 이름 | lunchpick-backend |
| Job URL | http://myjenkins.io/job/lunchpick-backend/ |
| API 응답코드 | **200 (성공)** |
| Job 타입 | Pipeline (CpsScmFlowDefinition) |
| SCM | Git (https://github.com/hiondal/lunch-menu-recommender) |
| Credentials | github-credentials |
| Script Path | deployment/cicd/Jenkinsfile-backend |
| Buildable | true |

## 5. 필요 Jenkins Credentials

| Credential ID | 타입 | 용도 |
|---------------|------|------|
| github-credentials | Username/Password | Git 소스 체크아웃 및 매니페스트 저장소 접근 |
| imagereg-credentials | Username/Password | 이미지 레지스트리 로그인 |
| dockerhub-credentials | Username/Password | DockerHub 로그인 |

## 6. CD 연동

- 매니페스트 저장소의 Kustomize overlay 이미지 태그를 업데이트하면 ArgoCD가 변경을 감지하여 자동 배포 수행
- CI 파이프라인은 배포(kubectl apply)를 직접 수행하지 않음 (GitOps 원칙 준수)

## 7. 주의사항

1. Jenkins에 `dockerhub-credentials`, `imagereg-credentials`, `github-credentials` Credentials가 사전 등록되어 있어야 한다
2. Kubernetes cloud `aks-ondal`이 Jenkins에 설정되어 있어야 한다
3. 매니페스트 저장소에 `lunchpick/kustomize/overlays/{environment}/kustomization.yaml` 파일이 존재해야 한다
4. SonarQube 서버가 Jenkins에 `SonarQube` 이름으로 설정되어 있어야 한다 (SKIP_SONARQUBE=true로 건너뛸 수 있음)
