package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 식사 기록 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface MealRecordRepository extends JpaRepository<MealRecordEntity, Long> {

    Optional<MealRecordEntity> findByMealId(String mealId);

    Optional<MealRecordEntity> findByMealIdAndMemberId(String mealId, String memberId);

    List<MealRecordEntity> findByMemberIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            String memberId, LocalDateTime from, LocalDateTime to);

    boolean existsByMemberIdAndRecordedAtBetween(
            String memberId, LocalDateTime from, LocalDateTime to);

    long countByMemberId(String memberId);

    /**
     * 요일별 카테고리 분포 집계 (PostgreSQL 네이티브 쿼리)
     *
     * <p>EXTRACT(DOW FROM ...) 반환값: 0=일, 1=월, ..., 6=토</p>
     */
    @Query(value = """
            SELECT EXTRACT(DOW FROM m.recorded_at) AS day_of_week,
                   m.category AS category,
                   COUNT(*) AS cnt
            FROM meal_record m
            WHERE m.member_id = :memberId
              AND m.recorded_at >= :fromDate
            GROUP BY EXTRACT(DOW FROM m.recorded_at), m.category
            ORDER BY day_of_week, cnt DESC
            """, nativeQuery = true)
    List<Object[]> findWeeklyCategoryDistribution(
            @Param("memberId") String memberId,
            @Param("fromDate") LocalDateTime fromDate);
}
