# Redis 캐시 DB 설계서

## 1. Redis Database 할당표

| DB 번호 | 용도 | 담당 서비스 | 설명 |
|---------|------|------------|------|
| DB 0 | 공통 영역 | 전체 공통 | 세션, JWT 블랙리스트, 공유 설정 |
| DB 1 | member-service 전용 | member-service | 회원 프로파일, 취향 프로파일 캐시 |
| DB 2 | recommendation-service 전용 | recommendation-service | 추천 결과, 추천 이유 캐시 |
| DB 3 | payment-service 전용 | payment-service | 구독 플랜 목록, 활성 구독 캐시 |
| DB 4 | ai-pipeline-service 전용 | ai-pipeline-service | AI 추천 결과 캐시, AI 추천 이유 캐시, AI 응답 캐시 |
| DB 5~14 | 예비 서비스 | 미정 | 향후 서비스 확장 시 사용 |
| DB 15 | 예비 영역 | - | 긴급 용도 예비 |

---

## 2. 격리 원칙

- 각 서비스는 **자신에게 할당된 database 번호만 접근**
- 타 서비스 database 직접 참조 금지 (예: recommendation-service가 DB 1 접근 불가)
- DB 0 (공통 영역) 데이터 변경 시 **전체 서비스 영향도 분석 필수**
- Key Naming Convention: `{domain}:{entity}:{id}` 형식 권장

---

## 3. DB 0 — 공통 영역

**담당**: 전체 공통 (주로 member-service에서 쓰기, 타 서비스에서 읽기)

| 캐시 키 패턴 | 타입 | TTL | 설명 | 무효화 트리거 |
|-------------|------|-----|------|-------------|
| `session:{member_id}` | String | 1시간 | 세션 정보 (JSON) | 로그아웃 시 즉시 삭제 |
| `jwt:blacklist:{jti}` | String | 토큰 만료 시간 동일 | JWT 블랙리스트 | 자동 만료 |

**Key 예시**:
```
session:01HXYZ1234567890ABCDEF    → {"memberId":"...","plan":"PREMIUM_MONTHLY"}
jwt:blacklist:abc123-def456       → "1"
```

---

## 4. DB 1 — member-service 전용

**담당**: member-service

| 캐시 키 패턴 | 타입 | TTL | 설명 | 무효화 트리거 |
|-------------|------|-----|------|-------------|
| `member:taste_profile:{member_id}` | String (JSON) | 30분 | 취향 프로파일 캐시 | 온보딩 완료, 취향 벡터 갱신 시 |
| `member:profile:{member_id}` | String (JSON) | 10분 | 회원 프로파일 캐시 | 프로파일 업데이트 시 |

**Key 예시**:
```
member:taste_profile:01HXYZ1234  → {"tasteVector":{"한식":0.7,"양식":0.3},"isColdStart":false}
member:profile:01HXYZ1234        → {"nickname":"직장인A","email":"...","dietType":"일반"}
```

---

## 5. DB 2 — recommendation-service 전용

**담당**: recommendation-service

| 캐시 키 패턴 | 타입 | TTL | 설명 | 무효화 트리거 |
|-------------|------|-----|------|-------------|
| `rec:{member_id}:{location_grid}:{weather_code}:{weekday}` | String (JSON) | 당일 13:00까지 | 추천 결과 캐시 | 피드백 저장, 식사 기록, 취향 벡터 갱신 |
| `rec:reason:{recommendation_id}` | String (JSON) | 1시간 | 추천 이유 캐시 | 자동 만료 |
| `rec:stale:{member_id}` | String (JSON) | 24시간 | Stale 추천 캐시 (SWR 패턴) | 취향 벡터 갱신 시 |

**Key 예시**:
```
rec:01HXYZ1234:37560_126985:CLEAR:MON   → {"recommendations":[...],"isFallback":false}
rec:reason:01HREC-ABCD-EFGH            → {"naturalLanguageReason":"날씨가 맑고..."}
rec:stale:01HXYZ1234                   → {"recommendations":[...],"cachedAt":"2026-02-26T12:00:00"}
```

**TTL 계산 로직** (`_calculate_ttl_until_1pm`):
- 현재 시각이 13:00 이전: `13:00 - 현재 시각` (초 단위)
- 현재 시각이 13:00 이후: 다음날 13:00까지 (약 24시간)

