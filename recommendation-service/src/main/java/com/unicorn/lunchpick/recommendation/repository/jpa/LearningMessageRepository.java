package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.LearningMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 학습 완료 메시지 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface LearningMessageRepository extends JpaRepository<LearningMessageEntity, Long> {

    Optional<LearningMessageEntity> findTopByMemberIdOrderByGeneratedAtDesc(String memberId);
}
