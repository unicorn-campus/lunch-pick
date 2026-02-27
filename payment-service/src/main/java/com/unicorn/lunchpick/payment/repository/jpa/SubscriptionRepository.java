package com.unicorn.lunchpick.payment.repository.jpa;

import com.unicorn.lunchpick.payment.repository.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 구독 정보 JPA 레포지토리
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /**
     * 구독 ID로 구독 정보 조회
     *
     * @param subscriptionId 구독 도메인 식별자
     * @return 구독 정보
     */
    Optional<SubscriptionEntity> findBySubscriptionId(String subscriptionId);

    /**
     * 회원의 활성 구독 조회
     *
     * @param memberId 회원 식별자
     * @param status   구독 상태
     * @return 활성 구독 정보
     */
    Optional<SubscriptionEntity> findByMemberIdAndStatus(String memberId, String status);

    /**
     * 구독 ID와 회원 ID로 구독 정보 조회 (소유권 검증)
     *
     * @param subscriptionId 구독 도메인 식별자
     * @param memberId       회원 식별자
     * @return 구독 정보
     */
    java.util.Optional<SubscriptionEntity> findBySubscriptionIdAndMemberId(String subscriptionId, String memberId);

    /**
     * 회원의 특정 상태 구독 존재 여부 확인
     *
     * @param memberId 회원 식별자
     * @param status   구독 상태
     * @return 존재 여부
     */
    boolean existsByMemberIdAndStatus(String memberId, String status);
}
