package com.unicorn.lunchpick.member.repository.jpa;

import com.unicorn.lunchpick.member.repository.entity.TasteProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 취향 프로파일 JPA 레포지토리
 *
 * <p>회원의 취향 벡터 및 피드백 수 관리 메서드를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see TasteProfileEntity
 */
public interface TasteProfileRepository extends JpaRepository<TasteProfileEntity, Long> {

    /**
     * 회원 식별자로 취향 프로파일 조회
     *
     * @param memberId 회원 식별자 (UUID 문자열)
     * @return 취향 프로파일 Optional
     */
    Optional<TasteProfileEntity> findByMemberId(String memberId);

    /**
     * 회원 식별자의 취향 프로파일 존재 여부 확인
     *
     * @param memberId 회원 식별자
     * @return 존재하면 true
     */
    boolean existsByMemberId(String memberId);
}
