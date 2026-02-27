package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
