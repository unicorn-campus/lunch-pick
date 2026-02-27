# E2E 테스트 결과 레포트 #1

> 테스트일: 2026-02-27
> 테스터: qa-engineer (가디언)
> 테스트 범위: 전체 (TC-01~24 + GAP-01~08)

---

## 요약

| 항목 | 결과 |
|------|------|
| 총 TC | 32개 (TC 24 + GAP 8) |
| PASS | 23개 |
| FAIL | 9개 |
| console.error | manifest.json 404 (전 페이지 공통) / Hydration 에러 (퀴즈 재진입) |
| 판정 | **FAIL** |

---

## FAIL TC 상세

### [MINOR] TC-02: 로그인 실패 에러 토스트

- **현상**: `/login?error=access_denied` 접근 시 에러 토스트 미표시
- **재현 절차**: 브라우저에서 `http://localhost:3000/login?error=access_denied` 접근
- **기대 동작**: OAuth 에러 파라미터 수신 시 "로그인에 실패했어요" 에러 토스트 표시
- **실제 동작**: `error` 쿼리 파라미터 무시, 일반 로그인 화면 표시
- **원인**: `/login/page.tsx`에서 `code` 파라미터만 처리, `error` 파라미터 핸들링 없음
- **영향 파일**: `frontend/src/app/(auth)/login/page.tsx` (22~52번 줄)
- **담당**: frontend-developer
- **스크린샷**: `.temp/tc-02.png`

---

### [MINOR] TC-04: 퀴즈 재진입 시 Hydration 에러

- **현상**: 퀴즈 이탈 후 재진입 시 React Hydration 에러 발생 (좌하단 "1 Issue" 배지 표시)
- **재현 절차**: 퀴즈 7/10 상태에서 홈으로 이탈 → `/onboarding/quiz` 재진입
- **기대 동작**: 크래시 없이 이어서 진행
- **실제 동작**: Hydration 에러 발생 (`Error: Hydration failed because the server rendered...`), UI는 정상 동작
- **원인**: 서버/클라이언트 상태 불일치 (localStorage 기반 상태 hydration 미스매치)
- **영향 파일**: `frontend/src/app/(onboarding)/onboarding/quiz/page.tsx`
- **담당**: frontend-developer
- **스크린샷**: `.temp/tc-04.png`

---

### [MINOR] TC-10: "왜?" 바텀시트 컨텍스트 태그 미표시

- **현상**: 추천 이유 바텀시트에 컨텍스트 태그 미표시
- **재현 절차**: 홈 화면 → 추천 카드 "왜?" 버튼 클릭
- **기대 동작**: 자연어 이유 + 확신 스코어 + **컨텍스트 태그** 표시
- **실제 동작**: 자연어 이유 + 확신 스코어만 표시, 컨텍스트 태그 없음
- **원인**: 폴백 추천 데이터에 컨텍스트 태그 필드 미포함 또는 UI 미구현
- **영향 파일**: `frontend/src/app/(main)/home/page.tsx` (추천 이유 다이얼로그 섹션)
- **담당**: frontend-developer
- **스크린샷**: `.temp/tc-10.png`

---

### [CRITICAL] TC-14: "먹었어요!" API 연동 실패 (UTC 시간 전송 버그)

- **현상**: 실제 로그인 상태에서 "먹었어요!" 클릭 시 항상 실패
- **재현 절차**: `/meal-record?restaurantId=rest-004&name=종로 한식뷔페` 접근 → "먹었어요!" 클릭
- **기대 동작**: 식사 기록 완료 + 체크 애니메이션 + 카운트다운 바 표시
- **실제 동작**: `POST /api/v1/meals` → 400 `INVALID_MEAL_TIME` (점심 식사 기록은 10:30~15:00 사이에만 가능)
- **원인**: 프론트엔드가 `new Date().toISOString()`으로 UTC 시간 전송 (예: `2026-02-27T17:47:00.000Z`), 백엔드는 이를 UTC 시간으로 해석하여 KST 12:47을 UTC 17:47로 처리 → 점심 시간 범위 검증 실패
- **영향 파일**:
  - `frontend/src/app/(main)/meal-record/page.tsx` (88번 줄: `recordedAt: new Date().toISOString()`)
  - `recommendation-service/src/main/java/.../service/impl/MealServiceImpl.java` (시간대 검증 로직)
