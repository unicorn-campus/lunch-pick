package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 피드백 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    Optional<FeedbackEntity> findByMealId(String mealId);

    boolean existsByMealId(String mealId);

    List<FeedbackEntity> findByMemberIdOrderByCreatedAtDesc(String memberId);

    List<FeedbackEntity> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String memberId, LocalDateTime from, LocalDateTime to);

    long countByMemberId(String memberId);

    /**
     * 주간별 만족 비율 집계 (PostgreSQL 네이티브 쿼리)
     *
     * <p>ISO 주차(IYYY-IW) 기준으로 그룹화합니다.</p>
     */
    @Query(value = """
            SELECT to_char(f.created_at, 'IYYY-IW') AS year_week,
                   COUNT(*) AS total,
                   SUM(CASE WHEN f.satisfaction = 'GOOD' THEN 1 ELSE 0 END) AS good_count
            FROM lunchpick_recommendation.feedback f
            WHERE f.member_id = :memberId
              AND f.created_at >= :fromDate
            GROUP BY to_char(f.created_at, 'IYYY-IW')
            ORDER BY year_week
            """, nativeQuery = true)
    List<Object[]> findWeeklySatisfactionTrend(
            @Param("memberId") String memberId,
            @Param("fromDate") LocalDateTime fromDate);
}
