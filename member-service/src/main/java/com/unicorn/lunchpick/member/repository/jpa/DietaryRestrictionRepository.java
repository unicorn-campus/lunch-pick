package com.unicorn.lunchpick.member.repository.jpa;

import com.unicorn.lunchpick.member.repository.entity.DietaryRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 식이 제한 JPA 레포지토리
 *
 * <p>회원의 알레르기 및 식이 유형 관리 메서드를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see DietaryRestrictionEntity
 */
public interface DietaryRestrictionRepository extends JpaRepository<DietaryRestrictionEntity, Long> {

    /**
     * 회원 식별자로 식이 제한 정보 조회
     *
     * @param memberId 회원 식별자 (UUID 문자열)
     * @return 식이 제한 정보 Optional
     */
    Optional<DietaryRestrictionEntity> findByMemberId(String memberId);

    /**
     * 회원 식별자의 식이 제한 정보 존재 여부 확인
     *
     * @param memberId 회원 식별자
     * @return 존재하면 true
     */
    boolean existsByMemberId(String memberId);
}
