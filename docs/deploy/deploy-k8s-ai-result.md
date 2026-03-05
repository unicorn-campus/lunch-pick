# AI Pipeline Service K8s 배포 결과

## 배포 일시
2026-03-03

## 환경
- Cloud: AWS (EKS Auto Mode)
- Cluster: eks-ondal
- Namespace: lunchpick
- Registry: ECR (851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick)
- 프레임워크: FastAPI (Python 3.12)
- 서비스명: ai-pipeline-service (디렉토리명 기준, pyproject.toml 없음)

## 1. 사전 확인

### AWS CLI 로그인
- `aws sts get-caller-identity` → Account: 851725211153 (정상)

### K8s 클러스터 연결
- `kubectl cluster-info` → eks-ondal 정상

### Namespace 확인
- `kubectl get ns lunchpick` → Active (정상)

## 2. 매니페스트 적용

### ai-pipeline-service (deployment/k8s/ai-pipeline-service/)
```
configmap/cm-ai-pipeline-service created
deployment.apps/ai-pipeline-service created
secret/secret-ai-pipeline-service created
service/ai-pipeline-service created
```

## 3. 객체 생성 확인

```
NAME                                          READY   STATUS              RESTARTS   AGE
pod/ai-pipeline-service-66499644c4-kzfmm      0/1     ContainerCreating   0          12s

NAME                          TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
service/ai-pipeline-service   ClusterIP   10.100.71.82   <none>        80/TCP    12s
```

- AI 서비스는 별도 Ingress 없음 (백엔드 공통 Ingress lunchpick에 /api/v1, /health path로 포함)

## 4. 환경변수 매핑 테이블

AI 서비스는 --env-file ~/.env.ai 로 실행 (.env 기반). 전체 환경변수 매핑:

| 환경변수 | 지정 객체 | 값 |
|---------|----------|-----|
| DB_HOST | cm-ai-pipeline-service | postgres-postgresql |
| DB_PORT | cm-ai-pipeline-service | 5432 |
| DB_USER | cm-ai-pipeline-service | lunchpick |
| DB_PASSWORD | secret-ai-pipeline-service | P@ssw0rd$ |
| REDIS_HOST | cm-ai-pipeline-service | redis-master |
| REDIS_PORT | cm-ai-pipeline-service | 6379 |
| REDIS_DATABASE | cm-ai-pipeline-service | 4 |
| KAKAO_CLIENT_ID | cm-ai-pipeline-service | b588f91e780efc914b282ad7e3688e01 |
| KAKAO_CLIENT_SECRET | secret-ai-pipeline-service | JxKimFRKDOi8XGbsSaIkZPXV8qxGzp9O |
| KAKAO_REDIRECT_URI | cm-ai-pipeline-service | https://web.43.201.25.39.nip.io/login |
| KAKAO_API_KEY | cm-ai-pipeline-service | b588f91e780efc914b282ad7e3688e01 |
| KAKAO_JS_KEY | cm-ai-pipeline-service | 298b6fef9e85e51d5ffbdbbc782fa3c5 |
| GEMINI_API_KEY | secret-ai-pipeline-service | AIzaSyBfENgBCKf_OCTMRC9_WyKg03Br1KC0VCI |
| MEMBER_SERVICE_PORT | cm-ai-pipeline-service | 8081 |
| RECOMMENDATION_SERVICE_PORT | cm-ai-pipeline-service | 8082 |
| PAYMENT_SERVICE_PORT | cm-ai-pipeline-service | 8083 |
| AI_PIPELINE_SERVICE_PORT | cm-ai-pipeline-service | 8084 |
| APP_PORT | cm-ai-pipeline-service | 8084 |

- cm-common (공통): CORS_ALLOWED_ORIGINS, LOG_LEVEL_* 등 공통 환경변수 포함
- secret-common: JWT_SECRET 포함

## 5. Service 구성

| 항목 | 값 |
|------|-----|
| Service port | 80 |
| Service targetPort | 8084 (APP_PORT 기준) |
| Probe endpoint | /health |
| Ingress (공통) | api.web.43.201.25.39.nip.io/api/v1, /health |

## 6. Ingress 라우팅 (공통 Ingress에 포함)

| Path | Backend | Port |
|------|---------|------|
| /api/v1 | ai-pipeline-service | 80 |
| /health | ai-pipeline-service | 80 |

## 7. 완료 체크리스트

- [x] 객체이름 네이밍룰 준수 여부 (cm-ai-pipeline-service, secret-ai-pipeline-service, ai-pipeline-service)
- [x] Secret 매니페스트에서 'data' 대신 'stringData'를 사용 했는가?
- [x] 매니페스트 파일 안에 환경변수를 사용하지 않고 실제 값을 지정 했는가?
- [x] Image명이 '851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick/ai-pipeline-service:latest' 형식인지 재확인
- [x] Probe endpoint가 '/health'로 지정되었는가? (Actuator 아님)
- [x] 보안이 필요한 환경변수(API 키 등)는 Secret 매니페스트로 지정했는가? (GEMINI_API_KEY, KAKAO_CLIENT_SECRET, DB_PASSWORD)
- [x] ConfigMap과 Secret은 'env' 대신 'envFrom'을 사용하였는가?
- [x] REDIS_DATABASE=4 (ai 전용 DB) 지정 확인
- [x] rewrite-target annotation 미사용 확인
- [x] 컨테이너 실행 명령(.env.ai) 전체 환경변수 매핑 테이블 체크 완료
