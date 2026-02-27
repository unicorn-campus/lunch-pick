# AI 기반 인사이트 분석 구현 계획서 (PRD)

**작성일:** 2026-02-27
**작성자:** Planner (Claude Opus 4.6)
**상태:** Final (Consensus Approved)
**대상 버전:** Phase 1 MVP 확장

---

## 1. 배경 및 목적

### 현재 상태
- 인사이트 페이지(`/insights?tab=insight`)는 **카테고리 Top 5 통계 막대그래프**만 표시
- 백엔드(`HistoryServiceImpl.getTasteInsights`)는 `weeklyPattern`, `satisfactionTrend`, `weeklySummary`를 **빈값/null로 반환**
- `InsightsResponse` DTO에 이미 해당 필드가 선언되어 있으나 데이터가 채워지지 않음
- 프론트엔드는 이미 `weeklySummary`, `satisfactionTrend`, `weeklyPattern` 렌더링 코드가 존재 (데이터가 없어서 표시되지 않는 것뿐)

### 목표
기존 SQL 집계만 하던 인사이트에 **LLM 기반 AI 분석 3가지**를 추가하여 프리미엄 구독 가치를 높인다.

### 비즈니스 규칙
| 플랜 | 동작 |
|------|------|
| **PREMIUM** | 실제 AI 분석 결과 표시 |
| **FREE** | 블러 처리된 샘플 데이터 + "프리미엄에서 AI 인사이트를 확인하세요" 구독 유도 UI |

---

## 2. 구현할 3가지 AI 인사이트

### 2.1 주간 식습관 리포트 (Weekly Eating Report)
- **데이터 소스:** 최근 7일 식사 기록 (MealRecordEntity + FeedbackEntity)
- **LLM 입력:** 날짜, 식당명, 카테고리, 메뉴, 만족도, 키워드 목록
- **LLM 출력:** 자연어 분석문 (2~3문장, 200자 이내)
- **매핑 필드:** `InsightsResponse.weeklySummary` (기존 필드 활용)
- **예시 출력:** "이번 주는 한식 위주로 드셨고, 수요일 일식 점심의 만족도가 가장 높았어요. 주 후반에 다양한 카테고리를 시도해보는 것은 어떨까요?"

### 2.2 식사 밸런스 진단 (Meal Balance Diagnosis)
- **데이터 소스:** 최근 30일 카테고리 분포 (기존 topCategories 데이터 재활용)
- **LLM 입력:** 카테고리별 비율, 다양성 지수(unique categories / total meals), 편중 카테고리
- **LLM 출력:** JSON 구조 `{ diversityScore: number (0~100), diagnosis: string, coachingComment: string }`
- **매핑 필드:** `InsightsResponse`에 새 필드 `mealBalance` 추가
- **예시 출력:** `{ diversityScore: 45, diagnosis: "한식 편중", coachingComment: "한식 비율이 60%로 다소 편중되어 있어요. 일식이나 샐러드로 변화를 줘보세요." }`

### 2.3 만족도 패턴 분석 (Satisfaction Pattern Analysis)
- **데이터 소스:** 최근 30일 피드백 (GOOD/BAD/NEUTRAL + keyword)
- **LLM 입력:** 만족/불만족 식사의 카테고리, 키워드, 요일 분포
- **LLM 출력:** JSON 구조 `{ satisfactionRate: number, patterns: string[], patternComment: string }`
- **매핑 필드:** `InsightsResponse`에 새 필드 `satisfactionAnalysis` 추가
- **예시 출력:** `{ satisfactionRate: 78, patterns: ["한식 만족도 높음", "금요일 만족도 낮음"], patternComment: "한식을 드실 때 만족도가 높고, 금요일엔 새로운 시도가 오히려 만족도를 낮추는 경향이 있어요." }`

---

## 3. 변경 대상 파일 목록

### 3.1 AI Pipeline Service (FastAPI, port 8084)

