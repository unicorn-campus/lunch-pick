# lunchpick 백엔드 GitHub Actions CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | AWS |
| IMG_REG | docker.io |
| IMG_NAME (member-service) | hiondal/lunchpick-member-service |
| IMG_NAME (recommendation-service) | hiondal/lunchpick-recommendation-service |
| IMG_NAME (payment-service) | hiondal/lunchpick-payment-service |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| MANIFEST_SECRET_GIT_USERNAME | GIT_USERNAME |
| MANIFEST_SECRET_GIT_PASSWORD | GIT_PASSWORD |

## 클라우드별 추가 정보

**AWS:**
| 항목 | 값 |
|------|-----|
| ECR_ACCOUNT | (Repository Variable로 설정) |
| ECR_REGION | (Repository Variable로 설정) |

## 서비스 정보
| 항목 | 값 |
|------|-----|
| SYSTEM_NAME | lunchpick |
| SERVICE_NAMES | member-service, recommendation-service, payment-service |
| JDK_VERSION | 21 |

## 생성 파일
| 파일 | 설명 |
|------|------|
| `.github/workflows/backend-cicd.yaml` | GitHub Actions 워크플로우 |

## 파이프라인 구성
Build and Test → SonarQube Analysis → Build and Push Docker Images → Update Manifest Repository

## 변수 치환 내역
| 플레이스홀더 | 치환값 |
|-------------|--------|
| {SYSTEM_NAME} | lunchpick |
| {SERVICE_NAMES} | member-service, recommendation-service, payment-service |
| {JDK_VERSION} / {JDK버전} / {버전} | 21 |
| {IMG_REG} | docker.io |
| {IMG_NAME} (member-service) | hiondal/lunchpick-member-service |
| {IMG_NAME} (recommendation-service) | hiondal/lunchpick-recommendation-service |
| {IMG_NAME} (payment-service) | hiondal/lunchpick-payment-service |
| {MANIFEST_REPO_URL} | https://github.com/hiondal/lunchpick-manifest.git |
| {MANIFEST_SECRET_GIT_USERNAME} | GIT_USERNAME |
| {MANIFEST_SECRET_GIT_PASSWORD} | GIT_PASSWORD |
| CLOUD 기본값 'Azure' | 'AWS' |
| {서비스명1} | member-service |
| {서비스명2} | recommendation-service |
| {서비스명3} | payment-service |
| {이미지명1} | hiondal/lunchpick-member-service |
| {이미지명2} | hiondal/lunchpick-recommendation-service |
| {이미지명3} | hiondal/lunchpick-payment-service |
| {서비스명N} / {SERVICE_NAMEN} | (제거됨 - 3개 서비스에 맞게 조정) |
