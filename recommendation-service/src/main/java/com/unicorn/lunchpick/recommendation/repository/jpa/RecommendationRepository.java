package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.RecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 추천 결과 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface RecommendationRepository extends JpaRepository<RecommendationEntity, Long> {

    Optional<RecommendationEntity> findByRecommendationId(String recommendationId);

    List<RecommendationEntity> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String memberId, LocalDateTime from, LocalDateTime to);

    List<RecommendationEntity> findByMemberIdAndStatusOrderByCreatedAtDesc(
            String memberId, String status);

    boolean existsByRecommendationId(String recommendationId);
}
