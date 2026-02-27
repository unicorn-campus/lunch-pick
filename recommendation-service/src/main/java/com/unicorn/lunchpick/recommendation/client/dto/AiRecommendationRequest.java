package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI Pipeline 추천 생성 요청 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/recommendations} 엔드포인트로 전송하는 요청 본문입니다.
 * Python Pydantic 모델 {@code AiRecommendationRequest}와 필드명을 맞춥니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiRecommendationResponse
 */
@Builder
public record AiRecommendationRequest(

        /** 회원 식별자 */
        @JsonProperty("memberId")
        String memberId,

        /** 위도 */
        @JsonProperty("latitude")
        double latitude,

        /** 경도 */
        @JsonProperty("longitude")
        double longitude,

        /** 요청 시각 (ISO-8601) */
        @JsonProperty("requestedAt")
        Instant requestedAt,

        /** 콜드스타트 여부 (피드백 5건 미만) */
        @JsonProperty("isColdStart")
        boolean isColdStart,

        /** 피드백 건수 */
        @JsonProperty("feedbackCount")
        Integer feedbackCount,

        /** 취향 벡터 (카테고리 → 선호도 0.0~1.0) */
        @JsonProperty("tasteVector")
        Map<String, Double> tasteVector,

        /** 온보딩 스와이프 데이터 (콜드스타트 전용) */
        @JsonProperty("onboardingSwipes")
        List<OnboardingSwipeData> onboardingSwipes,

        /** 알레르기 필터 목록 */
        @JsonProperty("allergenFilter")
        List<String> allergenFilter,

        /** 식단 유형 */
        @JsonProperty("dietType")
        String dietType,

        /** 날씨 컨텍스트 */
        @JsonProperty("weather")
        WeatherContext weather,

        /** 최근 식사 이력 */
        @JsonProperty("recentMealHistory")
        List<RecentMealHistoryItem> recentMealHistory,

        /** 제외할 식당 ID 목록 (최근 3일 방문) */
        @JsonProperty("excludeRestaurantIds")
        List<String> excludeRestaurantIds,

        /** 직군 클러스터 (콜드스타트 전용) */
        @JsonProperty("jobCluster")
        String jobCluster,

        /** 주변 이용 가능한 식당 목록 (카카오맵 API 조회 결과, nullable) */
        @JsonProperty("availableRestaurants")
        List<AvailableRestaurantData> availableRestaurants,

        /** AI Pipeline 캐시 스킵 여부 (새로고침 시 true) */
        @JsonProperty("skipCache")
        boolean skipCache
) {

    /**
     * 온보딩 스와이프 데이터
     */
    @Builder
    public record OnboardingSwipeData(
            @JsonProperty("cardId") String cardId,
            @JsonProperty("category") String category,
            @JsonProperty("liked") boolean liked
    ) {}

    /**
     * 날씨 컨텍스트
     */
    @Builder
    public record WeatherContext(
            @JsonProperty("condition") String condition,
            @JsonProperty("description") String description,
            @JsonProperty("temperatureCelsius") double temperatureCelsius
    ) {}

    /**
     * 최근 식사 이력 항목
     */
    @Builder
    public record RecentMealHistoryItem(
            @JsonProperty("restaurantId") String restaurantId,
            @JsonProperty("category") String category,
            @JsonProperty("mealDate") String mealDate,
            @JsonProperty("satisfaction") String satisfaction
    ) {}

    /**
     * 주변 이용 가능한 식당 데이터 (카카오맵 API 조회 결과)
     */
    @Builder
    public record AvailableRestaurantData(
            @JsonProperty("restaurantId") String restaurantId,
            @JsonProperty("restaurantName") String restaurantName,
            @JsonProperty("representativeMenu") String representativeMenu,
            @JsonProperty("category") String category,
            @JsonProperty("distanceMeters") int distanceMeters,
            @JsonProperty("estimatedWalkMinutes") int estimatedWalkMinutes,
            @JsonProperty("allergens") List<String> allergens
    ) {}
}
