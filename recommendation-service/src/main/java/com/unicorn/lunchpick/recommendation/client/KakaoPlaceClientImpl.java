package com.unicorn.lunchpick.recommendation.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.recommendation.client.dto.NearbyRestaurant;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 카카오맵 Place API 클라이언트 구현체
 *
 * <p>WebClient로 카카오맵 키워드 장소 검색 API를 호출하고,
 * Resilience4j Circuit Breaker와 Redis 캐시로 보호합니다.</p>
 *
 * <p><b>캐시 전략:</b> {@code kakao:place:{latGrid}:{lonGrid}} 키, TTL 1시간, memberId 없이 공유 캐시</p>
 * <p><b>Circuit Breaker:</b> {@code kakao-map} 인스턴스, connect 2s, read 3s</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Slf4j
@Component
public class KakaoPlaceClientImpl implements KakaoPlaceClient {

    private static final int LOCATION_GRID_SIZE = 100;
    private static final Duration PLACE_CACHE_TTL = Duration.ofHours(1);
    private static final String PLACE_CACHE_PREFIX = "kakao:place:";
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    @Value("${external.map.endpoint:/v2/local/search/keyword.json}")
    private String searchEndpoint;

    private final WebClient kakaoMapWebClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public KakaoPlaceClientImpl(@Qualifier("kakaoMapWebClient") WebClient kakaoMapWebClient,
                                 StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper) {
        this.kakaoMapWebClient = kakaoMapWebClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "kakao-map", fallbackMethod = "searchNearbyRestaurantsFallback")
    public List<NearbyRestaurant> searchNearbyRestaurants(double latitude, double longitude, int radiusMeters) {
        // 1. Redis 캐시 조회
        String cacheKey = buildPlaceCacheKey(latitude, longitude);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("[KakaoMap] 캐시 히트 — key: {}", cacheKey);
            List<NearbyRestaurant> cachedResult = deserializeCachedRestaurants(cached);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        // 2. 카카오맵 API 호출
        log.info("[KakaoMap] API 호출 — lat: {}, lon: {}, radius: {}m", latitude, longitude, radiusMeters);
        String responseBody = kakaoMapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(searchEndpoint)
                        .queryParam("query", "맛집")
                        .queryParam("x", String.valueOf(longitude))
                        .queryParam("y", String.valueOf(latitude))
                        .queryParam("radius", radiusMeters)
                        .queryParam("category_group_code", "FD6")
                        .queryParam("size", 15)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block(BLOCK_TIMEOUT);

        if (responseBody == null) {
            log.warn("[KakaoMap] 빈 응답");
            return List.of();
        }

        // 3. 응답 파싱
        List<NearbyRestaurant> restaurants = parseKakaoResponse(responseBody);
        log.info("[KakaoMap] 식당 {}개 조회 완료", restaurants.size());

        // 4. Redis 캐시 저장
        cacheRestaurants(cacheKey, restaurants);

        return restaurants;
    }

    /**
     * Circuit Breaker fallback — 빈 리스트 반환
     */
    public List<NearbyRestaurant> searchNearbyRestaurantsFallback(double latitude, double longitude,
                                                                    int radiusMeters, Throwable t) {
        log.warn("[KakaoMap Fallback] 식당 검색 실패 — cause: {}", t.getMessage());
        return List.of();
    }

    private List<NearbyRestaurant> parseKakaoResponse(String responseBody) {
        List<NearbyRestaurant> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode documents = root.get("documents");
            if (documents == null || !documents.isArray()) {
                return result;
            }

            for (JsonNode doc : documents) {
                String categoryName = doc.has("category_name") ? doc.get("category_name").asText("") : "";
                String category = extractMainCategory(categoryName);
                String subMenu = extractSubCategory(categoryName);
                int distanceMeters = doc.has("distance") ? parseDistance(doc.get("distance").asText("0")) : 0;

                NearbyRestaurant restaurant = NearbyRestaurant.builder()
                        .restaurantId(doc.has("id") ? doc.get("id").asText() : "")
                        .restaurantName(doc.has("place_name") ? doc.get("place_name").asText() : "")
                        .representativeMenu(subMenu.isEmpty() ? category : subMenu)
                        .category(category)
                        .distanceMeters(distanceMeters)
                        .estimatedWalkMinutes(Math.max(1, distanceMeters / 80))
                        .address(doc.has("road_address_name") ? doc.get("road_address_name").asText("") : "")
                        .phone(doc.has("phone") ? doc.get("phone").asText("") : "")
                        .longitude(doc.has("x") ? doc.get("x").asDouble(0) : 0)
                        .latitude(doc.has("y") ? doc.get("y").asDouble(0) : 0)
                        .build();
                result.add(restaurant);
            }
        } catch (JsonProcessingException e) {
            log.warn("[KakaoMap] 응답 파싱 실패", e);
        }
        return result;
    }

    private String extractMainCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return "기타";
        // "음식점 > 한식 > 국밥" → "한식"
        String[] parts = categoryName.split(" > ");
        return parts.length >= 2 ? parts[1].trim() : parts[0].trim();
    }

    private String extractSubCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return "";
        // "음식점 > 한식 > 국밥" → "국밥"
        String[] parts = categoryName.split(" > ");
        return parts.length >= 3 ? parts[2].trim() : "";
    }

    private int parseDistance(String distance) {
        try {
            return Integer.parseInt(distance);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String buildPlaceCacheKey(double latitude, double longitude) {
        int latGrid = (int) (latitude * LOCATION_GRID_SIZE);
        int lonGrid = (int) (longitude * LOCATION_GRID_SIZE);
        return PLACE_CACHE_PREFIX + latGrid + ":" + lonGrid;
    }

    private void cacheRestaurants(String cacheKey, List<NearbyRestaurant> restaurants) {
        try {
            String json = objectMapper.writeValueAsString(restaurants);
            stringRedisTemplate.opsForValue().set(cacheKey, json, PLACE_CACHE_TTL);
            log.debug("[KakaoMap] 캐시 저장 완료 — key: {}, count: {}", cacheKey, restaurants.size());
        } catch (JsonProcessingException e) {
            log.warn("[KakaoMap] 캐시 직렬화 실패", e);
        }
    }

    private List<NearbyRestaurant> deserializeCachedRestaurants(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NearbyRestaurant.class));
        } catch (JsonProcessingException e) {
            log.warn("[KakaoMap] 캐시 역직렬화 실패", e);
            return null;
        }
    }
}
