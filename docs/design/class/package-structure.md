# 런치픽(LunchPick) 전체 패키지 구조

**작성자**: 홍길동 (아키) + 한승우 (마법사)
**기준**: `standard_package_structure.md` — Layered Architecture (Java/Spring Boot 3서비스) + Python/FastAPI (AI Pipeline)

---

## 1. 회원 서비스 (member-service)

**패키지 루트**: `com.unicorn.lunchpick.member`
**아키텍처**: Layered Architecture
**인증**: JWT + OAuth2 (카카오 소셜 로그인)

```
member-service/src/main/java/com/unicorn/lunchpick/member/
├── MemberApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── jwt/
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── CustomUserDetailsService.java
│   │   └── UserPrincipal.java
│   └── oauth2/
│       ├── KakaoOAuthConfig.java
│       ├── KakaoOAuthClient.java
│       └── KakaoProfile.java
├── controller/
│   ├── AuthController.java
│   ├── OnboardingController.java
│   ├── ProfileController.java
│   └── InternalMemberController.java
├── dto/
│   ├── request/
│   │   ├── KakaoLoginRequest.java
│   │   ├── OnboardingRequest.java
│   │   ├── CardSwipeResult.java
│   │   ├── OnboardingProgressRequest.java
│   │   ├── LocationConsentRequest.java
│   │   ├── DietaryRestrictionsRequest.java
│   │   └── UpdateProfileRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── OnboardingResponse.java
│       ├── OnboardingProgressSaveResponse.java
│       ├── LocationConsentResponse.java
│       ├── DietaryRestrictionsResponse.java
│       ├── MemberProfileResponse.java
│       ├── TasteProfileResponse.java
│       ├── SubscriptionStatusResponse.java
│       ├── NotificationSettingsDto.java
│       └── SubscriptionStatusDto.java
├── service/
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   ├── MemberService.java
│   ├── MemberServiceImpl.java
│   ├── OnboardingService.java
│   ├── OnboardingServiceImpl.java
│   ├── TasteVectorService.java
│   └── TokenService.java
├── domain/
│   ├── MemberProfile.java
│   ├── TasteProfile.java
│   ├── AuthResult.java
│   ├── MemberResult.java
│   ├── TasteVectorResult.java
│   ├── OnboardingResult.java
│   ├── ProgressSavedResult.java
│   ├── OnboardingProgress.java
│   └── LocationConsentResult.java
├── repository/
│   ├── entity/
│   │   ├── MemberEntity.java
│   │   ├── TasteProfileEntity.java
│   │   ├── DietaryRestrictionEntity.java
│   │   └── LocationConsentEntity.java
│   └── jpa/
│       ├── MemberRepository.java
│       ├── TasteProfileRepository.java
│       ├── DietaryRestrictionRepository.java
│       └── LocationConsentRepository.java
└── exception/
    └── MemberException.java
```

---

## 2. 추천·이력 서비스 (recommendation-service)

**패키지 루트**: `com.unicorn.lunchpick.recommendation`
**아키텍처**: Layered Architecture
**인증**: JWT

