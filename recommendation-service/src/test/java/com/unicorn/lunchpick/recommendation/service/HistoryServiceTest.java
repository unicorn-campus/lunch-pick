package com.unicorn.lunchpick.recommendation.service;

import com.unicorn.lunchpick.recommendation.dto.response.InsightsResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealHistoryResponse;
import com.unicorn.lunchpick.recommendation.exception.RecommendationException;
import com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity;
import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import com.unicorn.lunchpick.recommendation.repository.jpa.FeedbackRepository;
import com.unicorn.lunchpick.recommendation.repository.jpa.MealRecordRepository;
import com.unicorn.lunchpick.recommendation.service.impl.HistoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * HistoryServiceImpl 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private MealRecordRepository mealRecordRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private static final String MEMBER_ID = "member-001";

    // -------------------------------------------------------------------------
    // getMealHistoryTimeline
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("식사 이력 조회 — 기록 없으면 빈 목록과 안내 메시지 반환")
    void getMealHistoryTimeline_noRecords_returnsEmptyWithMessage() {
        // Given
        given(mealRecordRepository.findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(MEMBER_ID), any(), any())).willReturn(List.of());
        given(feedbackRepository.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(List.of());

        // When
        MealHistoryResponse response = historyService.getMealHistoryTimeline(
                MEMBER_ID, null, null, false);

        // Then
        assertThat(response.meals()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.message()).contains("첫 식사를 기록해보세요");
    }

    @Test
    @DisplayName("식사 이력 조회 — 기록 있으면 피드백 만족도 매핑")
    void getMealHistoryTimeline_withRecordsAndFeedback_mapsSatisfaction() {
        // Given
        MealRecordEntity meal = MealRecordEntity.builder()
                .mealId("meal-001")
                .memberId(MEMBER_ID)
                .restaurantName("맛있는 식당")
                .menuName("김치찌개")
                .category("한식")
                .recordedAt(LocalDateTime.now())
                .build();

        FeedbackEntity feedback = FeedbackEntity.builder()
                .feedbackId("fb-001")
                .memberId(MEMBER_ID)
                .mealId("meal-001")
                .satisfaction("GOOD")
                .skipped(false)
                .build();

        given(mealRecordRepository.findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(MEMBER_ID), any(), any())).willReturn(List.of(meal));
        given(feedbackRepository.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(List.of(feedback));

        // When
        MealHistoryResponse response = historyService.getMealHistoryTimeline(
                MEMBER_ID, null, null, false);

        // Then
        assertThat(response.meals()).hasSize(1);
        assertThat(response.meals().get(0).satisfaction()).isEqualTo("GOOD");
        assertThat(response.meals().get(0).restaurantName()).isEqualTo("맛있는 식당");
        assertThat(response.message()).isNull();
    }

    @Test
    @DisplayName("식사 이력 조회 — FREE 회원 30일 초과 조회 시 PREMIUM_REQUIRED 예외")
    void getMealHistoryTimeline_freeMemberOver30Days_throwsException() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now();

        // When & Then
        assertThatThrownBy(() -> historyService.getMealHistoryTimeline(
                MEMBER_ID, startDate, endDate, false))
                .isInstanceOf(RecommendationException.class)
                .hasMessageContaining("프리미엄에서 전체 이력을 확인하세요");
    }

    @Test
    @DisplayName("식사 이력 조회 — PREMIUM 회원은 30일 초과 조회 가능")
    void getMealHistoryTimeline_premiumMemberOver30Days_success() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now();
        given(mealRecordRepository.findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(MEMBER_ID), any(), any())).willReturn(List.of());
        given(feedbackRepository.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(List.of());

        // When
        MealHistoryResponse response = historyService.getMealHistoryTimeline(
                MEMBER_ID, startDate, endDate, true);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.meals()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getTasteInsights
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("인사이트 조회 — 기록 10건 미만이면 hasEnoughData=false 반환")
    void getTasteInsights_insufficientData_returnsNotEnough() {
        // Given
        given(mealRecordRepository.countByMemberId(MEMBER_ID)).willReturn(5L);

        // When
        InsightsResponse response = historyService.getTasteInsights(MEMBER_ID);

        // Then
        assertThat(response.hasEnoughData()).isFalse();
        assertThat(response.currentRecordCount()).isEqualTo(5);
        assertThat(response.requiredRecordCount()).isEqualTo(10);
        assertThat(response.message()).contains("10끼 이상 기록");
    }

    @Test
    @DisplayName("인사이트 조회 — 기록 10건 이상이면 카테고리 분포 포함 응답")
    void getTasteInsights_sufficientData_returnsCategoryDistribution() {
        // Given
        given(mealRecordRepository.countByMemberId(MEMBER_ID)).willReturn(15L);

        List<MealRecordEntity> recentMeals = List.of(
                buildMeal("한식"), buildMeal("한식"), buildMeal("한식"),
                buildMeal("일식"), buildMeal("일식"),
                buildMeal("중식")
        );
        given(mealRecordRepository.findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(MEMBER_ID), any(), any())).willReturn(recentMeals);

        // When
        InsightsResponse response = historyService.getTasteInsights(MEMBER_ID);

        // Then
        assertThat(response.hasEnoughData()).isTrue();
        assertThat(response.topCategories()).isNotEmpty();
        // 한식이 가장 많으므로 첫 번째 카테고리
        assertThat(response.topCategories().get(0).category()).isEqualTo("한식");
    }

    @Test
    @DisplayName("인사이트 조회 — 30건 이상이면 마일스톤 배지 포함")
    void getTasteInsights_milestone_returnsMilestoneBadge() {
        // Given
        given(mealRecordRepository.countByMemberId(MEMBER_ID)).willReturn(35L);
        given(mealRecordRepository.findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                eq(MEMBER_ID), any(), any())).willReturn(List.of(buildMeal("한식")));

        // When
        InsightsResponse response = historyService.getTasteInsights(MEMBER_ID);

        // Then
        assertThat(response.milestone()).isNotNull();
        assertThat(response.milestone().achieved()).isTrue();
        assertThat(response.milestone().message()).contains("30끼 달성");
    }

    private MealRecordEntity buildMeal(String category) {
        return MealRecordEntity.builder()
                .mealId(java.util.UUID.randomUUID().toString())
                .memberId(MEMBER_ID)
                .restaurantName("식당")
                .menuName("메뉴")
                .category(category)
                .recordedAt(LocalDateTime.now())
                .build();
    }
}