| 구분 | 파일 | 변경 내용 |
|------|------|-----------|
| **신규** | `ai-pipeline-service/router/insight_router.py` | `POST /api/v1/ai/insights` 엔드포인트 |
| **신규** | `ai-pipeline-service/model/insight_request.py` | `AiInsightRequest` Pydantic 모델 |
| **신규** | `ai-pipeline-service/model/insight_response.py` | `AiInsightResponse` Pydantic 모델 |
| **신규** | `ai-pipeline-service/prompt/insight_prompt.py` | `InsightPromptBuilder` 클래스 |
| **신규** | `ai-pipeline-service/prompt/templates/insight-system-v1.0.txt` | 인사이트 분석 시스템 프롬프트 |
| **신규** | `ai-pipeline-service/service/insight_service.py` | `InsightService` 오케스트레이터 |
| **신규** | `ai-pipeline-service/parser/insight_parser.py` | LLM 응답 JSON 파싱기 |
| **수정** | `ai-pipeline-service/main.py` | insight_router 등록 |
| **수정** | `ai-pipeline-service/prompt/loader.py` | `ACTIVE_INSIGHT_TEMPLATE` 상수 추가 |
| **수정** | `ai-pipeline-service/service/mock_llm_engine.py` | `generate_insights()` 목 메서드 추가 |

### 3.2 Recommendation Service (Spring Boot, port 8082)

| 구분 | 파일 | 변경 내용 |
|------|------|-----------|
| **수정** | `recommendation-service/.../dto/response/InsightsResponse.java` | `mealBalance`, `satisfactionAnalysis` 필드 추가 |
| **수정** | `recommendation-service/.../service/impl/HistoryServiceImpl.java` | AI 인사이트 호출 로직 + 프리미엄 분기 추가 |
| **수정** | `recommendation-service/.../controller/InsightController.java` | `UserPrincipal`에서 구독 상태 전달 |
| **수정** | `recommendation-service/.../client/AiPipelineClient.java` | `getInsightAnalysis()` 메서드 추가 |
| **수정** | `recommendation-service/.../client/AiPipelineClientImpl.java` | 인사이트 AI 호출 구현 + CB 보호 |
| **신규** | `recommendation-service/.../client/dto/AiInsightRequest.java` | AI 인사이트 요청 DTO |
| **신규** | `recommendation-service/.../client/dto/AiInsightResponse.java` | AI 인사이트 응답 DTO |
| **수정** | `recommendation-service/.../service/HistoryService.java` | `getTasteInsights` 시그니처 유지 (isPremium 불필요 — 백엔드는 항상 AI 포함 반환) |
| **수정** | `recommendation-service/.../service/impl/MealServiceImpl.java` | 식사 기록/피드백 제출 시 `insight:{memberId}` Redis 캐시 삭제 |
| **수정** | `recommendation-service/.../repository/jpa/MealRecordRepository.java` | 요일별 카테고리 집계 `@Query` 메서드 추가 |
| **수정** | `recommendation-service/.../repository/jpa/FeedbackRepository.java` | 주간 만족도 집계 `@Query` 메서드 추가 |

### 3.3 Frontend (Next.js)

| 구분 | 파일 | 변경 내용 |
|------|------|-----------|
| **수정** | `frontend/src/types/recommendation.ts` | `MealBalance`, `SatisfactionAnalysis` 타입 + `InsightsResponse` 필드 추가 |
| **수정** | `frontend/src/app/(main)/insights/page.tsx` | AI 인사이트 3개 카드 UI + 블러/구독유도 컴포넌트 |
| **수정** | `frontend/src/hooks/useRecommendation.ts` | `useInsights` 훅에 isPremium 연동 (변경 불필요 -- 기존 훅 그대로 사용 가능) |

---

## 4. 새로 추가할 API 엔드포인트 명세

### 4.1 AI Pipeline 내부 API

```
POST /api/v1/ai/insights
```

**요청 (AiInsightRequest):**
```json
{
  "memberId": "string",
  "recentMeals": [
    {
      "date": "2026-02-27",
      "restaurantName": "광화문 된장마을",
      "menuName": "된장찌개 정식",
      "category": "한식",
      "satisfaction": "GOOD",
      "keyword": "TASTE"
    }
  ],
  "categoryDistribution": {
    "한식": 0.45,
    "일식": 0.25,
    "양식": 0.15,
    "중식": 0.10,
    "샐러드/건강식": 0.05
  },
  "totalMealCount": 28,
  "periodDays": 30
}
```

