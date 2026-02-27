package com.unicorn.lunchpick.member.repository.jpa;

import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 JPA 레포지토리
 *
 * <p>회원 엔티티에 대한 CRUD 및 도메인 조회 메서드를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see MemberEntity
 */
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    /**
     * 도메인 식별자(memberId)로 회원 조회
     *
     * @param memberId 도메인 식별자 (UUID 문자열)
     * @return 회원 엔티티 Optional
     */
    Optional<MemberEntity> findByMemberId(String memberId);

    /**
     * 카카오 ID로 회원 조회
     *
     * @param kakaoId 카카오 OAuth ID
     * @return 회원 엔티티 Optional
     */
    Optional<MemberEntity> findByKakaoId(String kakaoId);

    /**
     * 이메일로 회원 조회
     *
     * @param email 이메일 주소
     * @return 회원 엔티티 Optional
     */
    Optional<MemberEntity> findByEmail(String email);

    /**
     * 카카오 ID 존재 여부 확인
     *
     * @param kakaoId 카카오 OAuth ID
     * @return 존재하면 true
     */
    boolean existsByKakaoId(String kakaoId);

    /**
     * memberId 존재 여부 확인
     *
     * @param memberId 도메인 식별자 (UUID 문자열)
     * @return 존재하면 true
     */
    boolean existsByMemberId(String memberId);

    /**
     * 닉네임으로 회원 조회
     *
     * @param nickname 닉네임
     * @return 회원 엔티티 Optional
     */
    Optional<MemberEntity> findByNickname(String nickname);
}
