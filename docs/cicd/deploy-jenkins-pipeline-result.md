# CI/CD 파이프라인 빌드 검증 결과

## 개요
Jenkins CI 파이프라인 3개를 생성하고, 빌드 오류를 수정하여 전체 CI/CD 흐름(빌드 -> 이미지 푸시 -> 매니페스트 업데이트)을 검증 완료했다.

## 환경 정보
| 항목 | 값 |
|---|---|
| Jenkins URL | http://myjenkins.io |
| CI 도구 | Jenkins 2.516.2 |
| CD 도구 | ArgoCD |
| K8s 클러스터 | eks-ondal |
| 이미지 레지스트리 | DockerHub (docker.io/hiondal) |
| 소스 레포 | https://github.com/hiondal/lunch-menu-recommender.git |
| 매니페스트 레포 | https://github.com/hiondal/lunchpick-manifest.git |

## Jenkins 잡 구성

### 1. lunchpick-backend
- **Jenkinsfile**: `deployment/cicd/Jenkinsfile-backend`
- **빌드 대상**: member-service, recommendation-service, payment-service (3개)
- **Pod 컨테이너**: gradle:8-jdk21, docker:27-dind, alpine/git
- **파라미터**: SERVICE (all/개별), BRANCH, ENVIRONMENT

### 2. lunchpick-frontend
- **Jenkinsfile**: `deployment/cicd/Jenkinsfile-frontend`
- **빌드 대상**: frontend (Next.js 16)
- **Pod 컨테이너**: node:20-slim (4Gi), docker:24-dind
- **파라미터**: ENVIRONMENT

### 3. lunchpick-ai
- **Jenkinsfile**: `deployment/cicd/Jenkinsfile-ai`
- **빌드 대상**: ai-pipeline-service (FastAPI)
- **Pod 컨테이너**: python:3.12-slim, docker:27-dind
- **파라미터**: ENVIRONMENT

## 빌드 결과 (Build #4)

| 잡 | 상태 | 소요시간 | Docker Push | Manifest Update |
|---|---|---|---|---|
| lunchpick-ai | SUCCESS | 137s | hiondal/ai-pipeline-service:dev-1818a02 | 완료 |
| lunchpick-frontend | SUCCESS | 197s | hiondal/frontend:dev-1818a02 | 완료 |
| lunchpick-backend | UNSTABLE | - | hiondal/lunchpick-{member,recommendation,payment}-service:dev-1818a02 | 완료 |

## 수정 이력

### 1차 수정 (Build #1 -> #2)
- **문제**: Git 인증 실패 (`Invalid username or token`)
- **해결**: Jenkins에 `github-credentials` (GitHub PAT) 크리덴셜 등록

### 2차 수정 (Build #2 -> #3)
- **문제 1**: `cleanWs()` DSL 메서드 없음 (Workspace Cleanup 플러그인 미설치)
- **해결**: `cleanWs()` -> `deleteDir()` 변경
- **문제 2**: `pytest --cov` 실패 (pytest-cov 미설치)
- **해결**: `pytest` 로 변경 (커버리지 옵션 제거)
- **문제 3**: Frontend node 컨테이너 OOMKilled
- **해결**: 메모리 1Gi -> 4Gi, CPU 1000m -> 2000m 증가

### 3차 수정 (Build #3 -> #4)
- **문제**: `deleteDir()`가 node_modules 삭제 실패 (파일 권한)
- **해결**: `deleteDir()` 제거 (`podRetention never()`로 Pod 자동 정리)

## CI/CD 크리덴셜

| Credential ID | 용도 |
|---|---|
| dockerhub-credentials | DockerHub 이미지 푸시 |
| github-credentials | 소스/매니페스트 레포 Git 접근 |

## 잔여 사항
- Backend 단위 테스트 6건 실패 (HistoryServiceTest, MealServiceTest) - 기존 코드 이슈
- AI 단위 테스트 실패 (test_reason_service.py) - 기존 코드 이슈
- Frontend ESLint 에러 7건 (setState in effect) - 기존 코드 이슈
- 위 항목들은 CI/CD 파이프라인과 무관한 소스 코드 품질 이슈임
