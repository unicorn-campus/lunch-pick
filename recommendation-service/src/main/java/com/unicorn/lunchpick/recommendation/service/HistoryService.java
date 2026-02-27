package com.unicorn.lunchpick.recommendation.service;

import com.unicorn.lunchpick.recommendation.dto.response.InsightsResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealHistoryResponse;

import java.time.LocalDate;

/**
 * 식사 이력 및 취향 인사이트 서비스 인터페이스
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface HistoryService {

    /**
     * 식사 이력 타임라인 조회
     *
     * <p>FREE 회원은 최근 30일로 제한됩니다. 30일 초과 시 {@code PREMIUM_REQUIRED} 예외.</p>
     *
     * @param memberId  회원 식별자
     * @param startDate 조회 시작일 (null이면 30일 전)
     * @param endDate   조회 종료일 (null이면 오늘)
     * @param isPremium 프리미엄 여부
     * @return 식사 이력 타임라인 응답
     */
    MealHistoryResponse getMealHistoryTimeline(String memberId, LocalDate startDate,
                                               LocalDate endDate, boolean isPremium);

    /**
     * 취향 인사이트 리포트 조회
     *
     * <p>10건 미만 시 기록 독려 메시지 반환, 30끼 달성 시 마일스톤 배지 포함.</p>
     *
     * @param memberId 회원 식별자
     * @return 취향 인사이트 응답
     */
    InsightsResponse getTasteInsights(String memberId);
}
