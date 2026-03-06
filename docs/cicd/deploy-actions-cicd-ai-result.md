# ai-pipeline-service AI 서비스 GitHub Actions CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | GCP |
| IMG_REG | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| IMG_NAME | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/ai-pipeline-service |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| MANIFEST_SECRET_GIT_USERNAME | GIT_USERNAME |
| MANIFEST_SECRET_GIT_PASSWORD | GIT_PASSWORD |

## 클라우드별 추가 정보

**GCP:**
| 항목 | 값 |
|------|-----|
| GCR_REGION | asia-northeast3 |
| GCR_PROJECT | lunchpick-489007 |
| GCR_REPO | lunchpick |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| AI_SERVICE | ai-pipeline-service |
| PYTHON_VERSION | 3.12 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `.github/workflows/ai-cicd.yaml` | GitHub Actions 워크플로우 |

## 파이프라인 구성
Build and Test → SonarQube Analysis → Build and Push Docker Image → Update AI Service Manifest Repository

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {AI_SERVICE} | ai-pipeline-service |
| {PYTHON_VERSION} | 3.12 |
| {IMG_REG} | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick |
| {IMG_NAME} | asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick/ai-pipeline-service |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |
| {MANIFEST_SECRET_GIT_USERNAME} | GIT_USERNAME |
| {MANIFEST_SECRET_GIT_PASSWORD} | GIT_PASSWORD |
| CLOUD 기본값 | GCP |

## 검증 체크리스트
- [x] 파이프라인에 kubectl apply가 없음 (CI/CD 분리 원칙 준수)
- [x] 모든 플레이스홀더 치환 완료
- [x] SonarQube 단계 포함 (continue-on-error: true, SKIP_SONARQUBE 기본 true)
- [x] 매니페스트 업데이트: kustomize edit set image
- [x] GCP Artifact Registry 인증: google-github-actions/auth@v2 + gcloud auth configure-docker
- [x] pip install -r requirements.txt 사용 (poetry 미사용)
- [x] 시크릿 하드코딩 없음
