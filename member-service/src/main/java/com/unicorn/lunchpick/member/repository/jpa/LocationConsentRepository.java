package com.unicorn.lunchpick.member.repository.jpa;

import com.unicorn.lunchpick.member.repository.entity.LocationConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 위치 동의 이력 JPA 레포지토리
 *
 * <p>회원의 위치 동의 이력 조회 메서드를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see LocationConsentEntity
 */
public interface LocationConsentRepository extends JpaRepository<LocationConsentEntity, Long> {

    /**
     * 회원 식별자로 최신 위치 동의 이력 조회
     *
     * @param memberId 회원 식별자 (UUID 문자열)
     * @return 위치 동의 이력 목록 (최신순)
     */
    List<LocationConsentEntity> findByMemberIdOrderByCreatedAtDesc(String memberId);

    /**
     * 회원의 가장 최근 위치 동의 이력 단건 조회
     *
     * @param memberId 회원 식별자
     * @return 최신 위치 동의 이력 Optional
     */
    Optional<LocationConsentEntity> findTopByMemberIdOrderByCreatedAtDesc(String memberId);
}
