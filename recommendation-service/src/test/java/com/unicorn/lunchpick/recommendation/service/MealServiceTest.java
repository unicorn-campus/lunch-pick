package com.unicorn.lunchpick.recommendation.service;

import com.unicorn.lunchpick.recommendation.dto.request.CreateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.request.FeedbackRequest;
import com.unicorn.lunchpick.recommendation.dto.request.UpdateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.response.FeedbackResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealResponse;
import com.unicorn.lunchpick.recommendation.exception.RecommendationException;
import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import com.unicorn.lunchpick.recommendation.repository.jpa.FeedbackRepository;
import com.unicorn.lunchpick.recommendation.repository.jpa.MealRecordRepository;
import com.unicorn.lunchpick.recommendation.service.impl.MealServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * MealServiceImpl 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MealServiceImpl mealService;

    private static final String MEMBER_ID = "member-001";
    private static final String MEAL_ID = "meal-001";

    // -------------------------------------------------------------------------
    // createMeal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("식사 기록 생성 — 점심 시간대(12:00)에 정상 생성")
    void createMeal_validTime_returnsMealResponse() {
        // Given
        LocalDateTime recordedAt = LocalDateTime.now().withHour(12).withMinute(0);
        CreateMealRequest request = new CreateMealRequest(
                "rec-001", "rest-001", "김치찌개", recordedAt);
        given(mealRecordRepository.existsByMemberIdAndRecordedAtBetween(
                eq(MEMBER_ID), any(), any())).willReturn(false);
        given(mealRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        MealResponse response = mealService.createMeal(MEMBER_ID, request);

        // Then
        assertThat(response.menuName()).isEqualTo("김치찌개");
        assertThat(response.message()).contains("오늘 점심 기록 완료");
        then(mealRecordRepository).should().save(any());
    }

    @Test
    @DisplayName("식사 기록 생성 — 09:00에 기록 시 INVALID_MEAL_TIME 예외")
    void createMeal_beforeMealTime_throwsException() {
        // Given
        LocalDateTime recordedAt = LocalDateTime.now().withHour(9).withMinute(0);
        CreateMealRequest request = new CreateMealRequest(
                null, "rest-001", "김치찌개", recordedAt);

        // When & Then
        assertThatThrownBy(() -> mealService.createMeal(MEMBER_ID, request))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("10:30~15:00");
        then(mealRecordRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("식사 기록 생성 — 16:00에 기록 시 INVALID_MEAL_TIME 예외")
    void createMeal_afterMealTime_throwsException() {
        // Given
        LocalDateTime recordedAt = LocalDateTime.now().withHour(16).withMinute(0);
        CreateMealRequest request = new CreateMealRequest(
                null, "rest-001", "김치찌개", recordedAt);

        // When & Then
        assertThatThrownBy(() -> mealService.createMeal(MEMBER_ID, request))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("10:30~15:00");
    }

    @Test
    @DisplayName("식사 기록 생성 — 당일 중복 기록 시 DUPLICATE_MEAL_RECORD 예외")
    void createMeal_duplicate_throwsException() {
        // Given
        LocalDateTime recordedAt = LocalDateTime.now().withHour(12).withMinute(0);
        CreateMealRequest request = new CreateMealRequest(
                null, "rest-001", "김치찌개", recordedAt);
        given(mealRecordRepository.existsByMemberIdAndRecordedAtBetween(
                eq(MEMBER_ID), any(), any())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> mealService.createMeal(MEMBER_ID, request))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("이미 기록");
        then(mealRecordRepository).should(never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateMeal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("식사 기록 수정 — 정상 수정")
    void updateMeal_success_returnsMealResponse() {
        // Given
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(MEAL_ID)
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("된장찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now().withHour(12))
                .build();
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.of(meal));

        UpdateMealRequest request = new UpdateMealRequest(
                "rest-002", "비빔밥", LocalDateTime.now().withHour(13));

        // When
        MealResponse response = mealService.updateMeal(MEMBER_ID, MEAL_ID, request);

        // Then
        assertThat(response.mealId()).isEqualTo(MEAL_ID);
        assertThat(response.message()).contains("수정");
    }

    @Test
    @DisplayName("식사 기록 수정 — 존재하지 않는 기록 시 MEAL_NOT_FOUND 예외")
    void updateMeal_notFound_throwsException() {
        // Given
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.empty());
        UpdateMealRequest request = new UpdateMealRequest("rest-002", "비빔밥", LocalDateTime.now().withHour(13));

        // When & Then
        assertThatThrownBy(() -> mealService.updateMeal(MEMBER_ID, MEAL_ID, request))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("식사 기록을 찾을 수 없습니다");
    }

    // -------------------------------------------------------------------------
    // deleteMeal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("식사 기록 취소 — 30초 이내 정상 취소")
    void deleteMeal_withinTimeout_deletesRecord() {
        // Given
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(MEAL_ID)
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("김치찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now().withHour(12))
                .build();
        // createdAt을 방금(10초 전)으로 설정 — 30초 이내
        setCreatedAt(meal, LocalDateTime.now().minusSeconds(10));
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.of(meal));

        // When
        mealService.deleteMeal(MEMBER_ID, MEAL_ID);

        // Then
        then(mealRecordRepository).should().delete(meal);
    }

    @Test
    @DisplayName("식사 기록 취소 — 30초 초과 시 CANCEL_TIMEOUT 예외")
    void deleteMeal_afterTimeout_throwsException() {
        // Given — createdAt을 2분 전으로 설정하여 타임아웃 유발
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(MEAL_ID)
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("김치찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now().withHour(12))
                .build();
        setCreatedAt(meal, LocalDateTime.now().minusMinutes(2));
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.of(meal));

        // When & Then
        assertThatThrownBy(() -> mealService.deleteMeal(MEMBER_ID, MEAL_ID))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("이력 화면에서 수정");
        then(mealRecordRepository).should(never()).delete(any());
    }

    /**
     * BaseTimeEntity.createdAt 필드를 Reflection으로 설정 (테스트 전용)
     */
    private void setCreatedAt(MealRecordEntity entity, LocalDateTime value) {
        try {
            java.lang.reflect.Field field =
                    com.unicorn.lunchpick.common.entity.BaseTimeEntity.class
                            .getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException("createdAt 설정 실패", e);
        }
    }

    // -------------------------------------------------------------------------
    // submitFeedback
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("피드백 제출 — GOOD 만족도 정상 제출")
    void submitFeedback_good_returnsFeedbackResponse() {
        // Given
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(MEAL_ID)
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("김치찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now().withHour(12))
                .build();
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.of(meal));
        given(feedbackRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(feedbackRepository.countByMemberId(MEMBER_ID)).willReturn(1L);

        FeedbackRequest request = new FeedbackRequest("GOOD", "TASTE");

        // When
        FeedbackResponse response = mealService.submitFeedback(MEMBER_ID, MEAL_ID, request);

        // Then
        assertThat(response.message()).contains("피드백 감사");
        assertThat(response.totalFeedbackCount()).isEqualTo(1L);
        then(feedbackRepository).should().save(any());
    }

    @Test
    @DisplayName("피드백 제출 — NEUTRAL + keyword null 이면 skipped=true")
    void submitFeedback_neutralWithoutKeyword_skippedTrue() {
        // Given
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId(MEAL_ID)
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("김치찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now().withHour(12))
                .build();
        given(mealRecordRepository.findByMealIdAndMemberId(MEAL_ID, MEMBER_ID))
                .willReturn(Optional.of(meal));
        given(feedbackRepository.save(any())).willAnswer(inv -> {
            var fb = (com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity) inv.getArgument(0);
            assertThat(fb.isSkipped()).isTrue();
            return fb;
        });
        given(feedbackRepository.countByMemberId(MEMBER_ID)).willReturn(1L);

        FeedbackRequest request = new FeedbackRequest("NEUTRAL", null);

        // When
        FeedbackResponse response = mealService.submitFeedback(MEMBER_ID, MEAL_ID, request);

        // Then
        assertThat(response).isNotNull();
    }
}
