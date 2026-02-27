package com.unicorn.lunchpick.recommendation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.recommendation.client.AiPipelineClient;
import com.unicorn.lunchpick.recommendation.client.KakaoPlaceClient;
import com.unicorn.lunchpick.recommendation.client.WeatherClient;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationResponse;
import com.unicorn.lunchpick.recommendation.client.dto.NearbyRestaurant;
import com.unicorn.lunchpick.recommendation.dto.request.AcceptRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RefreshRecommendationsRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RejectRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.response.AcceptRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RecommendationCardDto;
import com.unicorn.lunchpick.recommendation.dto.response.RecommendationReasonResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RejectRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.TodayRecommendationsResponse;
import com.unicorn.lunchpick.recommendation.exception.RecommendationException;
import com.unicorn.lunchpick.recommendation.repository.entity.RecommendationEntity;
import com.unicorn.lunchpick.recommendation.repository.jpa.RecommendationRepository;
import com.unicorn.lunchpick.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 추천 서비스 구현체
 *
 * <p>AI Pipeline 호출(Resilience4j CB), Redis 캐시, 폴백 추천을 구현합니다.</p>
 *
 * <p><b>추천 조회 흐름:</b></p>
 * <ol>
 *   <li>Redis 캐시 조회 — 히트 시 즉시 반환 (200ms 미만)</li>
 *   <li>캐시 미스 → {@link AiPipelineClient#getRecommendations(AiRecommendationRequest)} 호출</li>
 *   <li>AI 응답이 빈 목록(CB Fallback) → 규칙 기반 폴백 추천 생성</li>
 *   <li>추천 결과 DB 저장 + Redis 캐시 저장</li>
 * </ol>
 *
 * <p><b>Redis 캐시 키 패턴:</b></p>
 * <ul>
 *   <li>{@code rec:{memberId}:{latGrid}:{lonGrid}} — 추천 결과 캐시 (TTL 1시간)</li>
 *   <li>{@code rec:reason:{recommendationId}} — 추천 이유 캐시 (TTL 1시간)</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiPipelineClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String REC_CACHE_PREFIX = "rec:";
    private static final String REASON_CACHE_PREFIX = "rec:reason:";
    private static final Duration REC_CACHE_TTL = Duration.ofHours(1);
    private static final Duration REASON_CACHE_TTL = Duration.ofHours(1);
    private static final int LOCATION_GRID_SIZE = 100;
    private static final int FALLBACK_CONFIDENCE = 60;
    private static final int FALLBACK_DISTANCE_METERS = 300;
    private static final int FALLBACK_WALK_MINUTES = 4;

    private final RecommendationRepository recommendationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AiPipelineClient aiPipelineClient;
    private final KakaoPlaceClient kakaoPlaceClient;
    private final WeatherClient weatherClient;

    /**
     * {@inheritDoc}
     *
     * <p>캐시 키: {@code rec:{memberId}:{latGrid}:{lonGrid}}.
     * 캐시 미스 시 AI Pipeline 호출(CB 보호), 빈 응답 시 규칙 기반 폴백 추천 반환.</p>
     *
     * @param memberId  회원 식별자
     * @param latitude  위도
     * @param longitude 경도
     * @return 오늘의 추천 응답
     */
    @Override
    @Transactional
    public TodayRecommendationsResponse getTodayRecommendations(String memberId,
                                                                double latitude, double longitude) {
        String cacheKey = buildRecommendationCacheKey(memberId, latitude, longitude);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.debug("추천 캐시 히트 — memberId: {}", memberId);
            TodayRecommendationsResponse cachedResponse = deserializeRecommendationsResponse(cached);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        log.info("추천 캐시 미스 — AI Pipeline 호출 시작 — memberId: {}", memberId);
        return callAiPipelineAndBuild(memberId, latitude, longitude, cacheKey, false, List.of());
    }

    /**
     * {@inheritDoc}
     *
     * <p>추천 이유 캐시({@code rec:reason:{recommendationId}}) 조회 후 없으면
     * AI Pipeline 이유 생성 API를 호출합니다.
     * LLM 실패 시에도 AI Pipeline은 200을 반환하므로 CB는 네트워크/서버 오류에만 동작합니다.</p>
     *
     * @param memberId         회원 식별자
     * @param recommendationId 추천 ID
     * @return 추천 이유 응답
     */
    @Override
    @Transactional(readOnly = true)
    public RecommendationReasonResponse getRecommendationReason(String memberId, String recommendationId) {
        RecommendationEntity recommendation = recommendationRepository
                .findByRecommendationId(recommendationId)
                .orElseThrow(RecommendationException::recommendationNotFound);

        String reasonCacheKey = REASON_CACHE_PREFIX + recommendationId;
        String cachedReason = stringRedisTemplate.opsForValue().get(reasonCacheKey);

        if (cachedReason != null) {
            log.debug("추천 이유 캐시 히트 — recommendationId: {}", recommendationId);
            return buildReasonResponseFromCache(recommendationId, cachedReason, recommendation);
        }

        log.info("추천 이유 캐시 미스 — AI Pipeline 호출 — recommendationId: {}", recommendationId);
        return callAiPipelineForReason(memberId, recommendationId, recommendation, reasonCacheKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AcceptRecommendationResponse acceptRecommendation(String memberId, String recommendationId,
                                                              AcceptRecommendationRequest request) {
        RecommendationEntity recommendation = recommendationRepository
                .findByRecommendationId(recommendationId)
                .orElseThrow(RecommendationException::recommendationNotFound);

        recommendation.accept(request.reactionTimeMs());

        String acceptanceId = UUID.randomUUID().toString();
        log.info("추천 수락 완료 — memberId: {}, recommendationId: {}, reactionTimeMs: {}",
                memberId, recommendationId, request.reactionTimeMs());

        return AcceptRecommendationResponse.builder()
                .acceptanceId(acceptanceId)
                .restaurantId(recommendation.getRestaurantId())
                .restaurantName(recommendation.getRestaurantName())
                .restaurantAddress("")
                .message("수락이 완료되었어요. 맛있는 점심 되세요!")
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>거절 처리 후 Redis 캐시에서 위치 정보를 복원하여 AI Pipeline에
     * 대체 추천을 요청합니다. 이미 추천된 식당은 {@code excludeRestaurantIds}로 제외합니다.</p>
     *
     * <p>대체 추천 실패 시(캐시 없음, AI 응답 없음, 예외) 기존 동작({@code hasAlternative=false})을 유지합니다.</p>
     */
    @Override
    @Transactional
    public RejectRecommendationResponse rejectRecommendation(String memberId, String recommendationId,
                                                              RejectRecommendationRequest request) {
        RecommendationEntity recommendation = recommendationRepository
                .findByRecommendationId(recommendationId)
                .orElseThrow(RecommendationException::recommendationNotFound);

        recommendation.reject(request.rejectReason());

        log.info("추천 거절 완료 — memberId: {}, reason: {}", memberId, request.rejectReason());

        // 대체 추천 시도
        try {
            return tryAlternativeRecommendation(memberId, recommendation);
        } catch (Exception e) {
            log.warn("대체 추천 처리 중 예외 발생 — 기본 응답 반환 — memberId: {}", memberId, e);
            return buildNoAlternativeResponse();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TodayRecommendationsResponse refreshRecommendations(String memberId,
                                                                RefreshRecommendationsRequest request) {
        String cacheKey = buildRecommendationCacheKey(memberId, request.latitude(), request.longitude());
        stringRedisTemplate.delete(cacheKey);
        log.info("추천 캐시 무효화 — memberId: {}", memberId);

        return callAiPipelineAndBuild(memberId, request.latitude(), request.longitude(), cacheKey, true, request.rejectedIds());
    }

    // -------------------------------------------------------------------------
    // private 헬퍼 메서드
    // -------------------------------------------------------------------------

    /**
     * 대체 추천을 시도하여 RejectRecommendationResponse를 반환
     *
     * <p>Redis 캐시에서 멤버의 추천 캐시 키를 찾아 위치 정보를 복원하고,
     * 이미 추천된 식당을 제외한 뒤 AI Pipeline에 대체 추천을 요청합니다.
     * 성공 시 기존 캐시의 거절 카드를 대체 카드로 교체합니다.</p>
     *
     * @param memberId 회원 식별자
     * @param rejected 거절된 추천 엔티티
     * @return 대체 추천 포함 응답 또는 대체 불가 응답
     */
    private RejectRecommendationResponse tryAlternativeRecommendation(String memberId,
                                                                        RecommendationEntity rejected) {
        // 1. Redis에서 멤버의 추천 캐시 키 검색
        Set<String> keys = stringRedisTemplate.keys(REC_CACHE_PREFIX + memberId + ":*");
        if (keys == null || keys.isEmpty()) {
            log.info("추천 캐시 없음 — 대체 추천 불가 — memberId: {}", memberId);
            return buildNoAlternativeResponse();
        }

        String cacheKey = keys.iterator().next();

        // 2. 캐시 키에서 위치 정보 파싱
        double[] location = parseLocationFromCacheKey(cacheKey);
        if (location == null) {
            log.warn("캐시 키에서 위치 정보 파싱 실패 — 대체 추천 불가 — cacheKey: {}", cacheKey);
            return buildNoAlternativeResponse();
        }
        double latitude = location[0];
        double longitude = location[1];

        // 3. 현재 캐시에서 추천된 모든 식당 ID 수집 (제외 목적)
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        TodayRecommendationsResponse cachedResponse = (cached != null)
                ? deserializeRecommendationsResponse(cached) : null;

        List<String> excludeIds = new ArrayList<>();
        if (cachedResponse != null && cachedResponse.recommendations() != null) {
            cachedResponse.recommendations()
                    .forEach(card -> excludeIds.add(card.restaurantId()));
        }
        // 거절된 식당도 제외 목록에 포함 (중복 안전)
        if (!excludeIds.contains(rejected.getRestaurantId())) {
            excludeIds.add(rejected.getRestaurantId());
        }

        // 4. AI Pipeline 호출하여 대체 추천 획득
        RecommendationCardDto alternativeCard = fetchSingleAlternative(
                memberId, latitude, longitude, excludeIds);

        if (alternativeCard == null) {
            log.info("AI Pipeline 대체 추천 결과 없음 — memberId: {}", memberId);
            return buildNoAlternativeResponse();
        }

        // 5. 기존 캐시 갱신 — 거절된 카드를 대체 카드로 교체
        if (cachedResponse != null && cachedResponse.recommendations() != null) {
            List<RecommendationCardDto> updatedCards = new ArrayList<>(cachedResponse.recommendations());
            updatedCards.removeIf(card -> card.restaurantId().equals(rejected.getRestaurantId()));
            updatedCards.add(alternativeCard);

            TodayRecommendationsResponse updatedResponse = TodayRecommendationsResponse.builder()
                    .recommendations(updatedCards)
                    .isColdStart(cachedResponse.isColdStart())
                    .coldStartMessage(cachedResponse.coldStartMessage())
                    .isFallback(cachedResponse.isFallback())
                    .fallbackMessage(cachedResponse.fallbackMessage())
                    .generatedAt(cachedResponse.generatedAt())
                    .build();

            cacheRecommendationsResponse(cacheKey, updatedResponse);
            log.info("추천 캐시 갱신 완료 — 거절 식당({}) 대체({}) — memberId: {}",
                    rejected.getRestaurantId(), alternativeCard.restaurantId(), memberId);
        }

        return RejectRecommendationResponse.builder()
                .rejected(true)
                .hasAlternative(true)
                .alternativeRecommendation(alternativeCard)
                .message("새로운 추천을 찾았어요!")
                .noAlternativeMessage(null)
                .build();
    }

    /**
     * AI Pipeline에 요청하여 대체 추천 카드 1개를 반환
     *
     * <p>카카오맵 주변 식당 조회 + 날씨 정보를 포함하여 AI Pipeline에 요청하고,
     * 응답의 첫 번째 추천 항목을 DB에 저장 후 카드 DTO로 반환합니다.</p>
     *
     * @param memberId             회원 식별자
     * @param latitude             위도
     * @param longitude            경도
     * @param excludeRestaurantIds 제외할 식당 ID 목록
     * @return 대체 추천 카드 DTO, 결과가 없으면 null
     */
    private RecommendationCardDto fetchSingleAlternative(String memberId,
                                                           double latitude, double longitude,
                                                           List<String> excludeRestaurantIds) {
        // 카카오맵 주변 식당 조회
        List<NearbyRestaurant> nearbyRestaurants = kakaoPlaceClient.searchNearbyRestaurants(
                latitude, longitude, 500);

        List<AiRecommendationRequest.AvailableRestaurantData> availableRestaurants = null;
        if (!nearbyRestaurants.isEmpty()) {
            availableRestaurants = nearbyRestaurants.stream()
                    .map(r -> AiRecommendationRequest.AvailableRestaurantData.builder()
                            .restaurantId(r.restaurantId())
                            .restaurantName(r.restaurantName())
                            .representativeMenu(r.representativeMenu())
                            .category(r.category())
                            .distanceMeters(r.distanceMeters())
                            .estimatedWalkMinutes(r.estimatedWalkMinutes())
                            .allergens(List.of())
                            .build())
                    .toList();
        }

        // 날씨 조회
        AiRecommendationRequest.WeatherContext weather = weatherClient.getCurrentWeather(latitude, longitude);

        AiRecommendationRequest aiRequest = AiRecommendationRequest.builder()
                .memberId(memberId)
                .latitude(latitude)
                .longitude(longitude)
                .requestedAt(Instant.now())
                .isColdStart(false)
                .weather(weather)
                .allergenFilter(List.of())
                .recentMealHistory(List.of())
                .excludeRestaurantIds(excludeRestaurantIds)
                .availableRestaurants(availableRestaurants)
                .skipCache(true)
                .build();

        AiRecommendationResponse aiResponse = aiPipelineClient.getRecommendations(aiRequest);

        if (aiResponse == null || aiResponse.recommendations() == null || aiResponse.recommendations().isEmpty()) {
            return null;
        }

        // 첫 번째 추천만 사용
        AiRecommendationResponse.RecommendedRestaurant restaurant = aiResponse.recommendations().get(0);
        String recId = UUID.randomUUID().toString();
        saveRecommendationEntity(recId, memberId, restaurant, aiResponse.isFallback());

        return RecommendationCardDto.builder()
                .recommendationId(recId)
                .restaurantId(restaurant.restaurantId())
                .restaurantName(restaurant.restaurantName())
                .representativeMenu(restaurant.representativeMenu())
                .reasonSummary(restaurant.reasonSummary())
                .confidenceScore(restaurant.confidenceScore())
                .distanceMeters(Optional.ofNullable(restaurant.distanceMeters()).orElse(FALLBACK_DISTANCE_METERS))
                .estimatedWalkMinutes(Optional.ofNullable(restaurant.estimatedWalkMinutes()).orElse(FALLBACK_WALK_MINUTES))
                .category(restaurant.category())
                .isFallback(aiResponse.isFallback())
                .build();
    }

    /**
     * Redis 캐시 키에서 위도·경도를 복원
     *
     * <p>캐시 키 형식: {@code rec:{memberId}:{latGrid}:{lonGrid}}.
     * 그리드 값을 {@link #LOCATION_GRID_SIZE}로 나누어 근사 좌표를 복원합니다.</p>
     *
     * @param cacheKey Redis 캐시 키
     * @return [latitude, longitude] 배열, 파싱 실패 시 null
     */
    private double[] parseLocationFromCacheKey(String cacheKey) {
        try {
            // 키 형식: rec:{memberId}:{latGrid}:{lonGrid}
            String[] parts = cacheKey.split(":");
            if (parts.length < 4) {
                return null;
            }
            int latGrid = Integer.parseInt(parts[parts.length - 2]);
            int lonGrid = Integer.parseInt(parts[parts.length - 1]);
            return new double[]{(double) latGrid / LOCATION_GRID_SIZE, (double) lonGrid / LOCATION_GRID_SIZE};
        } catch (NumberFormatException e) {
            log.warn("캐시 키 위치 파싱 실패 — cacheKey: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 대체 추천 불가 응답 생성
     *
     * @return 대체 추천 없는 거절 응답
     */
    private RejectRecommendationResponse buildNoAlternativeResponse() {
        return RejectRecommendationResponse.builder()
                .rejected(true)
                .hasAlternative(false)
                .alternativeRecommendation(null)
                .message(null)
                .noAlternativeMessage("주변에 더 추천할 곳이 없어요. 거리를 넓혀볼까요?")
                .build();
    }

    /**
     * AI Pipeline 호출 후 TodayRecommendationsResponse 조립
     *
     * <p>AI Pipeline 응답이 빈 목록(CB Fallback)이면 규칙 기반 폴백 추천을 생성합니다.
     * 정상 응답이면 DB 저장 + Redis 캐시 저장 후 반환합니다.</p>
     *
     * @param memberId             회원 식별자
     * @param latitude             위도
     * @param longitude            경도
     * @param cacheKey             Redis 캐시 키
     * @param skipCache            캐시 스킵 여부
     * @param excludeRestaurantIds 제외할 식당 ID 목록
     * @return 추천 응답
     */
    private TodayRecommendationsResponse callAiPipelineAndBuild(String memberId,
                                                                  double latitude, double longitude,
                                                                  String cacheKey, boolean skipCache,
                                                                  List<String> excludeRestaurantIds) {
        // 카카오맵 Place API로 주변 식당 조회 (CB + Redis 캐시 보호)
        List<NearbyRestaurant> nearbyRestaurants = kakaoPlaceClient.searchNearbyRestaurants(
                latitude, longitude, 500);

        // 카카오맵 결과를 AI 요청용 DTO로 매핑
        List<AiRecommendationRequest.AvailableRestaurantData> availableRestaurants = null;
        if (!nearbyRestaurants.isEmpty()) {
            availableRestaurants = nearbyRestaurants.stream()
                    .map(r -> AiRecommendationRequest.AvailableRestaurantData.builder()
                            .restaurantId(r.restaurantId())
                            .restaurantName(r.restaurantName())
                            .representativeMenu(r.representativeMenu())
                            .category(r.category())
                            .distanceMeters(r.distanceMeters())
                            .estimatedWalkMinutes(r.estimatedWalkMinutes())
                            .allergens(List.of())
                            .build())
                    .toList();
            log.info("카카오맵 식당 {}개를 AI 요청에 포함 — memberId: {}", availableRestaurants.size(), memberId);
        } else {
            log.warn("카카오맵 식당 조회 결과 없음 — AI Pipeline 하드코딩 폴백 사용 — memberId: {}", memberId);
        }

        // OpenWeatherMap API로 현재 날씨 조회
        AiRecommendationRequest.WeatherContext weather = weatherClient.getCurrentWeather(latitude, longitude);

        AiRecommendationRequest aiRequest = AiRecommendationRequest.builder()
                .memberId(memberId)
                .latitude(latitude)
                .longitude(longitude)
                .requestedAt(Instant.now())
                .isColdStart(false)
                .weather(weather)
                .allergenFilter(List.of())
                .recentMealHistory(List.of())
                .excludeRestaurantIds(excludeRestaurantIds)
                .availableRestaurants(availableRestaurants)
                .skipCache(skipCache)
                .build();

        AiRecommendationResponse aiResponse = aiPipelineClient.getRecommendations(aiRequest);

        if (aiResponse == null || aiResponse.recommendations() == null || aiResponse.recommendations().isEmpty()) {
            log.warn("AI Pipeline 빈 응답(CB Fallback) — 규칙 기반 폴백 추천 생성 — memberId: {}", memberId);
            return generateFallbackRecommendations(memberId, latitude, longitude, true);
        }

        List<RecommendationCardDto> cards = aiResponse.recommendations().stream()
                .map(r -> {
                    String recId = UUID.randomUUID().toString();
                    saveRecommendationEntity(recId, memberId, r, aiResponse.isFallback());
                    return RecommendationCardDto.builder()
                            .recommendationId(recId)
                            .restaurantId(r.restaurantId())
                            .restaurantName(r.restaurantName())
                            .representativeMenu(r.representativeMenu())
                            .reasonSummary(r.reasonSummary())
                            .confidenceScore(r.confidenceScore())
                            .distanceMeters(Optional.ofNullable(r.distanceMeters()).orElse(FALLBACK_DISTANCE_METERS))
                            .estimatedWalkMinutes(Optional.ofNullable(r.estimatedWalkMinutes()).orElse(FALLBACK_WALK_MINUTES))
                            .category(r.category())
                            .isFallback(aiResponse.isFallback())
                            .build();
                })
                .collect(Collectors.toList());

        TodayRecommendationsResponse response = TodayRecommendationsResponse.builder()
                .recommendations(cards)
                .isColdStart(aiResponse.isColdStart())
                .coldStartMessage(aiResponse.isColdStart() ? "취향 데이터를 수집 중이에요." : null)
                .isFallback(aiResponse.isFallback())
                .fallbackMessage(aiResponse.isFallback() ? "최신 추천을 불러오고 있어요." : null)
                .generatedAt(LocalDateTime.now())
                .build();

        cacheRecommendationsResponse(cacheKey, response);
        return response;
    }

    /**
     * AI Pipeline 이유 생성 API 호출 후 RecommendationReasonResponse 조립
     *
     * @param memberId         회원 식별자
     * @param recommendationId 추천 식별자
     * @param recommendation   추천 엔티티
     * @param reasonCacheKey   Redis 캐시 키
     * @return 추천 이유 응답
     */
    private RecommendationReasonResponse callAiPipelineForReason(String memberId,
                                                                   String recommendationId,
                                                                   RecommendationEntity recommendation,
                                                                   String reasonCacheKey) {
        // 현재 날씨 조회 (서울 기본 좌표 — 이유 생성 시 좌표 불필요, 도시 단위 날씨 동일)
        AiRecommendationRequest.WeatherContext weather = weatherClient.getCurrentWeather(37.5665, 126.978);

        AiReasonRequest reasonRequest = AiReasonRequest.builder()
                .recommendationId(recommendationId)
                .restaurantId(recommendation.getRestaurantId())
                .restaurantName(recommendation.getRestaurantName())
                .category(recommendation.getCategory())
                .memberId(memberId)
                .confidenceScore(recommendation.getConfidenceScore())
                .representativeMenu(recommendation.getRepresentativeMenu())
                .weather(weather)
                .build();

        AiReasonResponse aiReasonResponse = aiPipelineClient.getRecommendationReason(reasonRequest);

        if (aiReasonResponse != null && aiReasonResponse.isReasonReady()) {
            stringRedisTemplate.opsForValue().set(reasonCacheKey,
                    aiReasonResponse.naturalLanguageReason(), REASON_CACHE_TTL);
            recommendation.updateReasonSummary(aiReasonResponse.naturalLanguageReason());
        }

        String naturalReason = aiReasonResponse != null
                ? aiReasonResponse.naturalLanguageReason()
                : "거리 및 평점 기반 추천";
        boolean isReasonReady = aiReasonResponse != null && aiReasonResponse.isReasonReady();
        List<String> contextTags = (aiReasonResponse != null && aiReasonResponse.contextTags() != null)
                ? aiReasonResponse.contextTags()
                : List.of();
        String fallbackMessage = (!isReasonReady)
                ? Optional.ofNullable(aiReasonResponse).map(AiReasonResponse::fallbackReason)
                          .orElse("추천 이유를 준비 중이에요.")
                : null;

        return RecommendationReasonResponse.builder()
                .recommendationId(recommendationId)
                .naturalLanguageReason(naturalReason)
                .confidenceScore(recommendation.getConfidenceScore())
                .contextTags(contextTags)
                .isReasonReady(isReasonReady)
                .fallbackMessage(fallbackMessage)
                .build();
    }

    /**
     * 캐시된 이유 문자열로 RecommendationReasonResponse 조립
     *
     * @param recommendationId 추천 식별자
     * @param cachedReason     캐시된 이유 문자열
     * @param recommendation   추천 엔티티
     * @return 추천 이유 응답
     */
    private RecommendationReasonResponse buildReasonResponseFromCache(String recommendationId,
                                                                       String cachedReason,
                                                                       RecommendationEntity recommendation) {
        return RecommendationReasonResponse.builder()
                .recommendationId(recommendationId)
                .naturalLanguageReason(cachedReason)
                .confidenceScore(recommendation.getConfidenceScore())
                .contextTags(List.of("취향"))
                .isReasonReady(true)
                .fallbackMessage(null)
                .build();
    }

    /**
     * 규칙 기반 폴백 추천 생성
     *
     * <p>AI Pipeline 완전 장애 시 위치 반경 내 인기 식당 규칙으로 추천 3개를 생성합니다.</p>
     *
     * @param memberId  회원 식별자
     * @param latitude  위도
     * @param longitude 경도
     * @param isFallback 폴백 여부
     * @return 규칙 기반 추천 응답
     */
    private TodayRecommendationsResponse generateFallbackRecommendations(String memberId,
                                                                          double latitude, double longitude,
                                                                          boolean isFallback) {
        List<RecommendationCardDto> cards = List.of(
                buildFallbackCard(UUID.randomUUID().toString(), "rest-001", "주변 인기 식당 1", "오늘의 메뉴", "한식", isFallback),
                buildFallbackCard(UUID.randomUUID().toString(), "rest-002", "주변 인기 식당 2", "세트 메뉴", "일식", isFallback),
                buildFallbackCard(UUID.randomUUID().toString(), "rest-003", "주변 인기 식당 3", "런치 스페셜", "양식", isFallback)
        );

        cards.forEach(card -> recommendationRepository.save(RecommendationEntity.builder()
                .recommendationId(card.recommendationId())
                .memberId(memberId)
                .restaurantId(card.restaurantId())
                .restaurantName(card.restaurantName())
                .representativeMenu(card.representativeMenu())
                .reasonSummary("주변 인기 식당이에요")
                .confidenceScore(FALLBACK_CONFIDENCE)
                .distanceMeters(FALLBACK_DISTANCE_METERS)
                .estimatedWalkMinutes(FALLBACK_WALK_MINUTES)
                .category(card.category())
                .isFallback(isFallback)
                .build()));

        return TodayRecommendationsResponse.builder()
                .recommendations(cards)
                .isColdStart(false)
                .coldStartMessage(null)
                .isFallback(isFallback)
                .fallbackMessage(isFallback ? "최신 추천을 불러오고 있어요." : null)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 폴백 추천 카드 빌드
     *
     * @param recId      추천 UUID
     * @param restId     식당 ID
     * @param restName   식당명
     * @param menu       대표 메뉴
     * @param category   카테고리
     * @param isFallback 폴백 여부
     * @return 추천 카드 DTO
     */
    private RecommendationCardDto buildFallbackCard(String recId, String restId,
                                                     String restName, String menu,
                                                     String category, boolean isFallback) {
        return RecommendationCardDto.builder()
                .recommendationId(recId)
                .restaurantId(restId)
                .restaurantName(restName)
                .representativeMenu(menu)
                .reasonSummary("주변 인기 식당이에요")
                .confidenceScore(FALLBACK_CONFIDENCE)
                .distanceMeters(FALLBACK_DISTANCE_METERS)
                .estimatedWalkMinutes(FALLBACK_WALK_MINUTES)
                .category(category)
                .isFallback(isFallback)
                .build();
    }

    /**
     * AI 추천 결과를 RecommendationEntity로 저장
     *
     * @param recId      추천 UUID
     * @param memberId   회원 식별자
     * @param restaurant AI 응답 추천 식당
     * @param isFallback 폴백 여부
     */
    private void saveRecommendationEntity(String recId, String memberId,
                                           AiRecommendationResponse.RecommendedRestaurant restaurant,
                                           boolean isFallback) {
        recommendationRepository.save(RecommendationEntity.builder()
                .recommendationId(recId)
                .memberId(memberId)
                .restaurantId(restaurant.restaurantId())
                .restaurantName(restaurant.restaurantName())
                .representativeMenu(restaurant.representativeMenu())
                .reasonSummary(restaurant.reasonSummary())
                .confidenceScore(restaurant.confidenceScore())
                .distanceMeters(Optional.ofNullable(restaurant.distanceMeters()).orElse(FALLBACK_DISTANCE_METERS))
                .estimatedWalkMinutes(Optional.ofNullable(restaurant.estimatedWalkMinutes()).orElse(FALLBACK_WALK_MINUTES))
                .category(restaurant.category())
                .isFallback(isFallback)
                .build());
    }

    /**
     * 추천 결과를 Redis에 캐시 저장
     *
     * @param cacheKey 캐시 키
     * @param response 추천 응답
     */
    private void cacheRecommendationsResponse(String cacheKey, TodayRecommendationsResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(cacheKey, json, REC_CACHE_TTL);
            log.debug("추천 캐시 저장 완료 — key: {}", cacheKey);
        } catch (JsonProcessingException ex) {
            log.warn("추천 캐시 직렬화 실패 — 캐시 저장 스킵", ex);
        }
    }

    /**
     * 추천 캐시 키 생성
     *
     * <p>위도·경도를 100m 그리드로 양자화하여 같은 구역 내 동일 키를 공유합니다.</p>
     *
     * @param memberId  회원 식별자
     * @param latitude  위도
     * @param longitude 경도
     * @return Redis 캐시 키
     */
    private String buildRecommendationCacheKey(String memberId, double latitude, double longitude) {
        int latGrid = (int) (latitude * LOCATION_GRID_SIZE);
        int lonGrid = (int) (longitude * LOCATION_GRID_SIZE);
        return REC_CACHE_PREFIX + memberId + ":" + latGrid + ":" + lonGrid;
    }

    /**
     * JSON 문자열을 TodayRecommendationsResponse로 역직렬화
     *
     * @param json JSON 문자열
     * @return 역직렬화된 응답, 실패 시 null
     */
    private TodayRecommendationsResponse deserializeRecommendationsResponse(String json) {
        try {
            return objectMapper.readValue(json, TodayRecommendationsResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("추천 캐시 역직렬화 실패 — 캐시 스킵", ex);
            return null;
        }
    }
}
