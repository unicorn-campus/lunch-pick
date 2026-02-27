package com.unicorn.lunchpick.recommendation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.recommendation.client.AiPipelineClient;
import com.unicorn.lunchpick.recommendation.client.dto.AiInsightRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiInsightResponse;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private static final String INSIGHT_CACHE_PREFIX = "insight:";
    private static final Duration INSIGHT_CACHE_TTL = Duration.ofHours(6);

    private static final String[] DOW_NAMES = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private final MealRecordRepository mealRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final AiPipelineClient aiPipelineClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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

        // 피드백 매핑 (satisfaction + keyword)
        Map<String, FeedbackEntity> feedbackMap = feedbackRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .collect(Collectors.toMap(FeedbackEntity::getMealId, fb -> fb,
                        (existing, replacement) -> existing));

        List<MealHistoryItemDto> items = meals.stream()
                .map(meal -> {
                    FeedbackEntity fb = feedbackMap.get(meal.getMealId());
                    return MealHistoryItemDto.builder()
                            .mealId(meal.getMealId())
                            .date(meal.getRecordedAt().toLocalDate())
                            .restaurantName(meal.getRestaurantName())
                            .menuName(meal.getMenuName())
                            .category(meal.getCategory())
                            .categoryColor(CATEGORY_COLORS.getOrDefault(meal.getCategory(), "#DFE6E9"))
                            .satisfaction(fb != null ? fb.getSatisfaction() : null)
                            .keyword(fb != null ? fb.getKeyword() : null)
                            .recordedAt(meal.getRecordedAt());
                })
                .map(MealHistoryItemDto.MealHistoryItemDtoBuilder::build)
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
                    .mealBalance(null)
                    .satisfactionAnalysis(null)
                    .isAiGenerated(false)
                    .build();
        }

        // Redis 캐시 확인
        String cacheKey = INSIGHT_CACHE_PREFIX + memberId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                InsightsResponse cachedResponse = objectMapper.readValue(cached, InsightsResponse.class);
                log.debug("인사이트 캐시 히트 — memberId: {}", memberId);
                return cachedResponse;
            } catch (Exception e) {
                log.warn("인사이트 캐시 역직렬화 실패 — 캐시 스킵: {}", e.getMessage());
            }
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

        // SQL 집계: weeklyPattern (요일별 최다 카테고리)
        List<InsightsResponse.WeeklyPatternDto> weeklyPattern = buildWeeklyPattern(memberId, from);

        // SQL 집계: satisfactionTrend (주간별 만족 비율)
        List<InsightsResponse.SatisfactionTrendDto> satisfactionTrend = buildSatisfactionTrend(memberId, from);

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

        // AI Pipeline 호출 (인사이트 분석)
        String weeklySummary = null;
        InsightsResponse.MealBalanceDto mealBalance = null;
        InsightsResponse.SatisfactionAnalysisDto satisfactionAnalysis = null;
        boolean isAiGenerated = false;

        try {
            AiInsightResponse aiResponse = callAiPipelineForInsights(
                    memberId, recentMeals, categoryCount, mealTotal);

            if (aiResponse != null) {
                weeklySummary = aiResponse.weeklySummary();
                isAiGenerated = true;

                if (aiResponse.mealBalance() != null) {
                    mealBalance = InsightsResponse.MealBalanceDto.builder()
                            .diversityScore(aiResponse.mealBalance().diversityScore())
                            .diagnosis(aiResponse.mealBalance().diagnosis())
                            .coachingComment(aiResponse.mealBalance().coachingComment())
                            .build();
                }

                if (aiResponse.satisfactionAnalysis() != null) {
                    satisfactionAnalysis = InsightsResponse.SatisfactionAnalysisDto.builder()
                            .satisfactionRate(aiResponse.satisfactionAnalysis().satisfactionRate())
                            .patterns(aiResponse.satisfactionAnalysis().patterns())
                            .patternComment(aiResponse.satisfactionAnalysis().patternComment())
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("AI 인사이트 호출 실패 — 기존 SQL 데이터만 반환: {}", e.getMessage());
        }

        log.debug("인사이트 조회 완료 — memberId: {}, totalCount: {}, aiGenerated: {}",
                memberId, totalCount, isAiGenerated);

        InsightsResponse response = InsightsResponse.builder()
                .hasEnoughData(true)
                .currentRecordCount((int) totalCount)
                .requiredRecordCount(INSIGHT_MIN_RECORDS)
                .message(null)
                .topCategories(topCategories)
                .weeklyPattern(weeklyPattern)
                .satisfactionTrend(satisfactionTrend)
                .weeklySummary(weeklySummary)
                .milestone(milestone)
                .mealBalance(mealBalance)
                .satisfactionAnalysis(satisfactionAnalysis)
                .isAiGenerated(isAiGenerated)
                .build();

        // Redis 캐시 저장
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(cacheKey, json, INSIGHT_CACHE_TTL);
        } catch (Exception e) {
            log.warn("인사이트 캐시 저장 실패: {}", e.getMessage());
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // AI Pipeline 호출
    // -----------------------------------------------------------------------

    private AiInsightResponse callAiPipelineForInsights(
            String memberId,
            List<MealRecordEntity> recentMeals,
            Map<String, Long> categoryCount,
            long mealTotal) {

        // 피드백 매핑
        Map<String, FeedbackEntity> feedbackMap = feedbackRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .collect(Collectors.toMap(FeedbackEntity::getMealId, fb -> fb,
                        (existing, replacement) -> existing));

        // 식사 데이터 조립
        List<AiInsightRequest.MealData> mealDataList = recentMeals.stream()
                .map(meal -> {
                    FeedbackEntity fb = feedbackMap.get(meal.getMealId());
                    return AiInsightRequest.MealData.builder()
                            .date(meal.getRecordedAt().toLocalDate().toString())
                            .restaurantName(meal.getRestaurantName())
                            .menuName(meal.getMenuName())
                            .category(meal.getCategory())
                            .satisfaction(fb != null ? fb.getSatisfaction() : null)
                            .keyword(fb != null ? fb.getKeyword() : null)
                            .build();
                })
                .toList();

        // 카테고리 분포 비율
        Map<String, Double> distribution = new LinkedHashMap<>();
        categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> distribution.put(e.getKey(),
                        mealTotal > 0 ? Math.round((e.getValue() * 1000.0 / mealTotal)) / 1000.0 : 0.0));

        AiInsightRequest request = AiInsightRequest.builder()
                .memberId(memberId)
                .recentMeals(mealDataList)
                .categoryDistribution(distribution)
                .totalMealCount((int) mealTotal)
                .periodDays(30)
                .build();

        return aiPipelineClient.getInsightAnalysis(request);
    }

    // -----------------------------------------------------------------------
    // SQL 집계 헬퍼
    // -----------------------------------------------------------------------

    private List<InsightsResponse.WeeklyPatternDto> buildWeeklyPattern(String memberId, LocalDateTime from) {
        try {
            List<Object[]> rows = mealRecordRepository.findWeeklyCategoryDistribution(memberId, from);
            // 요일별 최다 카테고리 추출 (첫 번째 행이 cnt DESC 정렬이므로 요일별 첫 행만 사용)
            Map<Integer, InsightsResponse.WeeklyPatternDto> patternMap = new LinkedHashMap<>();
            for (Object[] row : rows) {
                int dow = ((Number) row[0]).intValue();
                String category = (String) row[1];
                if (!patternMap.containsKey(dow)) {
                    String dayName = (dow >= 0 && dow < DOW_NAMES.length) ? DOW_NAMES[dow] : "UNKNOWN";
                    patternMap.put(dow, InsightsResponse.WeeklyPatternDto.builder()
                            .dayOfWeek(dayName)
                            .topCategory(category)
                            .averageSatisfaction(0.0)
                            .build());
                }
            }
            return List.copyOf(patternMap.values());
        } catch (Exception e) {
            log.warn("weeklyPattern SQL 집계 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<InsightsResponse.SatisfactionTrendDto> buildSatisfactionTrend(String memberId, LocalDateTime from) {
        try {
            List<Object[]> rows = feedbackRepository.findWeeklySatisfactionTrend(memberId, from);
            return rows.stream()
                    .map(row -> {
                        String week = (String) row[0];
                        long total = ((Number) row[1]).longValue();
                        long goodCount = ((Number) row[2]).longValue();
                        double rate = total > 0 ? Math.round((goodCount * 1000.0 / total)) / 10.0 : 0.0;
                        return InsightsResponse.SatisfactionTrendDto.builder()
                                .week(week)
                                .satisfactionRate(rate)
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("satisfactionTrend SQL 집계 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
