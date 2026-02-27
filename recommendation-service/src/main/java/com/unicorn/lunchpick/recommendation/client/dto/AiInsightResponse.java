package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI Pipeline 인사이트 분석 응답 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/insights} 엔드포인트의 응답 본문입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
public record AiInsightResponse(

        @JsonProperty("weeklySummary")
        String weeklySummary,

        @JsonProperty("mealBalance")
        MealBalance mealBalance,

        @JsonProperty("satisfactionAnalysis")
        SatisfactionAnalysis satisfactionAnalysis,

        @JsonProperty("metadata")
        AiMetadata metadata
) {

    public record MealBalance(
            @JsonProperty("diversityScore") int diversityScore,
            @JsonProperty("diagnosis") String diagnosis,
            @JsonProperty("coachingComment") String coachingComment
    ) {}

    public record SatisfactionAnalysis(
            @JsonProperty("satisfactionRate") int satisfactionRate,
            @JsonProperty("patterns") List<String> patterns,
            @JsonProperty("patternComment") String patternComment
    ) {}

    public record AiMetadata(
            @JsonProperty("source") String source,
            @JsonProperty("modelUsed") String modelUsed,
            @JsonProperty("latencyMs") int latencyMs
    ) {}
}
