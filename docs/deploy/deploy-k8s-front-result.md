# 프론트엔드 K8s 배포 결과

## 배포 일시
2026-03-03

## 환경
- Cloud: GCP (GKE)
- Cluster: gce-ondal
- Namespace: lunchpick
- Registry: GCR (asia-northeast3-docker.pkg.dev/lunchpick-489007/lunchpick)
- IngressClass: gce (annotation 방식)
- 프레임워크: Next.js 15 (standalone mode)
- 실제 컨테이너 포트: 3000 (Dockerfile EXPOSE 3000)

## 1. 사전 확인

### K8s 클러스터 연결 확인
```
kubectl config current-context
gke_lunchpick-489007_asia-northeast3_gce-ondal
```
- 결과: 정상

### Namespace 확인
```
NAME        STATUS   AGE
lunchpick   Active   19m
```
- 결과: 정상

### 비고
- gke-gcloud-auth-plugin.exe 위치: C:\Users\hiond\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\
- 환경변수 USE_GKE_GCLOUD_AUTH_PLUGIN=True 필요
- kubectl 실행 시 PATH에 gcloud bin 디렉토리 추가 필요

## 2. 매니페스트 적용

### frontend (deployment/k8s/frontend/)
```
configmap/cm-frontend created
deployment.apps/frontend created
ingress.networking.k8s.io/frontend created
service/frontend created
```

### Probe 수정 이력
- 초기 Probe 경로: `/health` → 404 응답 (Next.js standalone에 /health 엔드포인트 미존재)
- 수정 후 Probe 경로: `/` → 200 응답 (정상)

## 3. 객체 생성 확인

```
NAME                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/frontend   1/1     1            1           111s

NAME               TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
service/frontend   ClusterIP   34.118.233.69   <none>        8080/TCP   110s

NAME                                 CLASS    HOSTS                     ADDRESS   PORTS   AGE
ingress.networking.k8s.io/frontend   <none>   web.34.50.22.190.nip.io             80      111s
```

- frontend Deployment: 1/1 READY (정상)
- frontend Service: ClusterIP 34.118.233.69, port 8080
- frontend Ingress: GCE LB 프로비저닝 중 (ADDRESS 할당 대기)

## 4. Pod 상태

```
NAME                        READY   STATUS    RESTARTS   AGE
frontend-5f9799f64d-qgsnf   1/1     Running   0          30s
```
- Pod IP: 10.103.128.201:3000
- 상태: Running, Ready (정상)

## 5. Ingress 라우팅

| Path | Backend Service | Port |
|------|----------------|------|
| /    | frontend       | 8080 |

- Ingress HOST: web.34.50.22.190.nip.io
- GCE LB ADDRESS: 프로비저닝 중 (수 분 내 할당 예정)

## 6. ConfigMap (cm-frontend) runtime-env.js

```javascript
window.__runtime_config__ = {
  APP_ENV: "production",
  API_GROUP: "/api/v1",
  MEMBER_HOST: "https://api.web.34.50.22.190.nip.io",
  RECOMMENDATION_HOST: "https://api.web.34.50.22.190.nip.io",
  PAYMENT_HOST: "https://api.web.34.50.22.190.nip.io",
  AI_HOST: "https://api.web.34.50.22.190.nip.io",
  KAKAO_CLIENT_ID: "d34722dff8545446e14b2616bb62c6b0",
  KAKAO_API_KEY: "b588f91e780efc914b282ad7e3688e01",
  KAKAO_JS_KEY: "298b6fef9e85e51d5ffbdbbc782fa3c5"
};
```
- volumeMount 경로: /app/public/runtime-env.js (subPath: runtime-env.js)

## 7. Service 구성

| 항목 | 값 |
|------|-----|
| Service port | 8080 |
| Service targetPort | 3000 (Next.js standalone EXPOSE 포트) |
| Probe endpoint | / (200 OK 확인) |
| Ingress host | web.34.50.22.190.nip.io |

> 주의: Dockerfile EXPOSE 3000, Next.js PORT=3000 → Service targetPort=3000 적용
> 가이드의 containerPort 3000 지시에 따라 Service port=8080, targetPort=3000으로 분리

## 8. 완료 체크리스트

- [x] 객체이름 네이밍룰 준수 여부 (Ingress: frontend, ConfigMap: cm-frontend, Service: frontend, Deployment: frontend)
- [x] Dockerfile EXPOSE 포트(3000) 확인 후 containerPort/targetPort 설정
- [x] Service port=8080, targetPort=3000으로 분리 설정
- [x] ConfigMap runtime-env.js HOST 값을 https://api.web.34.50.22.190.nip.io로 변경
- [x] volumeMount 경로 /app/public/runtime-env.js (컨테이너 실행 결과 문서 기준)
- [x] GCP이므로 ingressClassName 미지정, annotation kubernetes.io/ingress.class: gce 사용
- [x] Ingress host: web.34.50.22.190.nip.io (api. 없음 - 프론트엔드 전용)
- [x] Probe 경로 / 로 수정 (Next.js standalone /health 미지원 → 404 확인 후 수정)
- [x] imagePullSecrets: lunchpick 지정
- [x] imagePullPolicy: Always 지정
- [x] resources requests/limits 지정 (250m/256Mi ~ 1000m/1024Mi)
- [x] Deployment READY 1/1 확인
- [x] Pod Running 상태 확인
