# ai-pipeline-service 개발 체크리스트

## 작업 정보
- 담당: AI 엔지니어 (마법사)
- 완료일: 2026-02-26
- 브랜치: main

---

## 구현 완료 항목

### 1. 하위 레이어

| 파일 | 설명 | 상태 |
|------|------|------|
| `llm/circuit_breaker.py` | Circuit Breaker (CLOSED/OPEN/HALF_OPEN 상태 전이, failure_threshold=5, recovery_timeout=60s) | 완료 |
| `cache/cache_manager.py` | Redis Cache-Aside (DB4, TTL=당일 13:00 KST, Stale TTL=24h, 프롬프트 해시 TTL=4h) | 완료 |
| `llm/llm_client.py` | LangChain init_chat_model 추상화, 지수 백오프 Retry(500ms→1s→2s, 최대 3회), Primary→Fallback 모델 전환 | 완료 |

### 2. 프롬프트

| 파일 | 설명 | 상태 |
|------|------|------|
| `prompt/templates/recommendation-system-v1.0.txt` | 일반 추천 시스템 프롬프트 | 완료 |
| `prompt/templates/recommendation-coldstart-v1.0.txt` | 콜드스타트 추천 프롬프트 (직군 클러스터 Prior, confidence 40~65) | 완료 |
| `prompt/templates/reason-system-v1.0.txt` | 추천 이유 생성 프롬프트 (Few-shot 3개) | 완료 |
| `prompt/loader.py` | 파일 기반 템플릿 로더, SHA256[:12] 해시 생성 | 완료 |
| `prompt/recommendation_prompt.py` | RecommendationPromptBuilder, ColdStartPromptBuilder | 완료 |
| `prompt/reason_prompt.py` | ReasonPromptBuilder | 완료 |

### 3. Pydantic 모델

| 파일 | 설명 | 상태 |
|------|------|------|
| `model/common.py` | WeatherCondition, AiSource, CBStateEnum, COLD_START_FEEDBACK_THRESHOLD=5 | 완료 |
| `model/weather_context.py` | WeatherContext (camelCase alias 지원) | 완료 |
| `model/ai_metadata.py` | TokenUsage, AiMetadata | 완료 |
| `model/recommendation_request.py` | AiRecommendationRequest (camelCase alias) | 완료 |
| `model/recommendation_response.py` | AiRecommendationResponse, RecommendedRestaurant | 완료 |
| `model/reason_request.py` | AiReasonRequest | 완료 |
| `model/reason_response.py` | AiReasonResponse, ParsedReason | 완료 |

### 4. 파서

| 파일 | 설명 | 상태 |
|------|------|------|
| `parser/recommendation_parser.py` | Markdown 제거 → JSON 파싱 → Pydantic 검증 | 완료 |
| `parser/reason_parser.py` | JSON 파싱 → ParsedReason, context_tags 유효값 필터 | 완료 |

### 5. 서비스

| 파일 | 설명 | 상태 |
|------|------|------|
| `service/fallback_engine.py` | 반경 500m 조회, 알레르기/제외 필터, Haversine 거리, 상위 3개 반환 | 완료 |
| `service/recommendation_service.py` | 오케스트레이터: 캐시→CB→LLM(일반/콜드스타트)→폴백 3-tier | 완료 |
| `service/reason_service.py` | 오케스트레이터: 캐시→CB→LLM→폴백 | 완료 |

### 6. 라우터

| 파일 | 설명 | 상태 |
|------|------|------|
| `router/recommendation_router.py` | POST /api/v1/ai/recommendations | 완료 |
| `router/reason_router.py` | POST /api/v1/ai/recommendation-reason | 완료 |
| `router/health.py` | GET /health (CB 상태 + Redis 연결 실시간 반환) | 완료 |
| `main.py` | FastAPI 앱 진입점, 라우터 등록 | 완료 |

---

## 테스트 결과

### 단위 테스트 실행 결과
```
pytest tests/ -v
47 passed in 4.49s
```

| 테스트 파일 | 테스트 수 | 결과 |
|-------------|-----------|------|
| `test_circuit_breaker.py` | 8 | PASS |
| `test_cache_manager.py` | 11 | PASS |
| `test_llm_client.py` | 8 | PASS |
| `test_fallback_engine.py` | 9 | PASS |
| `test_recommendation_service.py` | 6 | PASS |
| `test_reason_service.py` | 5 | PASS |
| 합계 | 47 | 47 PASS / 0 FAIL |

---

## 서비스 기동 검증

### /health 응답
```json
{
  "status": "ok",
  "circuit_breaker": {"state": "CLOSED", "failure_count": 0},
  "redis": {"connected": true, "db": 4},
  "llm_provider": "anthropic",
  "uptime_seconds": 3
}
```

### POST /api/v1/ai/recommendations (API 키 미설정 → FALLBACK_RULE_BASED)
```
Status: 200
{
  "recommendations": [...],  // 3개 폴백 추천 정상 반환
  "isFallback": true,
  "metadata": {"source": "FALLBACK_RULE_BASED", ...}
}
```

### POST /api/v1/ai/recommendation-reason (API 키 미설정 → FALLBACK)
```
Status: 200
{
  "isReasonReady": false,
  "fallbackReason": "추천 이유를 준비 중이에요.",
  "metadata": {"source": "FALLBACK_RULE_BASED", ...}
}
```

---

## 폴백 동작 검증 (API 키 없는 환경)

- LLM 호출 실패 → Circuit Breaker 카운트 증가 확인
- Primary(Haiku) 실패 → Fallback(Sonnet) 자동 전환 확인
- 최종 실패 → FALLBACK_RULE_BASED 응답 (200 OK) 확인
- 추천 이유: `isReasonReady=false`, `fallbackReason` 포함 확인

---

## 구현 제약 준수 여부

| 제약 | 준수 여부 |
|------|-----------|
| TODO/FIXME/HACK 금지 | 준수 |
| LLM API 키 하드코딩 금지 | 준수 (환경변수 ANTHROPIC_API_KEY 사용) |
| 테스트 시 LLM mock 필수 | 준수 (AsyncMock으로 모든 LLM 호출 mock) |
| LLM 장애 시 200 반환 | 준수 (폴백 추천 포함 200 OK) |
| Pydantic v2 + camelCase alias | 준수 |