```
recommendation-service/src/main/java/com/unicorn/lunchpick/recommendation/
├── RecommendationApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── RedisConfig.java
│   ├── RestTemplateConfig.java
│   └── jwt/
│       ├── JwtAuthenticationFilter.java
│       ├── JwtTokenProvider.java
│       ├── CustomUserDetailsService.java
│       └── UserPrincipal.java
├── controller/
│   ├── RecommendationController.java
│   ├── MealController.java
│   ├── HistoryController.java
│   └── InsightController.java
├── dto/
│   ├── request/
│   │   ├── AcceptRecommendationRequest.java
│   │   ├── RejectRecommendationRequest.java
│   │   ├── RefreshRecommendationsRequest.java
│   │   ├── CreateMealRequest.java
│   │   ├── UpdateMealRequest.java
│   │   ├── FeedbackRequest.java
│   │   ├── AiRecommendationRequest.java
│   │   └── AiReasonRequest.java
│   └── response/
│       ├── TodayRecommendationsResponse.java
│       ├── RecommendationCard.java
│       ├── RecommendationReasonResponse.java
│       ├── AcceptRecommendationResponse.java
│       ├── RejectRecommendationResponse.java
│       ├── MealResponse.java
│       ├── FeedbackResponse.java
│       ├── MealHistoryResponse.java
│       ├── MealHistoryItem.java
│       ├── InsightsResponse.java
│       ├── AiRecommendationResponse.java
│       └── AiReasonResponse.java
├── service/
│   ├── RecommendationService.java
│   ├── RecommendationServiceImpl.java
│   ├── MealRecordService.java
│   ├── MealRecordServiceImpl.java
│   ├── FeedbackService.java
│   ├── FeedbackServiceImpl.java
│   ├── InsightService.java
│   ├── InsightServiceImpl.java
│   ├── TasteLearningService.java
│   ├── TasteLearningServiceImpl.java
│   ├── TasteVectorCalculator.java
│   ├── ContextCollector.java
│   └── CacheService.java
├── domain/
│   ├── RecommendationContext.java
│   ├── TasteProfileDto.java
│   ├── WeatherContext.java
│   ├── RecentMealHistory.java
│   ├── OnboardingSwipeData.java
│   ├── CategoryDistribution.java
│   ├── WeeklyPattern.java
│   ├── SatisfactionTrend.java
│   ├── MilestoneBadge.java
│   ├── AiMetadata.java
│   ├── TokenUsage.java
│   ├── RecommendedRestaurant.java
│   ├── WeightedFeedback.java
│   └── RestaurantInfo.java
├── repository/
│   ├── entity/
│   │   ├── RecommendationEntity.java
│   │   ├── MealRecordEntity.java
│   │   ├── FeedbackEntity.java
│   │   ├── PreferenceVectorEntity.java
│   │   └── LearningMessageEntity.java
│   └── jpa/
│       ├── RecommendationRepository.java
│       ├── MealRecordRepository.java
│       └── FeedbackRepository.java
├── client/
│   ├── AiPipelineClient.java
│   ├── MemberServiceClient.java
│   ├── WeatherApiClient.java
│   └── MapApiClient.java
└── exception/
    └── RecommendationException.java
```

---

## 3. 결제 서비스 (payment-service)

**패키지 루트**: `com.unicorn.lunchpick.payment`
**아키텍처**: Layered Architecture
**인증**: JWT

```
payment-service/src/main/java/com/unicorn/lunchpick/payment/
├── PaymentApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── RedisConfig.java
│   └── jwt/
│       ├── JwtAuthenticationFilter.java
│       ├── JwtTokenProvider.java
│       ├── CustomUserDetailsService.java
│       └── UserPrincipal.java
├── controller/
│   └── SubscriptionController.java
├── dto/
│   ├── request/
│   │   ├── CreateSubscriptionRequest.java
│   │   ├── CancelSubscriptionRequest.java
│   │   └── PaymentMethodDto.java
│   └── response/
│       ├── SubscriptionPlansResponse.java
│       ├── SubscriptionPlan.java
│       ├── CreateSubscriptionResponse.java
│       ├── CancelSubscriptionResponse.java
│       └── ExtendTrialResponse.java
├── service/
│   ├── SubscriptionService.java
│   ├── SubscriptionServiceImpl.java
│   ├── PaymentService.java
│   ├── PaymentServiceImpl.java
│   ├── PaymentValidator.java
│   └── SubscriptionEventPublisher.java
├── domain/
│   ├── PaymentResult.java
│   ├── PgPaymentResult.java
│   ├── CardInfo.java
│   ├── SubscriptionResult.java
│   ├── CancelResult.java
│   └── ExtendTrialResult.java
├── repository/
│   ├── entity/
│   │   ├── SubscriptionEntity.java
│   │   └── PaymentHistoryEntity.java
│   └── jpa/
│       ├── SubscriptionRepository.java
│       └── PaymentHistoryRepository.java
├── client/
│   └── PgGatewayClient.java
└── exception/
    └── PaymentException.java
```

---

## 4. AI Pipeline 서비스 (ai-pipeline-service)