- **담당**: frontend-developer (UTC→KST 변환 필요), backend-developer (시간대 처리 정책 확인)
- **스크린샷**: `.temp/tc-14-fail.png`

---

### [MAJOR] TC-15: 중복 기록 다이얼로그 미표시

- **현상**: 동일 날짜 중복 식사 기록 시 "이미 기록" 다이얼로그 미표시
- **재현 절차**: 식사 기록 후 동일 restaurantId로 재기록 시도
- **기대 동작**: 409 `DUPLICATE_MEAL_RECORD` → "이미 기록되었어요. 수정하시겠어요?" 다이얼로그
- **실제 동작**: TC-14 버그로 인해 400 `INVALID_MEAL_TIME`이 먼저 발생, 중복 기록 자체에 도달 불가. API 레벨에서는 `DUPLICATE_MEAL_RECORD` 에러 코드 정상 반환 확인, 프론트에서 `toast.info('이미 기록되었어요. 수정하시겠어요?')` 처리 코드 존재하나 실제 동작 불가
- **원인**: TC-14 UTC 시간 버그의 연쇄 영향
- **영향 파일**: `frontend/src/app/(main)/meal-record/page.tsx` (100번 줄)
- **담당**: frontend-developer
- **스크린샷**: `.temp/tc-14-fail.png`

---

### [MAJOR] TC-21: 구독 결제 API 계약 불일치

- **현상**: 프론트엔드 결제 요청 시 백엔드가 필드 인식 실패
- **재현 절차**: `/subscription` → "7일 무료 체험 시작" → 카드 정보 입력 → "결제하기"
- **기대 동작**: 결제 성공 + "프리미엄이 활성화되었어요!" 토스트 + 구독 상태 갱신
- **실제 동작**:
  - 브라우저 UI에서 결제 성공 토스트 표시됨 (201 응답)
  - 그러나 구독 상태가 재진입 시 "무료"로 되돌아옴 (캐시 또는 상태 갱신 버그)
  - 직접 API 호출 시 `UnrecognizedPropertyException: cardNumber` — 프론트가 flat 구조(`cardNumber`, `expiryDate`, `cvc`)로 전송하나 백엔드는 중첩 `paymentMethod` 객체 요구
- **원인**: 프론트엔드가 `paymentMethod: { type, cardNumber, expiryMonth, expiryYear, cvc }` 중첩 구조 대신 flat 필드 전송
- **영향 파일**:
  - `frontend/src/app/(main)/subscription/page.tsx` (결제 요청 페이로드 구성 부분)
  - `payment-service/src/main/java/.../dto/request/CreateSubscriptionRequest.java`
  - `payment-service/src/main/java/.../dto/request/PaymentMethodDto.java`
- **담당**: frontend-developer
- **스크린샷**: `.temp/tc-21.png`

---

### [MAJOR] TC-22: 결제 실패 에러 UI 미확인 (CircuitBreaker OPEN)

- **현상**: 결제 실패 시나리오 재현 불가 — payment-service CircuitBreaker가 OPEN 상태로 모든 결제 호출이 폴백 처리됨
- **재현 절차**: 잘못된 카드 정보로 결제 시도
- **기대 동작**: 결제 실패 에러 UI 표시
- **실제 동작**: CircuitBreaker OPEN으로 인해 폴백(`PAYMENT_FAILED`) 즉시 반환. 에러 메시지 자체는 "결제가 실패했어요. 다른 결제 수단을 시도해주세요." 반환 (브라우저 에러 UI 표시 여부 미확인)
- **원인**: 테스트 환경에서 CB 미리셋. `resilience4j.circuitbreaker.pg-gateway` 임계값 초과 상태
- **영향 파일**: `payment-service/src/main/java/.../service/impl/SubscriptionServiceImpl.java` (CircuitBreaker 설정)
- **담당**: backend-developer (CB 리셋 또는 테스트 환경 CB 비활성화 필요)
- **스크린샷**: 없음 (재현 불가)

