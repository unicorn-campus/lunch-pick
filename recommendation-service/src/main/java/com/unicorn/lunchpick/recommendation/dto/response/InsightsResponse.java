package com.unicorn.lunchpick.recommendation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 취향 인사이트 리포트 응답 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record InsightsResponse(
        boolean hasEnoughData,
        int currentRecordCount,
        int requiredRecordCount,
        String message,
        List<CategoryDistributionDto> topCategories,
        List<WeeklyPatternDto> weeklyPattern,
        List<SatisfactionTrendDto> satisfactionTrend,
        String weeklySummary,
        MilestoneBadgeDto milestone
) {

    /** 카테고리 분포 DTO */
    @Builder
    public record CategoryDistributionDto(
            String category, int count, double percentage, String color) {}

    /** 요일별 패턴 DTO */
    @Builder
    public record WeeklyPatternDto(
            String dayOfWeek, String topCategory, double averageSatisfaction) {}

    /** 주간 만족도 트렌드 DTO */
    @Builder
    public record SatisfactionTrendDto(
            String week, double satisfactionRate) {}

    /** 마일스톤 배지 DTO */
    @Builder
    public record MilestoneBadgeDto(
            boolean achieved, int count, String message, double accuracyImprovement) {}
}
