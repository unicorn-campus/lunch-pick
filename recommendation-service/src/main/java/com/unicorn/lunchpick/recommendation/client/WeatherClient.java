package com.unicorn.lunchpick.recommendation.client;

import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;

/**
 * 날씨 API 클라이언트 인터페이스
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
public interface WeatherClient {

    /**
     * 현재 날씨 조회
     *
     * @param latitude  위도
     * @param longitude 경도
     * @return WeatherContext (실패 시 null)
     */
    AiRecommendationRequest.WeatherContext getCurrentWeather(double latitude, double longitude);
}
