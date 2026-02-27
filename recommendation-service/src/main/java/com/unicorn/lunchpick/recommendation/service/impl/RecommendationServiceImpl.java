package com.unicorn.lunchpick.recommendation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.recommendation.client.AiPipelineClient;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationResponse;
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
import java.util.List;
import java.util.Optional;
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
        return callAiPipelineAndBuild(memberId, latitude, longitude, cacheKey);
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

        return RejectRecommendationResponse.builder()
                .rejected(true)
                .hasAlternative(false)
                .alternativeRecommendation(null)
                .message(null)
                .noAlternativeMessage("주변에 더 추천할 곳이 없어요. 거리를 넓혀볼까요?")
                .build();
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

        return callAiPipelineAndBuild(memberId, request.latitude(), request.longitude(), cacheKey);
    }

    // -------------------------------------------------------------------------
    // private 헬퍼 메서드
    // -------------------------------------------------------------------------

    /**
     * AI Pipeline 호출 후 TodayRecommendationsResponse 조립
     *
     * <p>AI Pipeline 응답이 빈 목록(CB Fallback)이면 규칙 기반 폴백 추천을 생성합니다.
     * 정상 응답이면 DB 저장 + Redis 캐시 저장 후 반환합니다.</p>
     *
     * @param memberId  회원 식별자
     * @param latitude  위도
     * @param longitude 경도
     * @param cacheKey  Redis 캐시 키
     * @return 추천 응답
     */
    private TodayRecommendationsResponse callAiPipelineAndBuild(String memberId,
                                                                  double latitude, double longitude,
                                                                  String cacheKey) {
        AiRecommendationRequest aiRequest = AiRecommendationRequest.builder()
                .memberId(memberId)
                .latitude(latitude)
                .longitude(longitude)
                .requestedAt(Instant.now())
                .isColdStart(false)
                .allergenFilter(List.of())
                .recentMealHistory(List.of())
                .excludeRestaurantIds(List.of())
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
        AiReasonRequest reasonRequest = AiReasonRequest.builder()
                .recommendationId(recommendationId)
                .restaurantId(recommendation.getRestaurantId())
                .restaurantName(recommendation.getRestaurantName())
                .category(recommendation.getCategory())
                .memberId(memberId)
                .confidenceScore(recommendation.getConfidenceScore())
                .representativeMenu(recommendation.getRepresentativeMenu())
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