**응답 (AiInsightResponse):**
```json
{
  "weeklySummary": "이번 주는 한식과 일식을 골고루 드셨어요. ...",
  "mealBalance": {
    "diversityScore": 65,
    "diagnosis": "양호",
    "coachingComment": "다양한 카테고리를 시도하고 계시네요. ..."
  },
  "satisfactionAnalysis": {
    "satisfactionRate": 78,
    "patterns": ["한식 만족도 높음", "금요일 만족도 낮음"],
    "patternComment": "한식을 드실 때 만족도가 높고 ..."
  },
  "metadata": {
    "source": "LLM",
    "modelUsed": "claude-3-5-haiku-20241022",  // Mock 모드 시 "mock-rule-engine-v1"
    "latencyMs": 1200
  }
}
```

### 4.2 기존 인사이트 API 변경 (recommendation-service)

```
GET /api/v1/insights
```

**응답 변경 (InsightsResponse 확장):**

기존 필드는 그대로 유지하고, 아래 필드가 추가/채워짐:

| 필드 | 변경 | 설명 |
|------|------|------|
| `weeklySummary` | **기존 null -> AI 생성 텍스트** | 주간 식습관 리포트 (AI 생성) |
| `weeklyPattern` | **기존 빈 리스트 -> SQL 집계** | 요일별 패턴 (SQL로 채움, AI 불필요) |
| `satisfactionTrend` | **기존 빈 리스트 -> SQL 집계** | 만족도 트렌드 (SQL로 채움, AI 불필요) |
| `mealBalance` | **신규** | 식사 밸런스 진단 (AI 생성) |
| `satisfactionAnalysis` | **신규** | 만족도 패턴 분석 (AI 생성) |
| `isAiGenerated` | **신규** | AI 분석 포함 여부 (백엔드는 항상 true 반환, 프론트엔드에서 `isPremium && isAiGenerated`로 표시 분기) |

---

## 5. AI 프롬프트 전략

### 5.1 시스템 프롬프트 (`insight-system-v1.0.txt`)

```
당신은 LunchPick의 식사 분석 AI입니다.
사용자의 최근 식사 기록을 분석하여 3가지 인사이트를 생성합니다.

반드시 아래 JSON 형식으로만 응답하세요:
{
  "weeklySummary": "최근 7일 식사 패턴에 대한 자연어 분석 (200자 이내, 친근한 톤)",
  "mealBalance": {
    "diversityScore": 0~100 정수 (카테고리 다양성 점수),
    "diagnosis": "매우 다양" | "양호" | "약간 편중" | "편중",
    "coachingComment": "식사 밸런스에 대한 코칭 코멘트 (150자 이내)"
  },
  "satisfactionAnalysis": {
    "satisfactionRate": 0~100 정수 (전체 만족 비율),
    "patterns": ["발견된 패턴 1", "발견된 패턴 2"],
    "patternComment": "만족도 패턴에 대한 분석 코멘트 (150자 이내)"
  }
}

분석 규칙:
- diversityScore: (고유 카테고리 수 / 전체 식사 수) * 100 기반으로 LLM이 보정
- diagnosis 기준: 80+ 매우 다양, 60~79 양호, 40~59 약간 편중, 0~39 편중
- 톤: 친근하고 긍정적. 부정적 피드백도 건설적으로 표현
- 한국어로 응답
```

### 5.2 사용자 프롬프트 구성 (InsightPromptBuilder)

```
## 회원 식사 기록 요약
- 분석 기간: 최근 {periodDays}일
- 총 식사 수: {totalMealCount}끼

## 카테고리 분포
{categoryDistribution 텍스트}

## 최근 7일 상세 기록
{recentMeals 리스트 - 날짜, 식당, 카테고리, 만족도}

## 피드백 통계
- GOOD: {goodCount}건, BAD: {badCount}건, NEUTRAL: {neutralCount}건
- 만족 키워드: TASTE {tasteCount}, PRICE {priceCount}, KINDNESS {kindnessCount}

위 데이터를 분석하여 JSON 형식으로 응답해주세요.
```

### 5.3 LLM 모델 선택
- **Primary:** Claude 3.5 Haiku (reason_model 재활용, 비용 효율)
- **Fallback:** Claude 3.5 Sonnet (장애 시 자동 전환, 기존 CB 패턴 그대로)
- **Temperature:** 0.4 (분석 정확성과 자연스러움 밸런스)
- **Max Tokens:** 1024 (한국어 3개 인사이트 JSON 안전 마진 확보)

