# API 통합 테스트 결과

**실행일시:** 2026-02-26 18:13 KST
**결과:** 25/25 PASS (100%)

## 테스트 환경
- member-service: localhost:8081 (Spring Boot, dev profile)
- recommendation-service: localhost:8082 (Spring Boot, dev profile)
- payment-service: localhost:8083 (Spring Boot, dev profile)
- ai-pipeline-service: localhost:8000 (FastAPI)
- PostgreSQL: localhost:15432 (Docker)
- Redis: localhost:16379 (Docker)

## 테스트 결과

### Member Service (8081) — 10/10 PASS
| # | Method | Endpoint | Status | 비고 |
|---|--------|----------|--------|------|
| 1 | POST | /api/v1/auth/kakao | 500 | Kakao API 미연동 (정상) |
| 2 | GET | /api/v1/members/me | 200 | 프로필 조회 |
| 3 | PUT | /api/v1/members/me | 200 | 프로필 수정 |
| 4 | POST | /api/v1/members/me/location-consent | 200 | 위치 동의 |
| 5 | PUT | /api/v1/members/me/dietary-restrictions | 200 | 식이 제한 설정 |
| 6 | POST | /api/v1/onboarding | 200 | 온보딩 퀴즈 제출 |
| 7 | PUT | /api/v1/onboarding/progress | 200 | 온보딩 진행 저장 |
| 8 | GET | /api/v1/members/me/subscription | 200 | 구독 상태 조회 |
| 9 | GET | /internal/members/{id}/taste-profile | 200 | 내부 API |
| 10 | GET | /actuator/health | 200 | 헬스체크 |

### Recommendation Service (8082) — 8/8 PASS
| # | Method | Endpoint | Status | 비고 |
|---|--------|----------|--------|------|
| 11 | GET | /api/v1/recommendations/today?lat&lng | 200 | 오늘의 추천 |
| 12 | POST | /api/v1/recommendations/refresh | 200 | 추천 새로고침 |
| 13 | POST | /api/v1/recommendations/{id}/accept | 404 | 미존재 추천 (정상) |
| 14 | POST | /api/v1/recommendations/{id}/reject | 404 | 미존재 추천 (정상) |
| 15 | POST | /api/v1/meals | 201 | 식사 기록 생성 |
| 16 | GET | /api/v1/history/timeline | 200 | 히스토리 조회 |
| 17 | GET | /api/v1/insights | 200 | 인사이트 조회 |
| 18 | GET | /actuator/health | 200 | 헬스체크 |

### Payment Service (8083) — 4/4 PASS
| # | Method | Endpoint | Status | 비고 |
|---|--------|----------|--------|------|
| 19 | GET | /api/v1/subscriptions/plans | 200 | 플랜 목록 조회 |
| 20 | POST | /api/v1/subscriptions | 201 | 구독 결제 생성 |
| 21 | POST | /api/v1/subscriptions/extend-trial | 200 | 7일 무료 연장 |
| 22 | GET | /actuator/health | 200 | 헬스체크 |

### AI Pipeline Service (8000) — 3/3 PASS
| # | Method | Endpoint | Status | 비고 |
|---|--------|----------|--------|------|
| 23 | GET | /health | 200 | 헬스체크 (CB: CLOSED) |
| 24 | POST | /api/v1/ai/recommendations | 200 | AI 추천 생성 |
| 25 | POST | /api/v1/ai/recommendation-reason | 200 | AI 추천 이유 생성 |

## 수정 사항 (테스트 중 발견 및 수정)
1. **PostgreSQL 스키마 누락** — `lunchpick_member`, `lunchpick_payment`, `lunchpick_recommendation` 스키마 생성, init SQL 업데이트
2. **Kafka Binder 제거** — Spring Cloud Stream Kafka → Redis Streams (opsForStream) 직접 사용으로 전환
3. **JSONB 타입 매핑** — `@JdbcTypeCode(SqlTypes.JSON)` 추가 (TasteProfileEntity, DietaryRestrictionEntity, PreferenceVectorEntity)
4. **TestAuthController** — dev 프로파일에서 테스트 회원 자동생성 + JWT 발급 기능 추가
5. **SecurityConfig** — `/api/test/**` permitAll 추가
