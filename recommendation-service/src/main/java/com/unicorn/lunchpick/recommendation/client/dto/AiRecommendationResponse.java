package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI Pipeline 추천 생성 응답 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/recommendations} 응답 본문입니다.
 * Python Pydantic 모델 {@code AiRecommendationResponse}와 필드명을 맞춥니다.</p>
 *
 * <p><b>Circuit Breaker Open 또는 폴백 시:</b> {@code isFallback=true},
 * {@code metadata.source}가 {@code STALE_CACHE} 또는 {@code FALLBACK_RULE_BASED}로 반환됩니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiRecommendationRequest
 */
public record AiRecommendationResponse(

        /** 추천 식당 목록 (1~3개) */
        @JsonProperty("recommendations")
        List<RecommendedRestaurant> recommendations,

        /** 폴백 여부 (LLM 장애 시 true) */
        @JsonProperty("isFallback")
        boolean isFallback,

        /** 콜드스타트 여부 */
        @JsonProperty("isColdStart")
        boolean isColdStart,

        /** 콜드스타트 태그 */
        @JsonProperty("coldStartTag")
        String coldStartTag,

        /** 캐시 키 */
        @JsonProperty("cacheKey")
        String cacheKey,

        /** 캐시 만료 시각 (ISO-8601) */
        @JsonProperty("cachedUntil")
        String cachedUntil,

        /** AI 메타데이터 */
        @JsonProperty("metadata")
        AiMetadata metadata
) {

    /**
     * 추천 식당 항목
     */
    public record RecommendedRestaurant(
            @JsonProperty("restaurantId") String restaurantId,
            @JsonProperty("restaurantName") String restaurantName,
            @JsonProperty("representativeMenu") String representativeMenu,
            @JsonProperty("category") String category,
            @JsonProperty("reasonSummary") String reasonSummary,
            @JsonProperty("confidenceScore") int confidenceScore,
            @JsonProperty("distanceMeters") Integer distanceMeters,
            @JsonProperty("estimatedWalkMinutes") Integer estimatedWalkMinutes
    ) {}

    /**
     * AI 메타데이터
     */
    public record AiMetadata(
            @JsonProperty("source") String source,
            @JsonProperty("modelUsed") String modelUsed,
            @JsonProperty("latencyMs") int latencyMs,
            @JsonProperty("circuitBreakerState") String circuitBreakerState
    ) {}
}