### 5.4 Mock LLM 대응
- `MockLLMEngine.generate_insights()` 추가
- 카테고리 분포 기반 규칙 엔진으로 정적 인사이트 생성
- `source: LLM`으로 반환하여 프론트엔드에서 동일하게 처리
- `metadata.modelUsed`를 `"mock-rule-engine-v1"`로 설정 (기존 `generate_recommendations()`, `generate_reason()`과 동일 패턴)

---

## 6. 프리미엄/무료 분기 전략

### 6.1 isPremium 확인 메커니즘

**결정: 프론트엔드 분기 방식 채택**

현재 코드 상태:
- `HistoryController`는 `isPremium = false` 하드코딩 (line 55)
- `UserPrincipal`에 isPremium 필드 없음
- recommendation-service에 SubscriptionService 없음
- 프론트엔드에는 기존 `useSubscriptionStatus()` 훅이 존재 (`isPremium` 반환)

MVP 전략:
- **백엔드는 isPremium과 무관하게 항상 AI 인사이트를 생성하여 반환**하되, 응답에 `isAiGenerated` 플래그를 포함
- `InsightController`에서는 기존 `HistoryController`와 동일하게 처리 (isPremium 하드코딩 false, 차후 member-service 내부 API 호출로 개선)
- **프론트엔드의 기존 `useSubscriptionStatus()` 훅**(`isPremium`)으로 AI 인사이트 표시 분기

이유: cross-service 호출 추가 없이 기존 패턴 유지, 프론트엔드에 이미 구독 상태 훅 존재

### 6.2 백엔드 동작 (recommendation-service)

```
InsightController.getTasteInsights():
  1. JWT에서 memberId 추출
  2. isPremium = false (하드코딩, 기존 HistoryController와 동일 패턴)
     → 차후 member-service 내부 API 호출로 개선
  3. SQL 집계 + ai-pipeline-service 호출 (항상 수행)
  4. 전체 데이터 반환 (isAiGenerated=true)
     → isPremium 여부와 무관하게 AI 인사이트를 항상 포함
```

### 6.3 프론트엔드 분기

```
InsightsContent:
  const { isPremium } = useSubscriptionStatus()  // 기존 훅 활용

  if (isPremium && data.isAiGenerated):
    -> AI 인사이트 카드 3개 정상 렌더링
  else:
    -> 블러 처리된 샘플 카드 + 구독 유도 오버레이
    -> "프리미엄에서 AI 인사이트를 확인하세요" CTA 버튼
```

### 6.4 블러 UI 구현 방식
- CSS `filter: blur(6px)` + `pointer-events: none` 으로 하드코딩 샘플 데이터를 블러 처리
- 그 위에 반투명 오버레이(`bg-white/80`) + 자물쇠 아이콘 + CTA 버튼
- 기존 `useSubscriptionStatus()` 훅 + `isPremium` 변수 그대로 활용

---

## 7. 구현 순서 (의존성 기반)

### Step 1: AI Pipeline - 인사이트 엔드포인트 (독립)
1. `AiInsightRequest`, `AiInsightResponse` Pydantic 모델 생성
2. `insight-system-v1.0.txt` 시스템 프롬프트 작성
3. `InsightPromptBuilder` 구현 (사용자 프롬프트 조립)
4. `InsightResponseParser` 구현 (LLM JSON 응답 파싱)
5. `InsightService` 오케스트레이터 구현 (LLM 호출 + 파싱 + 폴백)
6. `insight_router.py` 엔드포인트 구현
7. `main.py`에 라우터 등록
8. `loader.py`에 `ACTIVE_INSIGHT_TEMPLATE` 상수 추가
9. `MockLLMEngine.generate_insights()` 구현

**수락 기준:**
- `POST /api/v1/ai/insights` 호출 시 3가지 AI 분석이 JSON으로 반환됨
- LLM 키 없이도 Mock 엔진이 규칙 기반 인사이트를 반환함
- LLM 장애 시 폴백 인사이트가 반환됨 (500 에러 없음)

