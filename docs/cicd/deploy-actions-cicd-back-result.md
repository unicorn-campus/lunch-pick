# lunchpick 백엔드 GitHub Actions CI 파이프라인 결과서

## 실행 환경 정보
| 항목 | 값 |
|------|-----|
| CLOUD | Azure |
| IMG_REG | docker.io |
| IMG_NAME (member-service) | hiondal/lunchpick-member-service |
| IMG_NAME (recommendation-service) | hiondal/lunchpick-recommendation-service |
| IMG_NAME (payment-service) | hiondal/lunchpick-payment-service |
| MANIFEST_REPO_URL | https://github.com/hiondal/lunchpick-manifest.git |
| MANIFEST_SECRET_GIT_USERNAME | GIT_USERNAME |
| MANIFEST_SECRET_GIT_PASSWORD | GIT_PASSWORD |

## 클라우드별 추가 정보

**Azure:**
| 항목 | 값 |
|------|-----|
| ACR_NAME | (DockerHub 사용, ACR 미사용) |
| RESOURCE_GROUP | (DockerHub 사용, ACR 미사용) |

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
Build and Test -> SonarQube Analysis -> Build and Push Docker Images -> Update Manifest Repository

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
| CLOUD 기본값 | 'Azure' |
| {서비스명1} | member-service |
| {서비스명2} | recommendation-service |
| {서비스명3} | payment-service |
| {이미지명1} | hiondal/lunchpick-member-service |
| {이미지명2} | hiondal/lunchpick-recommendation-service |
| {이미지명3} | hiondal/lunchpick-payment-service |
| {서비스명N} / {SERVICE_NAMEN} | (제거됨 - 3개 서비스에 맞게 조정) |

## 검증 체크리스트
- [x] 파이프라인에 kubectl apply가 없음 (CI/CD 분리 원칙 준수)
- [x] 모든 서비스명이 실제 값으로 치환됨 (미치환 플레이스홀더 0건)
- [x] SonarQube 단계 포함 (continue-on-error: true, SKIP_SONARQUBE 기본 true)
- [x] 매니페스트 업데이트 방식: kustomize edit set image 사용
- [x] YAML 문법 유효성 검증 통과 (python3 yaml.safe_load)
- [x] DockerHub 로그인 포함, ACR/ECR/GCR 조건부 로그인은 조건 미충족으로 스킵
- [x] 시크릿 하드코딩 없음 (secrets.* 참조 사용)
