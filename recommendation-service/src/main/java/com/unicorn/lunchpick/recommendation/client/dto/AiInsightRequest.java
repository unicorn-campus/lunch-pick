package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * AI Pipeline 인사이트 분석 요청 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/insights} 엔드포인트로 전송하는 요청 본문입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Builder
public record AiInsightRequest(

        @JsonProperty("memberId")
        String memberId,

        @JsonProperty("recentMeals")
        List<MealData> recentMeals,

        @JsonProperty("categoryDistribution")
        Map<String, Double> categoryDistribution,

        @JsonProperty("totalMealCount")
        int totalMealCount,

        @JsonProperty("periodDays")
        int periodDays
) {

    @Builder
    public record MealData(
            @JsonProperty("date") String date,
            @JsonProperty("restaurantName") String restaurantName,
            @JsonProperty("menuName") String menuName,
            @JsonProperty("category") String category,
            @JsonProperty("satisfaction") String satisfaction,
            @JsonProperty("keyword") String keyword
    ) {}
}