### Step 2: Recommendation Service - AI 연동 + SQL 집계 (Step 1 의존)
1. `AiInsightRequest.java`, `AiInsightResponse.java` DTO 생성
2. `AiPipelineClient` 인터페이스에 `getInsightAnalysis()` 메서드 추가
3. `AiPipelineClientImpl`에 구현 + CB 폴백 추가 (기존 `BLOCK_TIMEOUT = 35초` 공유)
4. `InsightsResponse.java`에 `mealBalance`, `satisfactionAnalysis`, `isAiGenerated` 필드 추가
5. `HistoryServiceImpl.getTasteInsights()`에서 AI 호출 + SQL 집계 로직 추가 (isPremium 무관, 항상 수행)
6. `MealRecordRepository.java`에 요일별 카테고리 집계 `@Query` 메서드 추가
7. `FeedbackRepository.java`에 주간 만족도 집계 `@Query` 메서드 추가
8. `MealServiceImpl.java`에서 식사 기록/피드백 제출 시 `insight:{memberId}` Redis 캐시 삭제
9. `InsightController`에서 isPremium = false 하드코딩 (기존 `HistoryController` 동일 패턴)
10. `HistoryService` 인터페이스 — 기존 `getTasteInsights(String memberId)` 시그니처 유지 (isPremium 파라미터 불필요, 백엔드는 항상 AI 포함 반환)

#### SQL 집계 쿼리 시그니처 (JPQL)

**weeklyPattern -- 요일별 최다 카테고리 + 평균 만족도 (`MealRecordRepository`):**
```java
@Query(value = """
    SELECT EXTRACT(DOW FROM m.recorded_at) AS day_of_week,
           m.category AS category,
           COUNT(*) AS cnt
    FROM meal_records m
    WHERE m.member_id = :memberId
      AND m.recorded_at >= :fromDate
    GROUP BY EXTRACT(DOW FROM m.recorded_at), m.category
    ORDER BY day_of_week, cnt DESC
    """, nativeQuery = true)
List<Object[]> findWeeklyCategoryDistribution(
        @Param("memberId") String memberId,
        @Param("fromDate") LocalDateTime fromDate);
```

**satisfactionTrend -- 주간별 만족 비율 (`FeedbackRepository`):**
```java
@Query(value = """
    SELECT to_char(f.created_at, 'IYYY-IW') AS year_week,
           COUNT(*) AS total,
           SUM(CASE WHEN f.satisfaction = 'GOOD' THEN 1 ELSE 0 END) AS good_count
    FROM feedbacks f
    WHERE f.member_id = :memberId
      AND f.created_at >= :fromDate
    GROUP BY to_char(f.created_at, 'IYYY-IW')
    ORDER BY year_week
    """, nativeQuery = true)
List<Object[]> findWeeklySatisfactionTrend(
        @Param("memberId") String memberId,
        @Param("fromDate") LocalDateTime fromDate);
```

**수락 기준:**
- `GET /api/v1/insights` 호출 시 AI 분석이 항상 포함된 응답 수신 (isAiGenerated=true)
- weeklyPattern, satisfactionTrend가 SQL 집계로 채워짐
- ai-pipeline-service 장애 시에도 기존 통계는 정상 반환 (AI 필드만 null)
- 식사 기록/피드백 제출 시 `insight:{memberId}` Redis 캐시가 삭제됨

### Step 3: Frontend - AI 인사이트 카드 UI (Step 2 의존)
1. `recommendation.ts`에 `MealBalance`, `SatisfactionAnalysis` 타입 추가 + `InsightsResponse` 확장
2. `insights/page.tsx`에 AI 인사이트 카드 3개 추가:
   - 주간 식습관 리포트 카드 (weeklySummary)
   - 식사 밸런스 진단 카드 (diversityScore 원형 게이지 + 코칭 코멘트)
   - 만족도 패턴 분석 카드 (패턴 태그 + 코멘트)
3. 프리미엄/무료 분기 UI 구현:
   - 블러 처리 + 구독 유도 오버레이
   - 기존 `isPremium` 변수 활용
4. DEMO_INSIGHTS 데모 데이터 업데이트

**수락 기준:**
- 프리미엄 사용자: AI 인사이트 3개 카드가 정상 렌더링됨
- 무료 사용자: 블러 처리된 샘플 + "프리미엄에서 AI 인사이트를 확인하세요" CTA 표시
- CTA 클릭 시 `/subscription` 페이지로 이동
- API 에러 시 데모 데이터로 graceful fallback
- 로딩 중 스켈레톤 UI 표시

