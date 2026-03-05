# 백엔드 K8s 배포 결과

## 배포 일시
2026-03-03

## 환경
- Cloud: AWS (EKS Auto Mode)
- Cluster: eks-ondal
- Namespace: lunchpick
- Registry: ECR (851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick)
- IngressClassName: alb

## 1. 사전 확인

### AWS CLI 로그인 확인
```
{
    "UserId": "AIDA4MTWHKYI7GDEA45G2",
    "Account": "851725211153",
    "Arn": "arn:aws:iam::851725211153:user/hiondal"
}
```
- 결과: 정상

### K8s 클러스터 연결 확인
```
Kubernetes control plane is running at https://AC49EAE6D567FA1E9712A19B2299E87D.yl4.ap-northeast-2.eks.amazonaws.com
```
- 결과: 정상

### Namespace 확인
```
NAME        STATUS   AGE
lunchpick   Active   18m
```
- 결과: 정상

### IngressClass 확인
```
NAME   CONTROLLER              PARAMETERS   AGE
alb    eks.amazonaws.com/alb   <none>       40h
```
- 결과: alb IngressClass 존재 확인

### Redis / PostgreSQL 서비스 확인
```
postgres-postgresql   ClusterIP   10.100.64.70     <none>   5432/TCP   15m
redis-master          ClusterIP   10.100.132.174   <none>   6379/TCP   14m
```
- DB_HOST: postgres-postgresql
- REDIS_HOST: redis-master

## 2. 매니페스트 적용

### 공통 매니페스트 (deployment/k8s/common/)
```
configmap/cm-common created
ingress.networking.k8s.io/lunchpick created
secret/secret-common created
secret/lunchpick created
```

### member-service (deployment/k8s/member-service/)
```
configmap/cm-member-service created
deployment.apps/member-service created
secret/secret-member-service created
service/member-service created
```

### recommendation-service (deployment/k8s/recommendation-service/)
```
configmap/cm-recommendation-service created
deployment.apps/recommendation-service created
secret/secret-recommendation-service created
service/recommendation-service created
```

### payment-service (deployment/k8s/payment-service/)
```
configmap/cm-payment-service created
deployment.apps/payment-service created
secret/secret-payment-service created
service/payment-service created
```

## 3. 객체 생성 확인
```
NAME                                          READY   STATUS    RESTARTS   AGE
pod/member-service-85fb65c9c6-hfc86           0/1     Running   0          29s
pod/payment-service-76ffcbb44-r59wd           0/1     Running   0          26s
pod/recommendation-service-754b9985df-wqnk2   0/1     Running   0          28s

NAME                             TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
service/member-service           ClusterIP   10.100.159.63    <none>        80/TCP    29s
service/payment-service          ClusterIP   10.100.59.255    <none>        80/TCP    26s
service/recommendation-service   ClusterIP   10.100.74.236    <none>        80/TCP    27s

NAME                                  CLASS   HOSTS                         ADDRESS                                                                        PORTS   AGE
ingress.networking.k8s.io/lunchpick   alb     api.web.43.201.25.39.nip.io   k8s-lunchpic-lunchpic-c67078ae58-2096025917.ap-northeast-2.elb.amazonaws.com   80      42s
```
- 비고: EKS Auto Mode로 첫 Pod 배포 시 노드 자동 프로비저닝으로 수 분 소요 예상

## 4. Ingress 라우팅

| Path | Backend Service | Port |
|------|----------------|------|
| /api/v1/auth | member-service | 80 |
| /api/v1/onboarding | member-service | 80 |
| /api/test | member-service | 80 |
| /internal/members | member-service | 80 |
| /api/v1/members | member-service | 80 |
| /api/v1/history | recommendation-service | 80 |
| /api/v1/insights | recommendation-service | 80 |
| /api/v1/recommendations | recommendation-service | 80 |
| /api/v1/meals | recommendation-service | 80 |
| /api/v1/subscriptions | payment-service | 80 |
| /api/v1 | ai-pipeline-service | 80 |
| /health | ai-pipeline-service | 80 |