**패키지 루트**: `ai_pipeline_service` (Python 모듈)
**아키텍처**: Python/FastAPI 고유 구조
**설계자**: 한승우 (마법사)

```
ai-pipeline-service/
├── main.py                              ← FastAPI 앱 진입점
├── requirements.txt
├── Dockerfile
├── router/
│   ├── __init__.py
│   ├── recommendation_router.py         ← POST /api/v1/ai/recommendations
│   └── reason_router.py                 ← POST /api/v1/ai/recommendation-reason
├── service/
│   ├── __init__.py
│   ├── recommendation_service.py        ← 추천 생성 비즈니스 로직
│   ├── reason_service.py                ← 추천 이유 생성 비즈니스 로직
│   └── fallback_engine.py               ← 규칙 기반 폴백 추천
├── prompt/
│   ├── __init__.py
│   ├── recommendation_prompt.py         ← 추천 프롬프트 빌더 (콜드스타트 포함)
│   └── reason_prompt.py                 ← 이유 생성 프롬프트 빌더
├── llm/
│   ├── __init__.py
│   ├── llm_client.py                    ← init_chat_model 추상화, Circuit Breaker, Retry
│   └── circuit_breaker.py               ← Circuit Breaker 상태 관리
├── parser/
│   ├── __init__.py
│   ├── recommendation_parser.py         ← 추천 응답 파싱 및 스키마 검증
│   └── reason_parser.py                 ← 이유 응답 파싱
├── cache/
│   ├── __init__.py
│   └── cache_manager.py                 ← Redis Cache-Aside, TTL 관리
├── model/
│   ├── __init__.py
│   ├── recommendation_request.py        ← AiRecommendationRequest (Pydantic)
│   ├── recommendation_response.py       ← AiRecommendationResponse (Pydantic)
│   ├── reason_request.py                ← AiReasonRequest (Pydantic)
│   ├── reason_response.py               ← AiReasonResponse (Pydantic)
│   ├── weather_context.py               ← WeatherContext (Pydantic)
│   ├── ai_metadata.py                   ← AiMetadata, TokenUsage (Pydantic)
│   └── common.py                        ← RecommendedRestaurant, ParsedReason (Pydantic)
└── tests/
    ├── __init__.py
    ├── test_recommendation_service.py
    ├── test_reason_service.py
    ├── test_llm_client.py
    ├── test_circuit_breaker.py
    └── test_cache_manager.py
```

---

## 5. 공통 모듈 (common)

**패키지 루트**: `com.unicorn.lunchpick.common`

```
common/src/main/java/com/unicorn/lunchpick/common/
├── dto/
│   ├── ApiResponse.java                 ← 공통 응답 래퍼
│   └── ErrorResponse.java               ← 공통 에러 응답
├── entity/
│   └── BaseTimeEntity.java              ← createdAt, updatedAt
├── config/
│   └── JpaConfig.java
├── util/
│   ├── JwtTokenProvider.java            ← JWT 생성/검증 유틸
│   └── DateTimeUtil.java                ← KST 시각, 점심 시간대 판별
└── exception/
    ├── BusinessException.java           ← 비즈니스 예외 기반 클래스
    ├── NotFoundException.java
    ├── ValidationException.java
    ├── ConflictException.java
    └── UnauthorizedException.java
```

---

## 패키지 명명 규칙 요약

| 계층 | Java 패키지 | Python 모듈 |
|------|------------|-------------|
| 진입점 | `{Service}Application.java` | `main.py` |
| 라우터/컨트롤러 | `controller/` | `router/` |
| 서비스 | `service/` | `service/` |
| 도메인 모델 | `domain/` | `model/` (Pydantic) |
| JPA 엔티티 | `repository/entity/` | — |
| 저장소 | `repository/jpa/` | — |
| 외부 클라이언트 | `client/` | `llm/`, `cache/` |
| 설정 | `config/` | — |
| 예외 | `exception/` | — |
| DTO 요청 | `dto/request/` | `model/*_request.py` |
| DTO 응답 | `dto/response/` | `model/*_response.py` |