---

## 8. 성공 기준 (전체)

| # | 기준 | 검증 방법 |
|---|------|-----------|
| 1 | AI 인사이트 3가지가 프리미엄 사용자에게 표시됨 | 프리미엄 상태로 인사이트 페이지 접근 후 3개 카드 확인 (`useSubscriptionStatus().isPremium = true`) |
| 2 | 무료 사용자에게 블러 처리 + 구독 유도 UI 표시 | 무료 상태로 인사이트 페이지 접근 후 블러/CTA 확인 (`useSubscriptionStatus().isPremium = false`) |
| 3 | LLM 장애 시에도 기존 통계는 정상 작동 | ai-pipeline-service 중지 후 인사이트 페이지 접근 |
| 4 | Mock 모드에서도 인사이트가 반환됨 | LLM API 키 없이 서비스 기동 후 인사이트 확인 |
| 5 | weeklyPattern, satisfactionTrend가 SQL로 채워짐 | 10건 이상 기록 후 인사이트 API 응답에서 빈 리스트 아닌지 확인 |
| 6 | 기존 기능(Top 5, 마일스톤) 회귀 없음 | 기존 인사이트 탭 기능 정상 동작 확인 |

---

## 9. 기술 결정 사항

### 9.1 단일 LLM 호출 vs 3회 분리 호출
- **결정: 단일 호출** -- 3가지 인사이트를 하나의 프롬프트로 요청
- **이유:** 비용 절감 (토큰 오버헤드 1회), 레이턴시 감소, 프롬프트 간 컨텍스트 공유

### 9.2 동기 vs 비동기 AI 호출
- **결정: 동기 호출** (recommendation-service -> ai-pipeline-service REST, 기존 패턴 그대로)
- **이유:** 기존 CB + 타임아웃 인프라 활용, 인사이트 페이지 접근 빈도가 추천보다 낮아 부하 적음
- **타임아웃:** 35초 (기존 `AiPipelineClientImpl.BLOCK_TIMEOUT = Duration.ofSeconds(35)` 공유, readTimeout 30s + 5s 여유)

### 9.3 캐싱 전략
- **결정:** Redis 캐시 TTL 6시간 (키: `insight:{memberId}`)
- **이유:** 인사이트는 하루 중 크게 변하지 않으므로 추천(1시간)보다 긴 TTL 적용
- **캐시 무효화 트리거:** `MealServiceImpl.java`에서 식사 기록 생성/피드백 제출 시 `stringRedisTemplate.delete("insight:" + memberId)` 호출
  - `createMeal()` -- 새 식사 기록 시 인사이트 캐시 삭제
  - `submitFeedback()` -- 피드백 제출 시 인사이트 캐시 삭제

### 9.4 InsightsResponse 하위 호환성
- 신규 필드(`mealBalance`, `satisfactionAnalysis`, `isAiGenerated`)는 모두 nullable
- 기존 클라이언트는 새 필드를 무시하므로 하위 호환 유지

---

## 10. 리스크 및 완화 방안

| 리스크 | 영향 | 완화 |
|--------|------|------|
| LLM 응답 형식 불일치 | 파싱 실패 | InsightResponseParser에 관대한 파싱 + 기본값 폴백 |
| LLM 레이턴시 35초 초과 | 인사이트 페이지 로딩 지연 | 타임아웃 35초 (기존 BLOCK_TIMEOUT 공유), CB 보호, 기존 SQL 데이터 먼저 반환 |
| 데이터 부족 (10건 미만) | 무의미한 AI 분석 | 기존 `hasEnoughData` 체크 유지, AI 호출 자체를 스킵 |
| 프리미엄 분기 누락 | 무료 사용자에게 AI 노출 | 프론트엔드 `useSubscriptionStatus()` 훅 분기 + 백엔드는 항상 AI 포함 반환 (MVP 단계, 차후 백엔드 서버사이드 분기 추가) |

---

## 11. 범위 외 (Out of Scope)

- 인사이트 결과 공유 기능 (SNS 카드 이미지 생성)
- 월간/분기별 트렌드 리포트
- 인사이트 푸시 알림
- 인사이트 페이지 A/B 테스트
- weeklyPattern/satisfactionTrend 차트 시각화 고도화 (기존 단순 UI 유지)
