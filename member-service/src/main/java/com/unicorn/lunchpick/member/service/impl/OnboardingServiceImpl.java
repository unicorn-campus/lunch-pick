package com.unicorn.lunchpick.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.member.dto.request.CardSwipeResult;
import com.unicorn.lunchpick.member.dto.request.OnboardingProgressRequest;
import com.unicorn.lunchpick.member.dto.request.OnboardingRequest;
import com.unicorn.lunchpick.member.dto.response.OnboardingProgressSaveResponse;
import com.unicorn.lunchpick.member.dto.response.OnboardingResponse;
import com.unicorn.lunchpick.member.exception.MemberException;
import com.unicorn.lunchpick.member.repository.entity.DietaryRestrictionEntity;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.entity.TasteProfileEntity;
import com.unicorn.lunchpick.member.repository.jpa.DietaryRestrictionRepository;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import com.unicorn.lunchpick.member.repository.jpa.TasteProfileRepository;
import com.unicorn.lunchpick.member.service.OnboardingService;
import com.unicorn.lunchpick.member.service.TasteVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 취향 온보딩 서비스 구현체
 *
 * <p>스와이프 결과로 취향 벡터를 계산하고 DB에 저장합니다.
 * 진행 중 상태는 Redis에 임시 저장합니다(TTL 24시간).</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final int MINIMUM_SWIPE_COUNT = 7;
    private static final int TOP_CATEGORIES_COUNT = 3;
    private static final String PROGRESS_KEY_PREFIX = "onboarding:progress:";
    private static final Duration PROGRESS_TTL = Duration.ofHours(24);

    private final MemberRepository memberRepository;
    private final TasteProfileRepository tasteProfileRepository;
    private final DietaryRestrictionRepository dietaryRestrictionRepository;
    private final TasteVectorService tasteVectorService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>스와이프 7장 미만이면 {@link MemberException#insufficientSwipes()} 예외를 던집니다.
     * 성공 시 취향 프로파일과 식이제한 정보를 DB에 저장하고 회원 온보딩 완료 플래그를 설정합니다.</p>
     */
    @Override
    @Transactional
    public OnboardingResponse completeOnboarding(String memberId, OnboardingRequest request) {
        List<CardSwipeResult> swipeResults = request.swipeResults();
        if (swipeResults.size() < MINIMUM_SWIPE_COUNT) {
            throw MemberException.insufficientSwipes();
        }

        if (!Boolean.TRUE.equals(request.healthInfoConsentGiven())) {
            throw MemberException.healthInfoConsentRequired();
        }

        MemberEntity member = memberRepository.findByMemberId(memberId)
                .orElseThrow(MemberException::memberNotFound);

        Map<String, Double> tasteVector = tasteVectorService.calculateTasteVector(swipeResults);
        String tasteVectorJson = tasteVectorService.serializeToJson(tasteVector);

        saveTasteProfile(memberId, tasteVectorJson, swipeResults.size());
        saveDietaryRestriction(memberId, request.healthInfoConsentGiven());
        member.completeOnboarding();

        // Redis 진행 상태 삭제
        stringRedisTemplate.delete(PROGRESS_KEY_PREFIX + memberId);

        List<String> topCategories = tasteVectorService.getTopCategories(tasteVector, TOP_CATEGORIES_COUNT);

        log.info("온보딩 완료 — memberId: {}, topCategories: {}", memberId, topCategories);

        return OnboardingResponse.builder()
                .message("취향 분석이 완료되었어요! 맞춤 메뉴를 추천해드릴게요.")
                .topCategories(topCategories)
                .tasteVectorCreated(true)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>스와이프 결과 목록을 JSON으로 직렬화하여 Redis에 저장합니다.</p>
     */
    @Override
    public OnboardingProgressSaveResponse saveOnboardingProgress(String memberId, OnboardingProgressRequest request) {
        try {
            String progressJson = objectMapper.writeValueAsString(request.swipeResults());
            String key = PROGRESS_KEY_PREFIX + memberId;
            stringRedisTemplate.opsForValue().set(key, progressJson, PROGRESS_TTL);
            log.debug("온보딩 진행 상태 저장 — memberId: {}, count: {}", memberId, request.swipeResults().size());
            return OnboardingProgressSaveResponse.builder()
                    .message("진행 상태가 저장되었어요.")
                    .savedCount(request.swipeResults().size())
                    .build();
        } catch (JsonProcessingException ex) {
            log.error("온보딩 진행 상태 직렬화 실패 — memberId: {}", memberId, ex);
            return OnboardingProgressSaveResponse.builder()
                    .message("진행 상태 저장에 실패했어요.")
                    .savedCount(0)
                    .build();
        }
    }

    /**
     * 취향 프로파일 신규 생성 또는 갱신
     *
     * @param memberId       회원 식별자
     * @param tasteVectorJson 취향 벡터 JSON 문자열
     * @param swipeCount     스와이프 수 (초기 feedbackCount)
     */
    private void saveTasteProfile(String memberId, String tasteVectorJson, int swipeCount) {
        tasteProfileRepository.findByMemberId(memberId).ifPresentOrElse(
                existing -> existing.updateTasteVector(tasteVectorJson, swipeCount),
                () -> tasteProfileRepository.save(TasteProfileEntity.builder()
                        .memberId(memberId)
                        .tasteVector(tasteVectorJson)
                        .feedbackCount(swipeCount)
                        .isColdStart(swipeCount < 5)
                        .build())
        );
    }

    /**
     * 식이 제한 정보 신규 생성 또는 갱신
     *
     * @param memberId               회원 식별자
     * @param healthInfoConsentGiven 건강 정보 동의 여부
     */
    private void saveDietaryRestriction(String memberId, boolean healthInfoConsentGiven) {
        dietaryRestrictionRepository.findByMemberId(memberId).ifPresentOrElse(
                existing -> existing.update("[]", "[]", existing.getDietType(), healthInfoConsentGiven),
                () -> dietaryRestrictionRepository.save(DietaryRestrictionEntity.builder()
                        .memberId(memberId)
                        .allergens("[]")
                        .customAllergens("[]")
                        .dietType("일반")
                        .healthInfoConsentGiven(healthInfoConsentGiven)
                        .build())
        );
    }
}