---

### [MINOR] TC-24: 구독 해지 API UTF-8 인코딩 이슈

- **현상**: 구독 해지 시 한글 사유 전송 시 500 에러
- **재현 절차**: `DELETE /api/v1/subscriptions/{id}` + 한글 `cancelReasonDetail` 전송
- **기대 동작**: 해지 예약 성공 + "해지가 예약되었어요" 응답
- **실제 동작**: 한글 전송 시 `JsonMappingException: Invalid UTF-8 middle byte` → 500 에러. 영문 전송 시 정상 동작 확인
- **원인**: curl 테스트 환경의 UTF-8 인코딩 문제. 백엔드 `CancelSubscriptionRequest` DTO의 `cancelReasonDetail` 필드 한글 처리 이슈 (Content-Type 인코딩 미지정 또는 jackson UTF-8 설정 누락)
- **영향 파일**:
  - `payment-service/src/main/java/.../dto/request/CancelSubscriptionRequest.java`
  - `payment-service/src/main/resources/application.yml` (인코딩 설정)
- **담당**: backend-developer
- **스크린샷**: 없음 (API 레벨 확인)

---

### [MINOR] GAP-06: 랜딩 페이지 개발용 링크 노출

- **현상**: `/` 랜딩 페이지에 "홈으로 (개발용)" 링크 노출
- **재현 절차**: `http://localhost:3000/` 접근
- **기대 동작**: 프로덕션 빌드 시 개발용 링크 미노출
- **실제 동작**: "홈으로 (개발용)" 링크가 일반 사용자에게 노출
- **영향 파일**: `frontend/src/app/page.tsx`
- **담당**: frontend-developer

---

## 추가 발견 이슈

### [MAJOR] AI 파이프라인 장애 — 모든 추천이 폴백으로 동작

- **현상**: 전체 테스트 기간 동안 "⚠ AI 장애로 기본 추천이 표시되고 있어요" 배너 표시
- **원인**: ai-pipeline-service 또는 recommendation-service의 AI 추천 로직 장애. 폴백 추천(기본 주변 인기 식당)으로 대체 동작 중
- **영향**: TC-07 확신 스코어가 모두 60%로 동일, TC-08 콜드스타트 배너 구분 불가
- **담당**: ai-engineer, backend-developer

### [MINOR] manifest.json 404 — PWA 설정 미완

- **현상**: 모든 페이지에서 `manifest.json` 404 에러 (console.error 발생)
- **원인**: Next.js PWA manifest 파일 미생성
- **영향 파일**: `frontend/public/manifest.json` (미존재)
- **담당**: frontend-developer

---

## PASS TC 목록

