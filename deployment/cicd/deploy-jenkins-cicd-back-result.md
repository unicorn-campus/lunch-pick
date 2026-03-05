# 백엔드 Jenkins CI 파이프라인 생성 결과

## 개요
백엔드 서비스(member-service, recommendation-service, payment-service)를 위한 Jenkins CI 파이프라인을 생성했다.

## 생성 파일
| 파일 | 설명 |
|------|------|
| `deployment/cicd/Jenkinsfile-backend` | 백엔드 CI 파이프라인 정의 |

## 파이프라인 구성

### 실행 환경
- **Jenkins Cloud**: eks-ondal (Kubernetes Pod 기반 에이전트)
- **Pod 컨테이너**: gradle(JDK 21), docker(DinD), git
- **Pod 정책**: podRetention never() (빌드 후 Pod 삭제)

### 파이프라인 스테이지
| 단계 | 컨테이너 | 설명 |
|------|----------|------|
| Checkout | default | 소스 코드 체크아웃 및 이미지 태그(git short SHA) 생성 |
| Build | gradle | `gradle clean bootJar -x test`로 JAR 빌드 |
| Test | gradle | `gradle test` 실행 및 JUnit 결과 수집 |
| Docker Build & Push | docker | Dockerfile-backend 기반 이미지 빌드 후 DockerHub 푸시 |
| Update Manifest | git | manifest 저장소의 deployment.yaml 이미지 태그 업데이트 |

### 파라미터
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| SERVICE | choice | all | 빌드 대상 서비스 (all/개별 서비스) |
| BRANCH | string | main | 빌드 브랜치 |
| ENVIRONMENT | choice | dev | 배포 환경 (dev/staging/prod) |

### 이미지 정보
- **레지스트리**: docker.io (DockerHub)
- **이미지 네이밍**: `docker.io/hiondal/lunchpick-{서비스명}:{환경}-{gitShortSHA}`
- **예시**: `docker.io/hiondal/lunchpick-member-service:dev-3f9594f`

### Credentials 참조
| ID | 용도 |
|----|------|
| dockerhub-credentials | DockerHub 로그인 |
| github-credentials | Manifest 저장소 접근 |

### Dockerfile 빌드 인자
- `BUILD_LIB_DIR`: `{서비스명}/build/libs` (Gradle 빌드 결과 경로)
- `ARTIFACTORY_FILE`: `{서비스명}.jar` (JAR 파일명)

### Manifest 업데이트
- **저장소**: https://github.com/hiondal/lunchpick-manifest.git
- **대상 파일**: `{환경}/{서비스명}/deployment.yaml`
- **업데이트 방식**: sed를 사용하여 image 태그 치환
- **kubectl apply 없음**: GitOps 방식으로 manifest 저장소 업데이트만 수행

## 주의사항
1. Jenkins에 `dockerhub-credentials`와 `github-credentials` Credentials가 사전 등록되어 있어야 한다
2. Kubernetes cloud `eks-ondal`이 Jenkins에 설정되어 있어야 한다
3. Manifest 저장소에 `{환경}/{서비스명}/deployment.yaml` 파일이 존재해야 한다
4. CI 파이프라인은 kubectl apply를 수행하지 않으며, ArgoCD 등 GitOps 도구가 manifest 변경을 감지하여 배포한다
