package com.unicorn.lunchpick.recommendation.service.impl;

import com.unicorn.lunchpick.recommendation.dto.response.InsightsResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealHistoryItemDto;
import com.unicorn.lunchpick.recommendation.dto.response.MealHistoryResponse;
import com.unicorn.lunchpick.recommendation.exception.RecommendationException;
import com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity;
import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import com.unicorn.lunchpick.recommendation.repository.jpa.FeedbackRepository;
import com.unicorn.lunchpick.recommendation.repository.jpa.MealRecordRepository;
import com.unicorn.lunchpick.recommendation.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 식사 이력 및 취향 인사이트 서비스 구현체
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private static final int FREE_HISTORY_LIMIT_DAYS = 30;
    private static final int INSIGHT_MIN_RECORDS = 10;
    private static final int MILESTONE_COUNT = 30;

    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "한식", "#FF6B6B",
            "일식", "#4ECDC4",
            "중식", "#45B7D1",
            "양식", "#96CEB4",
            "분식", "#FFEAA7",
            "샐러드/건강식", "#81ECEC",
            "기타", "#DFE6E9"
    );

    private final MealRecordRepository mealRecordRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MealHistoryResponse getMealHistoryTimeline(String memberId, LocalDate startDate,
                                                       LocalDate endDate, boolean isPremium) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedEnd = endDate != null ? endDate : today;
        LocalDate resolvedStart = startDate != null ? startDate : today.minusDays(FREE_HISTORY_LIMIT_DAYS);

        // FREE 회원 30일 제한 검증
        if (!isPremium) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(resolvedStart, resolvedEnd);
            if (daysBetween > FREE_HISTORY_LIMIT_DAYS) {
                throw RecommendationException.premiumRequired();
            }
        }

        LocalDateTime from = resolvedStart.atStartOfDay();
        LocalDateTime to = resolvedEnd.plusDays(1).atStartOfDay();

        List<MealRecordEntity> meals = mealRecordRepository
                .findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(memberId, from, to);

        // 피드백 매핑
        Map<String, String> feedbackMap = feedbackRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .collect(Collectors.toMap(FeedbackEntity::getMealId, FeedbackEntity::getSatisfaction,
                        (existing, replacement) -> existing));

        List<MealHistoryItemDto> items = meals.stream()
                .map(meal -> MealHistoryItemDto.builder()
                        .mealId(meal.getMealId())
                        .date(meal.getRecordedAt().toLocalDate())
                        .restaurantName(meal.getRestaurantName())
                        .menuName(meal.getMenuName())
                        .category(meal.getCategory())
                        .categoryColor(CATEGORY_COLORS.getOrDefault(meal.getCategory(), "#DFE6E9"))
                        .satisfaction(feedbackMap.get(meal.getMealId()))
                        .recordedAt(meal.getRecordedAt())
                        .build())
                .toList();

        String message = items.isEmpty() ? "아직 기록이 없어요. 첫 식사를 기록해보세요!" : null;

        log.debug("식사 이력 조회 완료 — memberId: {}, count: {}", memberId, items.size());

        return MealHistoryResponse.builder()
                .meals(items)
                .totalCount(items.size())
                .message(message)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public InsightsResponse getTasteInsights(String memberId) {
        long totalCount = mealRecordRepository.countByMemberId(memberId);

        if (totalCount < INSIGHT_MIN_RECORDS) {
            log.debug("인사이트 데이터 부족 — memberId: {}, count: {}", memberId, totalCount);
            return InsightsResponse.builder()
                    .hasEnoughData(false)
                    .currentRecordCount((int) totalCount)
                    .requiredRecordCount(INSIGHT_MIN_RECORDS)
                    .message(INSIGHT_MIN_RECORDS + "끼 이상 기록하면 취향 인사이트가 열려요!")
                    .topCategories(List.of())
                    .weeklyPattern(List.of())
                    .satisfactionTrend(List.of())
                    .weeklySummary(null)
                    .milestone(null)
                    .build();
        }

        // 카테고리 분포 계산 (최근 30일)
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        List<MealRecordEntity> recentMeals = mealRecordRepository
                .findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(memberId, from, to);

        Map<String, Long> categoryCount = recentMeals.stream()
                .collect(Collectors.groupingBy(MealRecordEntity::getCategory, Collectors.counting()));

        long mealTotal = recentMeals.size();
        List<InsightsResponse.CategoryDistributionDto> topCategories = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> InsightsResponse.CategoryDistributionDto.builder()
                        .category(entry.getKey())
                        .count(entry.getValue().intValue())
                        .percentage(mealTotal > 0 ? Math.round((entry.getValue() * 1000.0 / mealTotal)) / 10.0 : 0.0)
                        .color(CATEGORY_COLORS.getOrDefault(entry.getKey(), "#DFE6E9"))
                        .build())
                .toList();

        // 마일스톤 배지
        InsightsResponse.MilestoneBadgeDto milestone = null;
        if (totalCount >= MILESTONE_COUNT) {
            milestone = InsightsResponse.MilestoneBadgeDto.builder()
                    .achieved(true)
                    .count((int) totalCount)
                    .message(MILESTONE_COUNT + "끼 달성! 이제 취향 분석이 훨씬 정확해졌어요.")
                    .accuracyImprovement(23.5)
                    .build();
        }

        log.debug("인사이트 조회 완료 — memberId: {}, totalCount: {}", memberId, totalCount);

        return InsightsResponse.builder()
                .hasEnoughData(true)
                .currentRecordCount((int) totalCount)
                .requiredRecordCount(INSIGHT_MIN_RECORDS)
                .message(null)
                .topCategories(topCategories)
                .weeklyPattern(List.of())
                .satisfactionTrend(List.of())
                .weeklySummary(null)
                .milestone(milestone)
                .build();
    }
}