- Ingress HOST: api.web.43.201.25.39.nip.io
- ALB ADDRESS: k8s-lunchpic-lunchpic-c67078ae58-2096025917.ap-northeast-2.elb.amazonaws.com

## 5. 환경변수 매핑 테이블

### member-service 전체 환경변수

| 환경변수 | 지정 객체 | 값 |
|---------|----------|-----|
| SERVER_PORT | cm-member-service | 8081 |
| SPRING_PROFILES_ACTIVE | cm-common | prod |
| DB_KIND | cm-common | postgresql |
| DB_HOST | cm-member-service | postgres-postgresql |
| DB_PORT | cm-common | 5432 |
| DB_NAME | cm-member-service | member |
| DB_USER | cm-member-service | lunchpick |
| DB_PASSWORD | secret-member-service | P@ssw0rd$ |
| DDL_AUTO | cm-common | update |
| SHOW_SQL | cm-common | false |
| REDIS_HOST | cm-common | redis-master |
| REDIS_PORT | cm-common | 6379 |
| REDIS_DATABASE | cm-member-service | 1 |
| KAFKA_BROKERS | cm-member-service | redis-master:6379 |
| MQ_SUBSCRIPTION_TOPIC | cm-member-service | subscription-events |
| JWT_SECRET | secret-common | (openssl rand -base64 32 생성값) |
| JWT_ACCESS_TOKEN_VALIDITY | cm-common | 1800 |
| JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400 |
| KAKAO_CLIENT_ID | cm-member-service | d34722dff8545446e14b2616bb62c6b0 |
| KAKAO_CLIENT_SECRET | secret-member-service | W6R3Gn0QyNaLj8ugyS7gQVxhJq7GRwUv |
| KAKAO_REDIRECT_URI | cm-member-service | https://web.43.201.25.39.nip.io/login |
| CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,...,https://web.43.201.25.39.nip.io |
| LOG_LEVEL_ROOT | cm-common | INFO |
| LOG_LEVEL_APP | cm-common | INFO |
| LOG_LEVEL_WEB | cm-common | INFO |
| LOG_LEVEL_SQL | cm-common | INFO |
| LOG_LEVEL_SQL_TYPE | cm-common | INFO |
| LOG_FILE_PATH | cm-member-service | logs/member-service.log |

### recommendation-service 전체 환경변수

| 환경변수 | 지정 객체 | 값 |
|---------|----------|-----|
| SERVER_PORT | cm-recommendation-service | 8082 |
| SPRING_PROFILES_ACTIVE | cm-common | prod |
| DB_KIND | cm-common | postgresql |
| DB_HOST | cm-recommendation-service | postgres-postgresql |
| DB_PORT | cm-common | 5432 |
| DB_NAME | cm-recommendation-service | recommendation |
| DB_USER | cm-recommendation-service | lunchpick |
| DB_PASSWORD | secret-recommendation-service | P@ssw0rd$ |
| DDL_AUTO | cm-common | update |
| SHOW_SQL | cm-common | false |
| REDIS_HOST | cm-common | redis-master |
| REDIS_PORT | cm-common | 6379 |
| REDIS_DATABASE | cm-recommendation-service | 2 |
| JWT_SECRET | secret-common | (openssl rand -base64 32 생성값) |
| JWT_ACCESS_TOKEN_VALIDITY | cm-common | 1800 |
| JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400 |
| MEMBER_SERVICE_URL | cm-recommendation-service | http://member-service:8081 |
| AI_PIPELINE_SERVICE_URL | cm-recommendation-service | http://ai-pipeline-service:8084 |
| WEATHER_API_URL | cm-recommendation-service | https://api.openweathermap.org |
| WEATHER_API_KEY | secret-recommendation-service | 1aa5bfca079a20586915b56f29235cc0 |
| KAKAO_API_KEY | cm-recommendation-service | b588f91e780efc914b282ad7e3688e01 |
| CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,...,https://web.43.201.25.39.nip.io |
| LOG_LEVEL_ROOT | cm-common | INFO |
| LOG_LEVEL_APP | cm-common | INFO |
| LOG_LEVEL_WEB | cm-common | INFO |
| LOG_LEVEL_SQL | cm-common | INFO |
| LOG_LEVEL_SQL_TYPE | cm-common | INFO |
| LOG_FILE_PATH | cm-recommendation-service | logs/recommendation-service.log |

