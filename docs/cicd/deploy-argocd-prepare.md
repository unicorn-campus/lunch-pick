# ArgoCD 매니페스트 레포지토리 구성 결과 보고서

## 작업 일시
2026-03-05

## 매니페스트 레포지토리 구성

### 레포지토리 정보
- **URL**: https://github.com/hiondal/lunchpick-manifest.git
- **브랜치**: main
- **유형**: Private

### 디렉토리 구조
```
lunchpick-manifest/
├── lunchpick/                          # 백엔드 시스템
│   └── kustomize/
│       ├── base/
│       │   ├── kustomization.yaml
│       │   ├── member-service/
│       │   │   ├── deployment.yaml
│       │   │   └── service.yaml
│       │   ├── recommendation-service/
│       │   │   ├── deployment.yaml
│       │   │   └── service.yaml
│       │   └── payment-service/
│       │       ├── deployment.yaml
│       │       └── service.yaml
│       └── overlays/
│           ├── dev/kustomization.yaml
│           ├── staging/kustomization.yaml
│           └── prod/kustomization.yaml
├── frontend/                           # 프론트엔드 서비스
│   └── kustomize/
│       ├── base/
│       │   ├── kustomization.yaml
│       │   ├── deployment.yaml
│       │   └── service.yaml
│       └── overlays/
│           ├── dev/kustomization.yaml
│           ├── staging/kustomization.yaml
│           └── prod/kustomization.yaml
├── ai-pipeline-service/                # AI 서비스
│   └── kustomize/
│       ├── base/
│       │   ├── kustomization.yaml
│       │   ├── deployment.yaml
│       │   └── service.yaml
│       └── overlays/
│           ├── dev/kustomization.yaml
│           ├── staging/kustomization.yaml
│           └── prod/kustomization.yaml
└── argocd/                             # ArgoCD Application CRD
    ├── lunchpick-dev.yaml
    ├── lunchpick-staging.yaml
    ├── lunchpick-prod.yaml
    ├── frontend-dev.yaml
    ├── frontend-staging.yaml
    ├── frontend-prod.yaml
    ├── ai-pipeline-service-dev.yaml
    ├── ai-pipeline-service-staging.yaml
    └── ai-pipeline-service-prod.yaml
```

## ArgoCD Application YAML 생성 목록

| Application 이름 | 서비스 유형 | 환경 | Kustomize 경로 |
|------------------|-----------|------|---------------|
| lunchpick-dev | 백엔드 시스템 | dev | lunchpick/kustomize/overlays/dev |
| lunchpick-staging | 백엔드 시스템 | staging | lunchpick/kustomize/overlays/staging |
| lunchpick-prod | 백엔드 시스템 | prod | lunchpick/kustomize/overlays/prod |
| frontend-dev | 프론트엔드 | dev | frontend/kustomize/overlays/dev |
| frontend-staging | 프론트엔드 | staging | frontend/kustomize/overlays/staging |
| frontend-prod | 프론트엔드 | prod | frontend/kustomize/overlays/prod |
| ai-pipeline-service-dev | AI 서비스 | dev | ai-pipeline-service/kustomize/overlays/dev |
| ai-pipeline-service-staging | AI 서비스 | staging | ai-pipeline-service/kustomize/overlays/staging |
| ai-pipeline-service-prod | AI 서비스 | prod | ai-pipeline-service/kustomize/overlays/prod |

## ArgoCD Application 등록 결과

### 인증 등록
- **Secret 이름**: manifest-repo-cred
- **네임스페이스**: argocd
- **레이블**: argocd.argoproj.io/secret-type=repository
- **상태**: 등록 완료

### Application 등록 상태
```
NAME                          SYNC STATUS   HEALTH STATUS
ai-pipeline-service-dev       Unknown       Healthy
ai-pipeline-service-prod      Unknown       Healthy
ai-pipeline-service-staging   Unknown       Healthy
frontend-dev                  Unknown       Healthy
frontend-prod                 Unknown       Healthy
frontend-staging              Unknown       Healthy
lunchpick-dev                 Unknown       Healthy
lunchpick-prod                Unknown       Healthy
lunchpick-staging             Unknown       Healthy
```

> SYNC STATUS가 Unknown인 것은 매니페스트 레포에 image tag만 존재하고 실제 배포가 아직 수행되지 않았기 때문입니다. CI 파이프라인 실행 후 image tag가 업데이트되면 ArgoCD가 자동 동기화하여 배포합니다.

## ArgoCD 감시 설정

| 항목 | 값 |
|------|-----|
| 매니페스트 레포지토리 | https://github.com/hiondal/lunchpick-manifest.git |
| 타겟 리비전 | HEAD |
| 배포 대상 클러스터 | https://kubernetes.default.svc |
| 배포 대상 네임스페이스 | lunchpick |
| 동기화 정책 | automated (prune: true, selfHeal: true) |

## Kustomize 검증 결과
- 백엔드 (lunchpick/kustomize/overlays/dev): 통과
- 프론트엔드 (frontend/kustomize/overlays/dev): 통과
- AI 서비스 (ai-pipeline-service/kustomize/overlays/dev): 통과

## 이미지 정보

| 서비스 | 이미지 | 기본 태그 |
|--------|--------|----------|
| member-service | hiondal/lunchpick-member-service | latest |
| recommendation-service | hiondal/lunchpick-recommendation-service | latest |
| payment-service | hiondal/lunchpick-payment-service | latest |
| frontend | hiondal/lunchpick-frontend | latest |
| ai-pipeline-service | hiondal/lunchpick-ai-pipeline-service | latest |

## 다음 단계
1. Step 2.5: Jenkins 설정 정보 수집 (Cloud Name, Git Credentials)
2. Step 3: CI/CD 파이프라인 작성 (Jenkinsfile - 빌드+푸시+manifest tag 업데이트)
