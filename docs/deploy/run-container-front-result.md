# 프론트엔드 컨테이너 실행 결과서

## 구성 환경
- 환경: docker run (VM 컨테이너 배포)
- VM: gcp (34.64.192.123)
- 실행 일시: 2026-02-28

## VM 접속 방법
```
ssh gcp
```

## 실행된 컨테이너

| 항목 | 값 |
|------|---|
| 서비스명 | frontend |
| 이미지 | asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick/frontend:latest |
| 포트 매핑 | 3000:3000 |
| 네트워크 | lunch-menu-recommender_default |
| 상태 | Running |

## runtime-env.js 설정
```javascript
window.__runtime_config__ = {
  APP_ENV: "development",
  API_GROUP: "/api/v1",
  MEMBER_HOST: "http://34.64.192.123:8081",
  RECOMMENDATION_HOST: "http://34.64.192.123:8082",
  PAYMENT_HOST: "http://34.64.192.123:8083",
  AI_HOST: "http://34.64.192.123:8084",
  KAKAO_CLIENT_ID: "d34722dff8545446e14b2616bb62c6b0",
  KAKAO_API_KEY: "b588f91e780efc914b282ad7e3688e01",
  KAKAO_JS_KEY: "298b6fef9e85e51d5ffbdbbc782fa3c5"
};
```

> runtime-env.js는 VM의 `~/frontend/public/runtime-env.js`에 생성하고 볼륨 마운트로 주입.
> 브라우저에서 백엔드 API를 호출하므로 VM 외부 IP(34.64.192.123) 사용.

## Health Check 결과
- [x] `docker ps | grep frontend` 확인: 컨테이너 실행 중
- [x] HTTP 200 OK (`wget -qO- http://localhost:3000 -S`)
- [ ] 브라우저 접속 확인: `http://34.64.192.123:3000`

## 실행 명령어
```bash
REGISTRY=asia-northeast3-docker.pkg.dev/travel-planner-b7120/lunchpick
docker run -d --name frontend --rm --network lunch-menu-recommender_default \
  -p 3000:3000 \
  -v ~/frontend/public/runtime-env.js:/app/public/runtime-env.js \
  ${REGISTRY}/frontend:latest
```