### payment-service 전체 환경변수

| 환경변수 | 지정 객체 | 값 |
|---------|----------|-----|
| SERVER_PORT | cm-payment-service | 8083 |
| SPRING_PROFILES_ACTIVE | cm-common | prod |
| DB_KIND | cm-common | postgresql |
| DB_HOST | cm-payment-service | postgres-postgresql |
| DB_PORT | cm-common | 5432 |
| DB_NAME | cm-payment-service | payment |
| DB_USER | cm-payment-service | lunchpick |
| DB_PASSWORD | secret-payment-service | P@ssw0rd$ |
| DDL_AUTO | cm-common | update |
| SHOW_SQL | cm-common | false |
| REDIS_HOST | cm-common | redis-master |
| REDIS_PORT | cm-common | 6379 |
| REDIS_DATABASE | cm-payment-service | 3 |
| KAFKA_BROKERS | cm-payment-service | redis-master:6379 |
| MQ_SUBSCRIPTION_TOPIC | cm-payment-service | subscription-events |
| JWT_SECRET | secret-common | (openssl rand -base64 32 생성값) |
| JWT_ACCESS_TOKEN_VALIDITY | cm-common | 1800 |
| JWT_REFRESH_TOKEN_VALIDITY | cm-common | 86400 |
| CORS_ALLOWED_ORIGINS | cm-common | http://localhost:8081,...,https://web.43.201.25.39.nip.io |
| LOG_LEVEL_ROOT | cm-common | INFO |
| LOG_LEVEL_APP | cm-common | INFO |
| LOG_LEVEL_WEB | cm-common | INFO |
| LOG_LEVEL_SQL | cm-common | INFO |
| LOG_LEVEL_SQL_TYPE | cm-common | INFO |
| LOG_FILE_PATH | cm-payment-service | logs/payment-service.log |

## 6. 완료 체크리스트

- [x] 객체이름 네이밍룰 준수 여부
- [x] Redis Host명을 ClusterIP 타입의 Service 객체로 했는가? (redis-master)
- [x] Database Host명을 ClusterIP타입의 Service 객체로 했는가? (postgres-postgresql)
- [x] Secret 매니페스트에서 'data' 대신 'stringData'를 사용 했는가?
- [x] JWT_SECRET을 openssl 명령으로 생성해서 지정했는가?
- [x] 매니페스트 파일 안에 환경변수를 사용하지 않고 실제 값을 지정 했는가?
- [x] Image Pull Secret에 USERNAME(AWS)과 PASSWORD(ecr get-login-password) 실제 값 지정 했는가?
- [x] Image명이 '851725211153.dkr.ecr.ap-northeast-2.amazonaws.com/lunchpick/{서비스명}:latest' 형식인지 재확인
- [x] Ingress host가 `api.web.43.201.25.39.nip.io` 형식인지 확인
- [x] ingressClassName이 `alb`로 설정되었는지 확인
- [x] Ingress 매니페스트의 각 서비스 backend.service.port.number와 Service 매니페스트의 port가 "80"으로 동일한가?
- [x] Ingress의 path는 각 서비스 별 Controller '@RequestMapping' 기준으로 지정했는가?
- [x] 보안이 필요한 환경변수는 Secret 매니페스트로 지정했는가?
- [x] REDIS_DATABASE는 각 서비스마다 다르게 지정했는가? (member=1, recommendation=2, payment=3)
- [x] ConfigMap과 Secret은 'env' 대신 'envFrom'을 사용하였는가?
- [x] rewrite-target annotation 미사용 확인
- [x] 컨테이너 실행 명령 매핑 테이블로 누락된 환경변수 체크 완료
