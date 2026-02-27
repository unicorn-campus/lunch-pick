package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.MealRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
