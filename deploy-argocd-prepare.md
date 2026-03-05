# Step 2: 매니페스트 레포지토리 + ArgoCD 구성 결과 보고서

## 작업 일시
2026-03-06

## 1. 매니페스트 레포지토리 구성

### 레포지토리 정보
- URL: https://github.com/hiondal/lunchpick-manifest.git
- 브랜치: main
- 로컬 경로: C:/Users/hiond/workspace/lunchpick-manifest-new

### 디렉토리 구조
```
lunchpick-manifest/
├── lunchpick/
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
│           └── dev/
│               └── kustomization.yaml
├── frontend/
│   └── kustomize/
│       ├── base/
│       │   ├── kustomization.yaml
│       │   ├── deployment.yaml
│       │   └── service.yaml
│       └── overlays/
│           └── dev/
│               └── kustomization.yaml
├── ai-pipeline-service/
│   └── kustomize/
│       ├── base/
│       │   ├── kustomization.yaml
│       │   ├── deployment.yaml
│       │   └── service.yaml
│       └── overlays/
│           └── dev/
│               └── kustomization.yaml
└── argocd/
    ├── lunchpick-dev.yaml
    ├── frontend-dev.yaml
    └── ai-pipeline-service-dev.yaml
```

### 기존 대비 변경 사항
- AI 서비스 포트: 8000 -> 8084 (실제 컨테이너 포트에 맞춤)
- base kustomization.yaml에서 namespace 제거 (overlay에서만 설정)
- staging/prod 환경 파일 삭제 (ENVIRONMENTS: dev만 유지)
- 프론트엔드 overlay의 중복 이미지 항목 제거

### 서비스별 이미지 및 포트 매핑

| 서비스 | 이미지 | 컨테이너 포트 | 서비스 포트 |
|--------|--------|--------------|------------|
| member-service | hiondal/lunchpick-member-service | 8081 | 80 |
| recommendation-service | hiondal/lunchpick-recommendation-service | 8082 | 80 |
| payment-service | hiondal/lunchpick-payment-service | 8083 | 80 |
| frontend | hiondal/lunchpick-frontend | 3000 | 8080 |
| ai-pipeline-service | hiondal/lunchpick-ai-pipeline-service | 8084 | 80 |

## 2. ArgoCD Application YAML 목록

| 파일명 | Application 이름 | 매니페스트 경로 | 대상 네임스페이스 |
|--------|------------------|----------------|------------------|
| argocd/lunchpick-dev.yaml | lunchpick-dev | lunchpick/kustomize/overlays/dev | lunchpick |
| argocd/frontend-dev.yaml | frontend-dev | frontend/kustomize/overlays/dev | lunchpick |
| argocd/ai-pipeline-service-dev.yaml | ai-pipeline-service-dev | ai-pipeline-service/kustomize/overlays/dev | lunchpick |

### syncPolicy 설정
- automated: prune=true, selfHeal=true
- syncOptions: CreateNamespace=true

## 3. ArgoCD 매니페스트 레포지토리 인증 등록

- Secret 이름: manifest-repo-cred
- 네임스페이스: argocd
- 레이블: argocd.argoproj.io/secret-type=repository
- 인증 방식: username/password (GitHub PAT)

## 4. ArgoCD Application 등록 결과

```
NAME                      SYNC STATUS   HEALTH STATUS
ai-pipeline-service-dev   Synced        Progressing
frontend-dev              Synced        Healthy
lunchpick-dev             Synced        Healthy
```

- 3개 Application 모두 Synced 상태
- ai-pipeline-service-dev: Progressing (파드 startup probe 통과 대기 중, 정상)
- frontend-dev, lunchpick-dev: Healthy

## 5. Kustomize 유효성 검사 결과

- lunchpick/kustomize/overlays/dev: 통과 (6개 리소스: 3 Deployment + 3 Service)
- frontend/kustomize/overlays/dev: 통과 (2개 리소스: 1 Deployment + 1 Service)
- ai-pipeline-service/kustomize/overlays/dev: 통과 (2개 리소스: 1 Deployment + 1 Service)

## 6. AKS 환경 추가 조치

ArgoCD 컨트롤러가 AKS 클러스터 리소스를 정상적으로 스캔할 수 있도록 다음 권한을 추가함:

1. **resource.exclusions 설정** (argocd-cm ConfigMap)
   - admissionregistration.k8s.io 그룹의 ValidatingAdmissionPolicy, ValidatingAdmissionPolicyBinding 제외

2. **ClusterRole/ClusterRoleBinding 추가**
   - argocd-admissionpolicy-access: admissionregistration.k8s.io 리소스 접근 권한
   - argocd-cluster-admin: cluster-admin 역할 바인딩 (ArgoCD 컨트롤러 SA)

## 7. 다음 단계

- Step 3: Jenkins CI 파이프라인 구성 (Jenkinsfile 작성, 매니페스트 업데이트 스크립트 작성)
