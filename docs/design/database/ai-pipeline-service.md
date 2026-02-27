# ai-pipeline-service 데이터 설계서

## 데이터설계 요약

| 항목 | 내용 |
|------|------|
| 서비스명 | ai-pipeline-service |
| DBMS | 없음 (Python/FastAPI, DB 미사용) |
| 테이블 수 | 0 |
| 캐시 DB | Redis DB 4 (AI 추천 결과 캐시, AI 추천 이유 캐시, AI LLM 응답 캐시) |
| 아키텍처 패턴 | 해당 없음 (Stateless API 서버) |
| 데이터 독립성 | 영속 데이터 없음. 캐시(DB 4)만 사용 |

---

## 1. 설계 원칙

- **Stateless**: ai-pipeline-service는 자체 영속 데이터베이스를 보유하지 않음
- **캐시 전용**: 모든 데이터는 Redis DB 4에 임시 저장 (TTL 기반 자동 만료)
- **멱등성 보장**: 동일한 입력(위치+날씨+취향 컨텍스트)에 대해 동일한 AI 응답 반환
- **LLM 비용 최적화**: 프롬프트 해시 기반 캐싱으로 중복 LLM 호출 방지

---

## 2. 영속 데이터베이스 미사용 이유

| 이유 | 설명 |
|------|------|
| Stateless 설계 | 추천 결과의 영속성은 recommendation-service가 담당 |
| 비용 절감 | AI 응답 특성상 TTL 기반 캐시로 충분, RDB 불필요 |
| 확장성 | Stateless이므로 수평 스케일 아웃 자유로움 |
| 단순성 | Python/FastAPI 특성상 Redis 직접 사용이 효율적 |

---

## 3. Redis DB 4 캐시 설계

### 3.1 AI 추천 결과 캐시

| 항목 | 내용 |
|------|------|
| 키 패턴 | `rec:{member_id}:{location_grid}:{weather_code}:{weekday}` |
| 타입 | String (JSON 직렬화) |
| TTL | 당일 13:00까지 (CacheManager._calculate_ttl_until_1pm()) |
| 무효화 트리거 | 피드백 저장, 식사 기록, 취향 벡터 갱신 이벤트 수신 시 |

**저장 데이터 구조**:
```json
{
  "recommendations": [
    {
      "restaurant_id": "REST-001",
      "restaurant_name": "강남 해장국",
      "representative_menu": "소고기 해장국",
      "category": "한식",
      "reason_summary": "날씨가 쌀쌀한 날 따뜻한 국물 요리",
      "confidence_score": 85,
      "distance_meters": 320,
      "estimated_walk_minutes": 4
    }
  ],
  "is_fallback": false,
  "is_cold_start": false,
  "cold_start_tag": null,
  "cache_key": "rec:01HXYZ1234:37560_126985:CLEAR:MON",
  "cached_until": "2026-02-26T13:00:00",
  "metadata": {
    "source": "ai_llm",
    "model_used": "gpt-4o",
    "latency_ms": 1240,
    "token_usage": {"prompt_tokens": 850, "completion_tokens": 320, "total_tokens": 1170},
    "circuit_breaker_state": "CLOSED"
  }
}
```

---

### 3.2 AI 추천 이유 캐시

| 항목 | 내용 |
|------|------|
| 키 패턴 | `rec:reason:{recommendation_id}` |
| 타입 | String (JSON 직렬화) |
| TTL | 1시간 |
| 무효화 트리거 | 자동 만료 |

**저장 데이터 구조**:
```json
{
  "recommendation_id": "01HREC-ABCD-EFGH-IJKL",
  "natural_language_reason": "오늘처럼 맑고 선선한 날씨에는 한식 국물 요리가 잘 맞아요. 최근 3일간 양식을 드셨으니 오늘은 한식이 좋을 것 같아요.",
  "confidence_score": 85,
  "context_tags": ["맑은날씨", "한식선호", "최근양식회피"],
  "is_reason_ready": true,
  "fallback_reason": null,
  "cached_until": "2026-02-26T14:00:00",
  "metadata": {
    "source": "ai_llm",
    "model_used": "gpt-4o",
    "latency_ms": 980,
    "token_usage": {"prompt_tokens": 620, "completion_tokens": 180, "total_tokens": 800},
    "circuit_breaker_state": "CLOSED"
  }
}
```

---

### 3.3 AI LLM 응답 캐시 (비용 최적화)

| 항목 | 내용 |
|------|------|
| 키 패턴 | `ai:response:recommendation:{sha256_hash_12}` |
| 타입 | String (JSON 직렬화) |
| TTL | 30분 |
| 무효화 트리거 | 자동 만료 |

| 항목 | 내용 |
|------|------|
| 키 패턴 | `ai:response:reason:{sha256_hash_12}` |
| 타입 | String (JSON 직렬화) |
| TTL | 1시간 |
| 무효화 트리거 | 자동 만료 |

**Hash 계산 방식**:
```python
import hashlib

def build_cache_key(prompt: str) -> str:
    return hashlib.sha256(prompt.encode()).hexdigest()[:12]
```

---

## 4. CacheManager 동작 방식

```
[추천 요청 수신]
       |
       v
[캐시 키 생성] → rec:{member_id}:{location_grid}:{weather_code}:{weekday}
       |
       v
[Redis DB 4 조회]
       |
    Hit?
   /      \
Yes        No
 |          |
 v          v
[캐시 반환]  [LLM 응답 캐시 조회] → ai:response:recommendation:{hash}
              |
           Hit?
          /      \
        Yes        No
         |          |
         v          v
      [LLM 캐시 반환]  [LLM API 호출]
                         |
                         v
                    [응답 파싱]
                         |
                         v
                    [rec:* 캐시 저장]
                    [ai:response:* 캐시 저장]
                         |
                         v
                    [결과 반환]
```

---

## 5. Stale-While-Revalidate 패턴

13:00 만료 직후 다수 사용자 동시 요청으로 인한 Cache Stampede 방지:

| 단계 | 설명 |
|------|------|
| 1. Stale 제공 | 만료된 캐시를 즉시 반환 (rec:stale:{member_id}) |
| 2. 백그라운드 갱신 | 비동기로 LLM 호출 및 캐시 갱신 |
| 3. 갱신 완료 | 이후 요청부터 신선한 캐시 제공 |

---

## 6. Circuit Breaker 상태와 Fallback

| 상태 | 설명 | 캐시 동작 |
|------|------|----------|
| CLOSED | 정상 | LLM 호출 후 캐시 저장 |
| HALF_OPEN | 복구 중 | 제한적 LLM 호출, 캐시 우선 |
| OPEN | 장애 | Stale 캐시 또는 FallbackEngine 사용 |

**Fallback 추천 키 패턴** (FallbackEngine 사용 시):
```
rec:fallback:{location_grid}:{category}  →  TTL: 6시간 (인기 식당 기반)
```
