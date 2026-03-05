# CI/CD 파이프라인 구성 완료

## 환경 정보
- CI 도구: Jenkins
- CD 도구: ArgoCD (GitOps)
- Cloud: Azure
- K8s 클러스터: aks-ondal
- K8s 네임스페이스: lunchpick
- 레지스트리: DockerHub (docker.io/hiondal)

## Step 1: CI/CD 도구 사전 설정 산출물
- 클라우드 사전작업: 완료 (Karpenter NodePool: cicd, sonarqube)
- Jenkins: 설치 완료 (bitnami 13.6.17, http://myjenkins.io)
- SonarQube: 설치 완료 (bitnami 8.1.17, http://mysonar.io)
- ArgoCD: 설치 완료 (argo 3.35.4, http://myargocd.io)
- Nginx 프록시: 설정 완료 (VM: azure, 20.249.211.140)
- 사전 설정 보고서: `docs/cicd/cicd-pre-setup-report.md`

## Step 2: 매니페스트 레포지토리 + ArgoCD 산출물
- 매니페스트 레포지토리: https://github.com/hiondal/lunchpick-manifest.git
- 구조: Kustomize base/overlays (lunchpick, frontend, ai-pipeline-service)
- ArgoCD Application YAML: argocd/lunchpick-dev.yaml, frontend-dev.yaml, ai-pipeline-service-dev.yaml
- ArgoCD Application 등록: 완료 (3개 Application Synced)
- ArgoCD 준비 보고서: `deploy-argocd-prepare.md`

## Step 3: CI/CD 파이프라인 산출물

### Jenkins Pipeline Jobs
| Job | Jenkinsfile | 설명 |
|-----|-------------|------|
| lunchpick-backend | `deployment/cicd/Jenkinsfile-backend` | Gradle 빌드 + Podman 이미지 푸시 + manifest tag 업데이트 |
| lunchpick-frontend | `deployment/cicd/Jenkinsfile-frontend` | Node.js 20 빌드 + Podman 이미지 푸시 + manifest tag 업데이트 |
| lunchpick-ai | `deployment/cicd/Jenkinsfile-ai` | Python 3.12 빌드 + Podman 이미지 푸시 + manifest tag 업데이트 |

### 파이프라인 공통 구조
1. **Get Source**: GitHub 소스 체크아웃
2. **Build & Test**: 언어별 빌드 (Gradle/npm/pip)
3. **SonarQube Analysis & Quality Gate**: 코드 품질 분석 (JaCoCo/sonar-scanner)
4. **Build & Push Images**: Podman 기반 컨테이너 이미지 빌드 및 DockerHub 푸시
5. **Update Manifest Repository**: Kustomize로 매니페스트 레포 image tag 업데이트
6. **ArgoCD 자동 배포**: manifest 변경 감지 → K8s 자동 배포

### 파이프라인 파라미터
| 파라미터 | 백엔드 | 프론트엔드 | AI |
|---------|--------|-----------|-----|
| BRANCH | main (기본) | main (기본) | main (기본) |
| ENVIRONMENT | dev/staging/prod | dev/staging/prod | dev/staging/prod |
| SKIP_SONARQUBE | false/true | false/true | false/true |
| SERVICE | all/member-service/recommendation-service/payment-service | - | - |

## CI/CD 흐름 다이어그램

```
개발자 코드 푸시
    |
    v
Jenkins CI 파이프라인
    |-- 소스 체크아웃
    |-- 빌드 & 테스트
    |-- SonarQube 코드 품질 분석
    |-- Podman 이미지 빌드 & DockerHub 푸시
    |-- 매니페스트 레포 image tag 업데이트 (Kustomize)
    |
    v
ArgoCD CD (GitOps)
    |-- 매니페스트 레포 변경 감지
    |-- K8s 클러스터 자동 배포 (aks-ondal / lunchpick)
```

## 접속 정보
| 도구 | URL | ID |
|------|-----|----|
| Jenkins | http://myjenkins.io | admin |
| SonarQube | http://mysonar.io | admin |
| ArgoCD | http://myargocd.io | admin |

## 다음 단계
1. Jenkins에서 각 파이프라인 Job을 실행하여 전체 흐름 검증 (빌드 → 푸시 → manifest 업데이트 → ArgoCD 자동 배포)
2. GitHub Webhook 설정으로 코드 푸시 시 자동 빌드 트리거 구성
