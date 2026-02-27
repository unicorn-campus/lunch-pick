package com.unicorn.lunchpick.recommendation.repository.jpa;

import com.unicorn.lunchpick.recommendation.repository.entity.PreferenceVectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 취향 벡터 스냅샷 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface PreferenceVectorRepository extends JpaRepository<PreferenceVectorEntity, Long> {

    Optional<PreferenceVectorEntity> findTopByMemberIdOrderByCalculatedAtDesc(String memberId);
}
