package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * AI Pipeline 추천 이유 생성 요청 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/recommendation-reason} 엔드포인트로 전송하는 요청 본문입니다.
 * Python Pydantic 모델 {@code AiReasonRequest}와 필드명을 맞춥니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiReasonResponse
 */
@Builder
public record AiReasonRequest(

        /** 추천 식별자 */
        @JsonProperty("recommendationId")
        String recommendationId,

        /** 식당 식별자 */
        @JsonProperty("restaurantId")
        String restaurantId,

        /** 식당명 */
        @JsonProperty("restaurantName")
        String restaurantName,

        /** 음식 카테고리 */
        @JsonProperty("category")
        String category,

        /** 회원 식별자 */
        @JsonProperty("memberId")
        String memberId,

        /** 취향 벡터 (카테고리 → 선호도 0.0~1.0) */
        @JsonProperty("tasteVector")
        Map<String, Double> tasteVector,

        /** 날씨 컨텍스트 */
        @JsonProperty("weather")
        AiRecommendationRequest.WeatherContext weather,

        /** 최근 식사 이력 */
        @JsonProperty("recentMealHistory")
        List<AiRecommendationRequest.RecentMealHistoryItem> recentMealHistory,

        /** 확신 스코어 (0~100) */
        @JsonProperty("confidenceScore")
        Integer confidenceScore,

        /** 대표 메뉴명 */
        @JsonProperty("representativeMenu")
        String representativeMenu
) {}