---

## 6. DB 3 — payment-service 전용

**담당**: payment-service

| 캐시 키 패턴 | 타입 | TTL | 설명 | 무효화 트리거 |
|-------------|------|-----|------|-------------|
| `plan:list` | String (JSON) | 1시간 | 구독 플랜 목록 캐시 | 플랜 변경 시 관리자 수동 무효화 |
| `subscription:active:{member_id}` | String (JSON) | 10분 | 활성 구독 정보 캐시 | 구독 상태 변경 시 |
| `payment:lock:{member_id}` | String | 30초 | 중복 결제 방지 잠금 | 결제 완료/실패 시 즉시 삭제 |

**Key 예시**:
```
plan:list                         → [{"planId":"PREMIUM_MONTHLY","price":9900,...},...]
subscription:active:01HXYZ1234   → {"planId":"PREMIUM_MONTHLY","expiresAt":"2026-03-26T00:00:00"}
payment:lock:01HXYZ1234          → "1"
```

---

## 7. DB 4 — ai-pipeline-service 전용

**담당**: ai-pipeline-service (Python/FastAPI)

| 캐시 키 패턴 | 타입 | TTL | 설명 | 무효화 트리거 |
|-------------|------|-----|------|-------------|
| `rec:{member_id}:{location_grid}:{weather_code}:{weekday}` | String (JSON) | 당일 13:00까지 | AI 추천 결과 캐시 | 취향 벡터 갱신, 피드백 저장 |
| `rec:reason:{recommendation_id}` | String (JSON) | 1시간 | AI 추천 이유 캐시 | 자동 만료 |
| `ai:response:recommendation:{hash}` | String (JSON) | 30분 | AI LLM 응답 캐시 (프롬프트 해시 기반) | 자동 만료 |
| `ai:response:reason:{hash}` | String (JSON) | 1시간 | AI 이유 생성 LLM 응답 캐시 | 자동 만료 |

**Key 예시**:
```
rec:01HXYZ1234:37560_126985:CLEAR:MON     → {"recommendations":[...],"isFallback":false,"cachedUntil":"..."}
rec:reason:01HREC-ABCD-EFGH              → {"naturalLanguageReason":"...","contextTags":["한식","맑음"]}
ai:response:recommendation:a1b2c3d4e5f6  → {"rawResponse":"...","modelUsed":"gpt-4o","tokenUsage":{...}}
ai:response:reason:f6e5d4c3b2a1          → {"rawResponse":"...","modelUsed":"gpt-4o","tokenUsage":{...}}
```

**ai:response 키 Hash 계산 방식**:
- 입력: 프롬프트 전체 문자열
- 알고리즘: SHA-256 앞 12자리 hex
- 목적: 동일한 컨텍스트(위치+날씨+취향)에서 동일 LLM 응답 재사용

---

## 8. 운영 고려사항

### 8.1 Redis 인스턴스 구성 (AWS ElastiCache)

| 환경 | 구성 | 비고 |
|------|------|------|
| 개발(dev) | Standalone 1노드 | 비용 절감 |
| 스테이징(staging) | Standalone 1노드 | dev와 동일 |
| 운영(prod) | Cluster Mode (3 샤드 x 2 레플리카) | HA 보장 |

### 8.2 메모리 관리

- `maxmemory-policy`: `allkeys-lru` (메모리 한계 도달 시 LRU 방식 자동 eviction)
- DB 4 (AI 응답 캐시): 큰 JSON 저장 가능성 → 별도 노드 그룹 고려

### 8.3 모니터링 지표

| 지표 | 경보 임계값 | 설명 |
|------|------------|------|
| Hit Rate | < 70% | 추천 캐시 적중률 저하 시 AI 파이프라인 부하 증가 |
| Memory Usage | > 80% | eviction 발생 전 스케일 업 |
| Connection Count | > 500 | 커넥션 풀 설정 검토 |

### 8.4 Key 만료 모니터링

- `rec:*` 키: 당일 13:00 일괄 만료 → 13:00 직후 AI 파이프라인 부하 급증 주의
- 완화 방안: 사용자별 요청 시간 분산 + Stale-While-Revalidate 패턴 적용
