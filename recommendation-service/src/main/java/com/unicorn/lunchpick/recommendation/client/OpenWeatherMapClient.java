package com.unicorn.lunchpick.recommendation.client;

import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * OpenWeatherMap API 클라이언트 구현체
 *
 * <p>현재 좌표 기반 날씨를 조회하여 AI Pipeline에 전달할 WeatherContext를 생성합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Slf4j
@Component
public class OpenWeatherMapClient implements WeatherClient {

    private final WebClient webClient;
    private final String apiKey;

    public OpenWeatherMapClient(
            @Qualifier("weatherWebClient") WebClient webClient,
            @Value("${external.weather.key:}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    @Override
    public AiRecommendationRequest.WeatherContext getCurrentWeather(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("WEATHER_API_KEY 미설정 — 날씨 조회 스킵");
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/2.5/weather")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "kr")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("OpenWeatherMap 응답 null");
                return null;
            }

            return parseResponse(response);
        } catch (Exception ex) {
            log.warn("OpenWeatherMap API 호출 실패 — 날씨 없이 추천 진행: {}", ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private AiRecommendationRequest.WeatherContext parseResponse(Map<String, Object> response) {
        // main.temp
        Map<String, Object> main = (Map<String, Object>) response.get("main");
        double temp = main != null ? ((Number) main.get("temp")).doubleValue() : 20.0;

        // weather[0].main, weather[0].description
        List<Map<String, Object>> weatherList = (List<Map<String, Object>>) response.get("weather");
        String weatherMain = "Clear";
        String description = "맑음";
        if (weatherList != null && !weatherList.isEmpty()) {
            Map<String, Object> w = weatherList.get(0);
            weatherMain = (String) w.getOrDefault("main", "Clear");
            description = (String) w.getOrDefault("description", "맑음");
        }

        // wind.speed
        Map<String, Object> wind = (Map<String, Object>) response.get("wind");
        double windSpeed = wind != null ? ((Number) wind.getOrDefault("speed", 0)).doubleValue() : 0;

        String condition = mapToCondition(weatherMain, temp, windSpeed);

        log.info("날씨 조회 완료 — {}°C, {}, {} (condition={})", temp, weatherMain, description, condition);

        return AiRecommendationRequest.WeatherContext.builder()
                .condition(condition)
                .description(description)
                .temperatureCelsius(temp)
                .build();
    }

    /**
     * OpenWeatherMap 날씨 코드를 AI Pipeline의 WeatherCondition enum으로 매핑
     */
    private String mapToCondition(String weatherMain, double temp, double windSpeed) {
        // 극단 온도 우선
        if (temp >= 33) return "HOT";
        if (temp <= -5) return "COLD";

        // 강풍
        if (windSpeed >= 10) return "WINDY";

        // 날씨 상태
        return switch (weatherMain) {
            case "Rain", "Drizzle", "Thunderstorm" -> "RAINY";
            case "Snow" -> "SNOWY";
            case "Clouds", "Mist", "Fog", "Haze" -> "CLOUDY";
            default -> "CLEAR";
        };
    }
}
