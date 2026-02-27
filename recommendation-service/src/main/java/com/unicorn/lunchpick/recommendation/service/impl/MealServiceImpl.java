package com.unicorn.lunchpick.recommendation.service.impl;

import com.unicorn.lunchpick.recommendation.dto.request.CreateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.request.FeedbackRequest;
import com.unicorn.lunchpick.recommendation.dto.request.UpdateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.response.FeedbackResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealResponse;
import com.unicorn.lunchpick.recommendation.exception.RecommendationException;
import com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity;
import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import com.unicorn.lunchpick.recommendation.repository.jpa.FeedbackRepository;
import com.unicorn.lunchpick.recommendation.repository.jpa.MealRecordRepository;
import com.unicorn.lunchpick.recommendation.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 식사 기록 서비스 구현체
 *
 * <p>식사 기록 생성/수정/취소 및 피드백 제출을 구현합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private static final LocalTime MEAL_START = LocalTime.of(10, 30);
    private static final LocalTime MEAL_END = LocalTime.of(15, 0);
    private static final Duration CANCEL_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_RESTAURANT_NAME = "식당";
    private static final String DEFAULT_CATEGORY = "기타";

    private final MealRecordRepository mealRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * {@inheritDoc}
     *
     * <p>식사 시간대(10:30~15:00) 검증 및 당일 중복 기록 방지.</p>
     */
    @Override
    @Transactional
    public MealResponse createMeal(String memberId, CreateMealRequest request) {
        LocalTime mealTime = request.recordedAt().toLocalTime();
        if (mealTime.isBefore(MEAL_START) || mealTime.isAfter(MEAL_END)) {
            throw RecommendationException.invalidMealTime();
        }

        LocalDateTime dayStart = LocalDate.from(request.recordedAt()).atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        if (mealRecordRepository.existsByMemberIdAndRecordedAtBetween(memberId, dayStart, dayEnd)) {
            throw RecommendationException.duplicateMealRecord();
        }

        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(UUID.randomUUID().toString())
                .memberId(memberId)
                .recommendationId(request.recommendationId())
                .restaurantId(request.restaurantId())
                .restaurantName(DEFAULT_RESTAURANT_NAME)
                .menuName(request.menuName() != null ? request.menuName() : "")
                .category(DEFAULT_CATEGORY)
                .recordedAt(request.recordedAt())
                .build();
        mealRecordRepository.save(meal);

        // 추천 캐시 무효화
        stringRedisTemplate.delete("rec:" + memberId + ":*");

        log.info("식사 기록 완료 — memberId: {}, mealId: {}", memberId, meal.getMealId());

        return MealResponse.builder()
                .mealId(meal.getMealId())
                .restaurantName(meal.getRestaurantName())
                .menuName(meal.getMenuName())
                .recordedAt(meal.getRecordedAt())
                .message("오늘 점심 기록 완료!")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MealResponse updateMeal(String memberId, String mealId, UpdateMealRequest request) {
        MealRecordEntity meal = mealRecordRepository.findByMealIdAndMemberId(mealId, memberId)
                .orElseThrow(RecommendationException::mealNotFound);

        meal.update(request.restaurantId(), null, request.menuName(), request.recordedAt());

        log.info("식사 기록 수정 완료 — memberId: {}, mealId: {}", memberId, mealId);

        return MealResponse.builder()
                .mealId(meal.getMealId())
                .restaurantName(meal.getRestaurantName())
                .menuName(meal.getMenuName())
                .recordedAt(meal.getRecordedAt())
                .message("식사 기록이 수정되었어요.")
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>생성 후 30초 이내에만 취소 가능합니다.</p>
     */
    @Override
    @Transactional
    public void deleteMeal(String memberId, String mealId) {
        MealRecordEntity meal = mealRecordRepository.findByMealIdAndMemberId(mealId, memberId)
                .orElseThrow(RecommendationException::mealNotFound);

        Duration elapsed = Duration.between(meal.getCreatedAt(), LocalDateTime.now());
        if (elapsed.compareTo(CANCEL_TIMEOUT) > 0) {
            throw RecommendationException.cancelTimeout();
        }

        mealRecordRepository.delete(meal);
        log.info("식사 기록 취소 완료 — memberId: {}, mealId: {}", memberId, mealId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FeedbackResponse submitFeedback(String memberId, String mealId, FeedbackRequest request) {
        mealRecordRepository.findByMealIdAndMemberId(mealId, memberId)
                .orElseThrow(RecommendationException::mealNotFound);

        boolean skipped = "NEUTRAL".equals(request.satisfaction()) && request.keyword() == null;

        FeedbackEntity feedback = FeedbackEntity.builder()
                .feedbackId(UUID.randomUUID().toString())
                .memberId(memberId)
                .mealId(mealId)
                .satisfaction(request.satisfaction())
                .keyword(request.keyword())
                .skipped(skipped)
                .build();
        feedbackRepository.save(feedback);

        long totalCount = feedbackRepository.countByMemberId(memberId);
        log.info("피드백 제출 완료 — memberId: {}, satisfaction: {}, totalCount: {}",
                memberId, request.satisfaction(), totalCount);

        return FeedbackResponse.builder()
                .message("피드백 감사해요!")
                .reflectionMessage("내일 추천에 반영할게요.")
                .totalFeedbackCount(totalCount)
                .build();
    }
}
