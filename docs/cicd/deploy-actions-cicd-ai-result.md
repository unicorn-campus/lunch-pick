# ai-pipeline-service AI 서비스 GitHub Actions CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | AWS |
| IMG_REG | docker.io |
| IMG_NAME | hiondal/lunchpick-ai-pipeline-service |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| MANIFEST_SECRET_GIT_USERNAME | GIT_USERNAME |
| MANIFEST_SECRET_GIT_PASSWORD | GIT_PASSWORD |

## 클라우드별 추가 정보

**AWS:**
| 항목 | 값 |
|------|-----|
| ECR_ACCOUNT | (vars.REGISTRY에서 참조) |
| ECR_REGION | (vars.ECR_REGION에서 참조) |

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
Build and Test -> SonarQube Analysis -> Build and Push Docker Image -> Update AI Service Manifest Repository

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {AI_SERVICE} | ai-pipeline-service |
| {PYTHON_VERSION} | 3.12 |
| {IMG_REG} | docker.io |
| {IMG_NAME} | hiondal/lunchpick-ai-pipeline-service |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |
| {MANIFEST_SECRET_GIT_USERNAME} | GIT_USERNAME |
| {MANIFEST_SECRET_GIT_PASSWORD} | GIT_PASSWORD |
| {ai-서비스-디렉토리} | ai-pipeline-service |
| CLOUD 기본값 'Azure' | 'AWS' |
| pip install poetry && poetry install | pip install -r requirements.txt |
| poetry build | (제거 - Dockerfile이 빌드 처리) |
| sonar.sources=app | sonar.sources=ai-pipeline-service |
| sonar.tests=tests | sonar.tests=ai-pipeline-service/tests |
| paths (app/**, tests/**) | ai-pipeline-service/**, .github/** |
