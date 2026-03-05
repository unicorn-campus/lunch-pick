# frontend 프론트엔드 GitHub Actions CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | AWS |
| IMG_REG | docker.io |
| IMG_NAME | hiondal/lunchpick-frontend |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| MANIFEST_SECRET_GIT_USERNAME | GIT_USERNAME |
| MANIFEST_SECRET_GIT_PASSWORD | GIT_PASSWORD |

## 클라우드별 추가 정보

**AWS:**
| 항목 | 값 |
|------|-----|
| ECR_REGION | vars.ECR_REGION (Repository Variable) |
| EKS_CLUSTER | eks-ondal |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| FRONTEND_FRAMEWORK | React (Next.js 15) |
| FRONTEND_SERVICE | frontend |
| SERVICE_NAME | frontend |
| NODE_VERSION | 20 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `.github/workflows/frontend-cicd.yaml` | GitHub Actions 워크플로우 |

## 파이프라인 구성
Build and Test → SonarQube Analysis → Build and Push Docker Image → Update Manifest Repository

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {FRONTEND_FRAMEWORK} | React (Next.js 15) |
| {FRONTEND_SERVICE} | frontend |
| {SERVICE_NAME} | frontend |
| {NODE_VERSION} | 20 |
| {IMG_REG} | docker.io |
| {IMG_NAME} | hiondal/lunchpick-frontend |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |
| {MANIFEST_SECRET_GIT_USERNAME} | GIT_USERNAME |
| {MANIFEST_SECRET_GIT_PASSWORD} | GIT_PASSWORD |
| {프론트엔드-디렉토리} | frontend |
| CLOUD 기본값 | 'Azure' → 'AWS' |

## 모노레포 대응
- push paths에 `frontend/` 프리픽스 추가
- npm ci, npm run build 단계에 `working-directory: frontend` 설정
- SonarQube sources/tests 경로를 `frontend/src`로 조정
- Upload artifact 경로를 `frontend/.next/`로 변경
- Docker build의 PROJECT_FOLDER를 `frontend`으로 설정

## 검증 체크리스트
- [x] React/Node.js 블록 활성화, Flutter 블록 주석
- [x] 모든 플레이스홀더 치환 완료
- [x] `${{ }}` 표현식 손상 없음
- [x] SonarQube 단계 포함 (continue-on-error: true)
- [x] 매니페스트 업데이트: kustomize edit set image
- [x] kubectl apply 없음
- [x] CI/CD 분리 원칙 준수 (CI: 빌드/푸시/매니페스트 tag 업데이트, CD: ArgoCD 자동 배포)
- [x] 시크릿 하드코딩 없음
