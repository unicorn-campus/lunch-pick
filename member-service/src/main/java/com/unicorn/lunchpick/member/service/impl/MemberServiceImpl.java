package com.unicorn.lunchpick.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.member.dto.request.DietaryRestrictionsRequest;
import com.unicorn.lunchpick.member.dto.request.LocationConsentRequest;
import com.unicorn.lunchpick.member.dto.request.UpdateProfileRequest;
import com.unicorn.lunchpick.member.dto.response.DietaryRestrictionsResponse;
import com.unicorn.lunchpick.member.dto.response.LocationConsentResponse;
import com.unicorn.lunchpick.member.dto.response.MemberProfileResponse;
import com.unicorn.lunchpick.member.dto.response.NotificationSettingsDto;
import com.unicorn.lunchpick.member.dto.response.SubscriptionStatusDto;
import com.unicorn.lunchpick.member.dto.response.SubscriptionStatusResponse;
import com.unicorn.lunchpick.member.dto.response.TasteProfileResponse;
import java.time.LocalDateTime;
import com.unicorn.lunchpick.member.exception.MemberException;
import com.unicorn.lunchpick.member.repository.entity.DietaryRestrictionEntity;
import com.unicorn.lunchpick.member.repository.entity.LocationConsentEntity;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.entity.TasteProfileEntity;
import com.unicorn.lunchpick.member.repository.jpa.DietaryRestrictionRepository;
import com.unicorn.lunchpick.member.repository.jpa.LocationConsentRepository;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import com.unicorn.lunchpick.member.repository.jpa.TasteProfileRepository;
import com.unicorn.lunchpick.member.service.MemberService;
import com.unicorn.lunchpick.member.service.TasteVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 회원 프로필 관리 서비스 구현체
 *
 * <p>회원 프로필 조회/수정, 위치 동의, 식이 제한 설정, 구독 상태 조회를 구현합니다.</p>
 *
 * <p><b>Redis 키 패턴:</b></p>
 * <ul>
 *   <li>{@code subscription:{memberId}} — 구독 상태 캐시 (TTL 24시간)</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final String SUBSCRIPTION_KEY_PREFIX = "subscription:";
    private static final String DEFAULT_PLAN = "FREE";
    private static final int FREE_HISTORY_LIMIT_DAYS = 7;
    private static final Duration SUBSCRIPTION_CACHE_TTL = Duration.ofHours(24);

    private final MemberRepository memberRepository;
    private final TasteProfileRepository tasteProfileRepository;
    private final DietaryRestrictionRepository dietaryRestrictionRepository;
    private final LocationConsentRepository locationConsentRepository;
    private final TasteVectorService tasteVectorService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(String memberId) {
        MemberEntity member = memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        DietaryRestrictionEntity dietary = dietaryRestrictionRepository
                .findByMemberId(memberId).orElse(null);

        List<String> allergens = mergeAllergens(dietary);
        String dietType = dietary != null ? dietary.getDietType() : "일반";

        SubscriptionStatusDto subscription = buildSubscriptionStatusDto(memberId);

        return MemberProfileResponse.builder()
                .memberId(member.getMemberId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .dietType(dietType)
                .allergens(allergens)
                .locationEnabled(member.isLocationEnabled())
                .notificationSettings(NotificationSettingsDto.builder()
                        .recommendationAlert(member.isRecommendationAlert())
                        .feedbackReminder(member.isFeedbackReminder())
                        .build())
                .subscription(subscription)
                .onboardingCompleted(member.isOnboardingCompleted())
                .createdAt(member.getCreatedAt())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MemberProfileResponse updateProfile(String memberId, UpdateProfileRequest request) {
        MemberEntity member = memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        boolean recommendationAlert = member.isRecommendationAlert();
        boolean feedbackReminder = member.isFeedbackReminder();

        if (request.notificationSettings() != null) {
            if (request.notificationSettings().recommendationAlert() != null) {
                recommendationAlert = request.notificationSettings().recommendationAlert();
            }
            if (request.notificationSettings().feedbackReminder() != null) {
                feedbackReminder = request.notificationSettings().feedbackReminder();
            }
        }

        member.updateProfile(request.nickname(), request.email(), recommendationAlert, feedbackReminder);

        log.info("프로필 수정 완료 — memberId: {}", memberId);
        return getProfile(memberId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LocationConsentResponse updateLocationConsent(String memberId, LocationConsentRequest request) {
        MemberEntity member = memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        LocationConsentEntity consent = LocationConsentEntity.builder()
                .memberId(memberId)
                .consented(request.consented())
                .consentedAt(request.consentedAt())
                .build();
        locationConsentRepository.save(consent);
        member.updateLocationEnabled(request.consented());

        String message = request.consented()
                ? "위치 기반 추천이 활성화되었어요."
                : "위치 기반 추천이 비활성화되었어요.";

        log.info("위치 동의 처리 완료 — memberId: {}, consented: {}", memberId, request.consented());

        return LocationConsentResponse.builder()
                .locationEnabled(request.consented())
                .message(message)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public DietaryRestrictionsResponse updateDietaryRestrictions(String memberId, DietaryRestrictionsRequest request) {
        memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        if (!Boolean.TRUE.equals(request.healthInfoConsentGiven())) {
            throw MemberException.healthInfoConsentRequired();
        }

        List<String> allergensList = request.allergens() != null ? request.allergens() : List.of();
        List<String> customAllergensList = request.customAllergens() != null ? request.customAllergens() : List.of();
        String allergenJson = serializeListToJson(allergensList);
        String customAllergenJson = serializeListToJson(customAllergensList);
        String dietType = request.dietType() != null ? request.dietType() : "일반";

        dietaryRestrictionRepository.findByMemberId(memberId).ifPresentOrElse(
                existing -> existing.update(allergenJson, customAllergenJson, dietType, true),
                () -> dietaryRestrictionRepository.save(DietaryRestrictionEntity.builder()
                        .memberId(memberId)
                        .allergens(allergenJson)
                        .customAllergens(customAllergenJson)
                        .dietType(dietType)
                        .healthInfoConsentGiven(true)
                        .build())
        );

        List<String> appliedAllergens = new ArrayList<>(allergensList);
        appliedAllergens.addAll(customAllergensList);

        log.info("식이 제한 설정 완료 — memberId: {}, dietType: {}", memberId, dietType);

        return DietaryRestrictionsResponse.builder()
                .message("식이 제한 설정이 저장되었어요.")
                .appliedAllergens(appliedAllergens)
                .dietType(dietType)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Redis 캐시에 구독 정보가 없으면 FREE 플랜 기본값을 반환합니다.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(String memberId) {
        memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        String cachedPlan = stringRedisTemplate.opsForValue()
                .get(SUBSCRIPTION_KEY_PREFIX + memberId + ":plan");
        String cachedExpiresAt = stringRedisTemplate.opsForValue()
                .get(SUBSCRIPTION_KEY_PREFIX + memberId + ":expiresAt");

        // PREMIUM_MONTHLY / PREMIUM_ANNUAL → PREMIUM 으로 정규화
        String plan = cachedPlan != null && cachedPlan.startsWith("PREMIUM") ? "PREMIUM"
                : cachedPlan != null ? cachedPlan : DEFAULT_PLAN;
        int historyLimitDays = "PREMIUM".equals(plan) ? 90 : FREE_HISTORY_LIMIT_DAYS;
        LocalDateTime expiresAt = parseLocalDateTime(cachedExpiresAt);

        return SubscriptionStatusResponse.builder()
                .plan(plan)
                .historyLimitDays(historyLimitDays)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public TasteProfileResponse getTasteProfile(String memberId) {
        memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        TasteProfileEntity tasteProfile = tasteProfileRepository.findByMemberId(memberId)
                .orElse(null);

        DietaryRestrictionEntity dietary = dietaryRestrictionRepository
                .findByMemberId(memberId).orElse(null);

        Map<String, Double> tasteVector = tasteProfile != null
                ? tasteVectorService.deserializeFromJson(tasteProfile.getTasteVector())
                : Map.of();

        List<String> allergenFilter = mergeAllergens(dietary);
        String dietType = dietary != null ? dietary.getDietType() : "일반";
        int feedbackCount = tasteProfile != null ? tasteProfile.getFeedbackCount() : 0;
        boolean isColdStart = tasteProfile == null || tasteProfile.isColdStart();

        return TasteProfileResponse.builder()
                .memberId(memberId)
                .tasteVector(tasteVector)
                .allergenFilter(allergenFilter)
                .dietType(dietType)
                .feedbackCount(feedbackCount)
                .isColdStart(isColdStart)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSubscriptionCache(String memberId, String plan, String expiresAt) {
        stringRedisTemplate.opsForValue().set(
                SUBSCRIPTION_KEY_PREFIX + memberId + ":plan", plan, SUBSCRIPTION_CACHE_TTL);
        if (expiresAt != null) {
            stringRedisTemplate.opsForValue().set(
                    SUBSCRIPTION_KEY_PREFIX + memberId + ":expiresAt", expiresAt, SUBSCRIPTION_CACHE_TTL);
        }
        log.info("구독 캐시 갱신 완료 — memberId: {}, plan: {}", memberId, plan);
    }

    /**
     * 알레르겐 목록과 직접 입력 알레르겐 목록을 병합
     *
     * @param dietary 식이 제한 엔티티 (null 허용)
     * @return 병합된 알레르겐 문자열 목록
     */
    private List<String> mergeAllergens(DietaryRestrictionEntity dietary) {
        if (dietary == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(deserializeListFromJson(dietary.getAllergens()));
        result.addAll(deserializeListFromJson(dietary.getCustomAllergens()));
        return result;
    }

    /**
     * Redis 캐시 기반 구독 상태 DTO 빌드
     *
     * @param memberId 회원 식별자
     * @return 구독 상태 DTO
     */
    private SubscriptionStatusDto buildSubscriptionStatusDto(String memberId) {
        String cachedPlan = stringRedisTemplate.opsForValue()
                .get(SUBSCRIPTION_KEY_PREFIX + memberId + ":plan");
        String cachedExpiresAt = stringRedisTemplate.opsForValue()
                .get(SUBSCRIPTION_KEY_PREFIX + memberId + ":expiresAt");

        // PREMIUM_MONTHLY / PREMIUM_ANNUAL → PREMIUM 으로 정규화
        String plan = cachedPlan != null && cachedPlan.startsWith("PREMIUM") ? "PREMIUM"
                : cachedPlan != null ? cachedPlan : DEFAULT_PLAN;
        int historyLimitDays = "PREMIUM".equals(plan) ? 90 : FREE_HISTORY_LIMIT_DAYS;
        LocalDateTime expiresAt = parseLocalDateTime(cachedExpiresAt);

        return SubscriptionStatusDto.builder()
                .plan(plan)
                .historyLimitDays(historyLimitDays)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * ISO 8601 문자열을 LocalDateTime으로 파싱
     *
     * @param value ISO 8601 문자열 (null 허용)
     * @return LocalDateTime 또는 null
     */
    private LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            log.warn("구독 만료일 파싱 실패 — value: {}", value);
            return null;
        }
    }

    /**
     * 문자열 목록을 JSON 배열 문자열로 직렬화
     *
     * @param list 문자열 목록
     * @return JSON 배열 문자열
     */
    private String serializeListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException ex) {
            log.error("목록 JSON 직렬화 실패", ex);
            return "[]";
        }
    }

    /**
     * JSON 배열 문자열을 문자열 목록으로 역직렬화
     *
     * @param json JSON 배열 문자열
     * @return 문자열 목록
     */
    private List<String> deserializeListFromJson(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            log.error("목록 JSON 역직렬화 실패 — json: {}", json, ex);
            return List.of();
        }
    }
}
