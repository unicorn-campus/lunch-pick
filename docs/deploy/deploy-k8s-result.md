# K8s 배포 완료 보고

## 배포 환경
- 클러스터 유형: EKS (eks-ondal)
- 리전: ap-northeast-2
- 네임스페이스: lunchpick
- 이미지 레지스트리: Docker Hub (hiondal)
- 배포 일시: 2026-03-05

## 배포 결과
- 백엔드 컨테이너: 빌드 및 배포 완료 (member-service, recommendation-service, payment-service)
- 프론트엔드 컨테이너: 빌드 및 배포 완료 (frontend)
- AI 서비스 컨테이너: 빌드 및 배포 완료 (ai-pipeline-service)
- K8s 배포: 완료 (전체 Pod 1/1 Running)
- Nginx Proxy: HTTPS 프록시 설정 완료

## Pod 상태
| 서비스 | Replicas | 상태 |
|--------|----------|------|
| frontend | 1 | Running |
| member-service | 1 | Running |
| recommendation-service | 1 | Running |
| payment-service | 1 | Running |
| ai-pipeline-service | 1 | Running |
| postgres-postgresql | 1 | Running |
| redis-master | 1 | Running |

## Ingress 정보
| 이름 | Host | ALB ADDRESS |
|------|------|-------------|
| frontend | web.43.201.25.39.nip.io | k8s-lunchpic-frontend-76693d28f8-651637953.ap-northeast-2.elb.amazonaws.com |
| lunchpick | api.web.43.201.25.39.nip.io | k8s-lunchpic-lunchpic-c67078ae58-1526357841.ap-northeast-2.elb.amazonaws.com |

## 접속 정보
- 프론트엔드: https://web.43.201.25.39.nip.io
- 백엔드 API: https://api.web.43.201.25.39.nip.io

## 접속 검증
- 프론트엔드 (https://web.43.201.25.39.nip.io): HTTP 200 OK
- 백엔드 API (https://api.web.43.201.25.39.nip.io/api/test/login): HTTP 200 OK
- 백엔드 API (https://api.web.43.201.25.39.nip.io/api/v1/recommendations/today): HTTP 403 (인증 필요, 정상)

## 백킹서비스
| 서비스 | 설치 방식 | Health Check |
|--------|----------|-------------|
| PostgreSQL | Helm bitnami/postgresql 14.3.2 | pg_isready: accepting connections |
| Redis | Helm bitnami/redis 18.4.0 | redis-cli ping: PONG |

## PostgreSQL DB/스키마
- DB: member, recommendation, payment
- 스키마: lunchpick_member, lunchpick_recommendation, lunchpick_payment

## 수정 이력
- Deployment replicas: 2 → 1 (사용자 요청)
- PostgreSQL 커스텀 스키마 수동 생성 (lunchpick_member, lunchpick_recommendation, lunchpick_payment)
- CICD Nginx config 임시 비활성화 (ALB DNS 미존재로 nginx -t 실패 방지)
