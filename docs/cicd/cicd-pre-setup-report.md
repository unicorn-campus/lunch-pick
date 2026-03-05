# CI/CD 도구 사전 설정 결과 보고서

## 1. 설치 환경

| 항목 | 값 |
|------|-----|
| CLOUD | AWS |
| CI_TOOL | Jenkins |
| K8S_CLUSTER | eks-ondal |
| VM_HOST | aws (43.201.25.39) |
| 레지스트리 | DockerHub (docker.io/hiondal) |

## 2. 클라우드 사전작업

| 리소스 | 상태 |
|--------|------|
| StorageClass (gp2-eks-auto) | 생성 완료 |
| IngressClass (alb) | 생성 완료 |
| ALB Subnet 태그 | 등록 완료 (4개 서브넷) |
| NodePool (cicd) | 생성 완료 (spot, t3a/m5a large/xlarge) |
| NodePool (sonarqube) | 생성 완료 (spot, t3a/m5a xlarge/2xlarge) |

## 3. 도구 설치 결과

| 도구 | 네임스페이스 | Helm Release | 상태 | Ingress Address |
|------|-----------|-------------|------|----------------|
| Jenkins | jenkins | jenkins (bitnami 13.6.17) | 설치 완료, Pod Ready | k8s-jenkins-jenkins-2646a74345-289826574.ap-northeast-2.elb.amazonaws.com |
| SonarQube | sonarqube | sonar (bitnami 8.1.17) | 설치 완료, Pod Ready (affinity 패치 적용) | k8s-sonarqub-sonarson-79285d79a9-869210287.ap-northeast-2.elb.amazonaws.com |
| ArgoCD | argocd | argocd (argo 3.35.4) | 설치 완료, Pod Ready (insecure 모드 정상) | k8s-argocd-argocdse-4b25862b32-2013769863.ap-northeast-2.elb.amazonaws.com |

## 4. 접속 정보

| 도구 | URL | ID | 암호 |
|------|-----|----|----|
| Jenkins | http://myjenkins.io | admin | P@ssw0rd$ |
| SonarQube | http://mysonar.io | admin | sonarP@ssw0rd$ |
| ArgoCD | http://myargocd.io | admin | xMGYD4tIFoljrQra |

> 암호 조회 명령:
> - Jenkins: `kubectl get secret jenkins -n jenkins -o jsonpath='{.data.jenkins-password}' | base64 -d`
> - SonarQube: `kubectl get secret sonar-sonarqube -n sonarqube -o jsonpath='{.data.sonarqube-password}' | base64 -d`
> - ArgoCD: `kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d`

## 5. RBAC

| 리소스 | 상태 |
|--------|------|
| Jenkins ClusterRoleBinding (jenkins-admin) | 생성 완료 (cluster-admin 역할) |

## 6. Nginx 프록시

| 항목 | 결과 |
|------|------|
| 설정 파일 | /etc/nginx/sites-available/cicd |
| nginx -t | syntax ok, test successful |
| curl myjenkins.io | HTTP 403 (인증 전 정상) |
| curl mysonar.io | HTTP 200 |
| curl myargocd.io | HTTP 200 |

## 7. 매니페스트 레포지토리

| 항목 | 값 |
|------|-----|
| URL | https://github.com/hiondal/lunchpick-manifest.git |
| 상태 | 이미 존재 (기존 사용) |

## 8. 수동 후속 작업

### Jenkins 플러그인 설치 (웹 UI: http://myjenkins.io)
- Kubernetes, Pipeline Utility Steps, Docker Pipeline, GitHub, Blue Ocean, SonarQube Scanner

### Jenkins Kubernetes Cloud 연결 (Dashboard > Manage Jenkins > Clouds)
- Kubernetes URL: `https://kubernetes.default`
- Kubernetes Namespace: `jenkins`
- Jenkins URL: `http://jenkins`
- Jenkins tunnel: `jenkins-agent-listener:50000`

### Jenkins tunnel 포트 설정 (Dashboard > Manage Jenkins > Security)

### SonarQube 설정 (웹 UI: http://mysonar.io)
- User Token 발급: MyAccount > Security
- Jenkins 통보 Webhook: `http://jenkins.jenkins.svc.cluster.local/sonarqube-webhook/`
- Quality Gate: 'Sonar way' 복사 후 Code Coverage 조정
- Jenkins Credential 등록: Token으로 등록
- SonarQube Server 설정: Jenkins System 설정에서 서버 URL 및 Token Credential 등록

### DockerHub Credentials 등록 (Jenkins 웹 UI)
- DockerHub에서 Personal Access Token 생성
- Credential 이름: `dockerhub-credentials` (Kind: Username with password)

> 상세 가이드: https://github.com/unicorn-plugins/npd/blob/main/resources/references/setup-cicd-tools.md
