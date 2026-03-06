# CI/CD 파이프라인 구성 완료

## 환경 정보
- CI 도구: GitHub Actions
- CD 도구: ArgoCD (GitOps)
- Cloud: GCP
- K8s 클러스터: gke-ondal (GKE Autopilot)
- K8s 네임스페이스: lunchpick
- 레지스트리: GCR (asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick)

## Step 1: CI/CD 도구 사전 설정 산출물
- 클라우드 사전작업: 완료 (GKE Autopilot - 별도 NodePool 불필요)
- Jenkins: 설치 완료 (이전 구성, GitHub Actions로 전환)
- SonarQube: 설치 완료 (VM Docker sonarqube:9.9-community, http://mysonar.io)
- ArgoCD: 설치 완료 (argo 3.35.4, http://myargocd.io)
- Nginx 프록시: 설정 완료 (VM: gcp)
- 사전 설정 보고서: `docs/cicd/cicd-pre-setup-report.md`

## Step 2: 매니페스트 레포지토리 + ArgoCD 산출물
- 매니페스트 레포지토리: https://github.com/hiondal/lunchpick-manifest.git
- 구조: Kustomize base/overlays (lunchpick, frontend, ai-pipeline-service)
- ArgoCD Application YAML: argocd/lunchpick-dev.yaml, frontend-dev.yaml, ai-pipeline-service-dev.yaml
- ArgoCD Application 등록: 완료 (3개 Application Synced)
- ArgoCD 준비 보고서: `docs/cicd/deploy-argocd-prepare.md`

## Step 3: CI/CD 파이프라인 산출물 (GitHub Actions)

### GitHub Actions Workflows
| Workflow | 파일 | 설명 |
|----------|------|------|
| Backend Services CI/CD | `.github/workflows/backend-cicd.yaml` | Gradle 빌드 + Docker 이미지 푸시 + manifest tag 업데이트 |
| Frontend CI/CD | `.github/workflows/frontend-cicd.yaml` | Node.js 20 빌드 + Docker 이미지 푸시 + manifest tag 업데이트 |
| AI Service CI/CD | `.github/workflows/ai-cicd.yaml` | Python 3.12 빌드 + Docker 이미지 푸시 + manifest tag 업데이트 |

### 파이프라인 공통 구조
1. **Build & Test**: 언어별 빌드 (Gradle/npm/pip)
2. **SonarQube Analysis & Quality Gate**: 코드 품질 분석 (continue-on-error, 기본 skip)
3. **Build & Push Images**: Docker Buildx 기반 이미지 빌드 + GCP Artifact Registry 푸시 (google-github-actions/auth@v2)
4. **Update Manifest Repository**: Kustomize로 매니페스트 레포 image tag 업데이트 (x-access-token PAT 인증)
5. **ArgoCD 자동 배포**: manifest 변경 감지 -> K8s 자동 배포

### GCP Artifact Registry 인증 방식
- GCR 인증: `google-github-actions/auth@v2` + `gcloud auth configure-docker`
- Secrets: `GCP_SA_KEY` (서비스 계정 키 JSON)
- Variables: `GCR_REGION`, `REGISTRY`

### 파이프라인 파라미터
| 파라미터 | 백엔드 | 프론트엔드 | AI |
|---------|--------|-----------|-----|
| ENVIRONMENT | dev (기본) | dev (기본) | dev (기본) |
| SKIP_SONARQUBE | true/false | true/false | true/false |
| SERVICE | all/member-service/recommendation-service/payment-service | - | - |

### 결과 보고서
- 백엔드: `docs/cicd/deploy-actions-cicd-back-result.md`
- 프론트엔드: `docs/cicd/deploy-actions-cicd-front-result.md`
- AI 서비스: `docs/cicd/deploy-actions-cicd-ai-result.md`

## CI/CD 흐름 다이어그램

```
개발자 코드 푸시 (GitHub)
    |
    v
GitHub Actions CI (ubuntu-latest runner)
    |-- 소스 체크아웃
    |-- 빌드 & 테스트
    |-- SonarQube 코드 품질 분석
    |-- Docker 이미지 빌드 & GCR 푸시 (google-github-actions/auth@v2)
    |-- 매니페스트 레포 image tag 업데이트 (Kustomize, x-access-token PAT)
    |
    v
ArgoCD CD (GitOps)
    |-- 매니페스트 레포 변경 감지
    |-- K8s 클러스터 자동 배포 (gke-ondal / lunchpick)
```

## 접속 정보
| 도구 | URL | ID |
|------|-----|----|
| SonarQube | http://mysonar.io | admin |
| ArgoCD | http://myargocd.io | admin |

## 다음 단계
1. GitHub Actions 워크플로우를 소스 레포에 push
2. GitHub Actions에서 각 워크플로우를 실행하여 전체 흐름 검증 (빌드 -> 푸시 -> manifest 업데이트 -> ArgoCD 자동 배포)
3. ArgoCD 대시보드에서 배포 상태 확인