| TC | 시나리오 | 결과 | 스크린샷 |
|----|---------|------|---------|
| TC-01 | 카카오 로그인 페이지 UI | PASS | `.temp/tc-01.png` |
| TC-03 | 취향 퀴즈 — 7장 미만 비활성화, 7장 완료 시 활성화 | PASS | `.temp/tc-03.png` |
| TC-04 | 퀴즈 이탈 재진입 — 이어서 진행 (Hydration 에러 있음) | PASS (부분) | `.temp/tc-04.png` |
| TC-05 | 위치 동의 — 동의 시 다음 단계 라우팅 | PASS | `.temp/tc-05.png` |
| TC-06 | 위치 거절 — 비활성화 토스트 표시 | PASS | `.temp/tc-06.png` |
| TC-07 | 홈 화면 — 추천 카드 3개 렌더링 | PASS | `.temp/tc-07.png` |
| TC-08 | 콜드스타트 — AI 장애 폴백 배너 표시 | PASS | `.temp/tc-07.png` |
| TC-09 | 추천 API 오류 — 폴백 UI 존재 | PASS | `.temp/tc-07.png` |
| TC-10 | "왜?" 버튼 — 이유+확신 스코어 (컨텍스트 태그 제외) | PASS (부분) | `.temp/tc-10.png` |
| TC-11 | "여기 갈래요" — accept API 200, 길찾기 전환 | PASS | `.temp/tc-11.png` |
| TC-12 | 거절 스와이프 — 사유 바텀시트, reject API 200 | PASS | `.temp/tc-12-bottomsheet.png` |
| TC-13 | 대체 없음 — "거리를 넓혀볼까요?" 토스트 | PASS | `.temp/tc-12-13.png` |
| TC-14 | "먹었어요!" — 데모 모드 체크+카운트다운 정상 | PASS (데모) | `.temp/tc-14.png` |
| TC-16 | 실행 취소 (20초 내) — 데모 모드 취소 바 정상 | PASS (데모) | `.temp/tc-14.png` |
| TC-17 | 30초 초과 취소 — CANCEL_TIMEOUT + "이력에서 수정" | PASS | 없음 (API) |
| TC-18 | 피드백 좋아요 — "내일 반영" 표시 | PASS | `.temp/tc-18.png` |
| TC-19 | 피드백 건너뛰기 — NEUTRAL 전송 | PASS (코드 확인) | 없음 |
| TC-20 | 구독 관리 — 무료/프리미엄 비교, 가격 표시 | PASS | `.temp/tc-20.png` |
| TC-21 | 구독 결제 — 결제 성공 UI (201) | PASS (부분) | `.temp/tc-21.png` |
| TC-23 | 해지 7일 연장 — "7일 연장" 응답 정상 | PASS (API) | 없음 |
| TC-24 | 해지 완료 — PENDING_CANCEL + 데이터 경고 | PASS (영문) | 없음 |
| GAP-01 | API 에러 Graceful Degradation — 폴백+토스트 | PASS | `.temp/tc-07.png` |
| GAP-02 | 빈 데이터 Empty State — 캘린더 레이아웃 표시 | PASS | `.temp/insights.png` |
| GAP-03 | 환경변수 미설정 Guard — 카카오 앱키 미설정 토스트 | PASS (코드 확인) | 없음 |
| GAP-04 | CSS 레이아웃 375px — 모바일 가독성 정상 | PASS | `.temp/gap-04-375px.png` |
| GAP-05 | SDK 로딩 실패 Fallback — 지도 폴백 UI 표시 | PASS | `.temp/tc-11.png` |
| GAP-07 | 경계값 — 빈 파라미터 시 크래시 없음 | PASS (부분) | 없음 |
| GAP-08 | 데모 모드 E2E — 로그인 없이 전체 플로우 진행 | PASS | `.temp/gap-08-demo.png` |

---

## 버그 우선순위 요약

| 우선순위 | 항목 | 담당 |
|---------|------|------|
| P0 (즉시) | TC-14: UTC 시간 전송 버그 → 식사 기록 전면 불가 | frontend-developer |
| P1 (고) | AI 파이프라인 장애 → 모든 추천 폴백 동작 | ai-engineer |
| P1 (고) | TC-21: 구독 결제 API 계약 불일치 | frontend-developer |
| P2 (중) | TC-15: 중복 기록 다이얼로그 (TC-14 수정 후 재검증) | frontend-developer |
| P2 (중) | TC-22: CircuitBreaker OPEN — 결제 테스트 환경 불가 | backend-developer |
| P3 (저) | TC-02: OAuth 에러 파라미터 미처리 | frontend-developer |
| P3 (저) | TC-04: Hydration 에러 (UI 동작은 정상) | frontend-developer |
| P3 (저) | TC-10: 컨텍스트 태그 미표시 | frontend-developer |
| P3 (저) | TC-24: 한글 해지 사유 인코딩 이슈 | backend-developer |
| P3 (저) | manifest.json 404 (PWA 미설정) | frontend-developer |
| P3 (저) | GAP-06: 개발용 링크 랜딩 페이지 노출 | frontend-developer |
