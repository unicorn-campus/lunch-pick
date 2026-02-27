package com.unicorn.lunchpick.payment.service;

import com.unicorn.lunchpick.payment.dto.request.CancelSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.request.CreateSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.response.CancelSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.CreateSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.ExtendTrialResponse;
import com.unicorn.lunchpick.payment.dto.response.SubscriptionPlansResponse;

/**
 * 구독 서비스 인터페이스
 *
 * <p>구독 플랜 조회, 구독 결제, 구독 해지, 7일 무료 연장 기능을 정의합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface SubscriptionService {

    /**
     * 구독 플랜 목록 조회
     *
     * <p>Redis 캐시({@code plan:list}, TTL 1시간)를 우선 조회합니다.
     * 캐시 미스 시 정적 플랜 정보를 반환하고 캐싱합니다.</p>
     *
     * @param memberId 회원 식별자
     * @return 구독 플랜 목록 및 현재 플랜 정보
     */
    SubscriptionPlansResponse getSubscriptionPlans(String memberId);

    /**
     * 구독 결제 처리
     *
     * <p>중복 결제 방지 Redis Lock 획득 후 PG 결제를 진행합니다.
     * 이중결제 방지를 위해 PG 결제에 Retry를 적용하지 않습니다.
     * 결제 성공 시 subscription-events 토픽에 이벤트를 발행합니다.</p>
     *
     * @param memberId 회원 식별자
     * @param request  구독 결제 요청
     * @return 구독 결제 응답
     */
    CreateSubscriptionResponse createSubscription(String memberId, CreateSubscriptionRequest request);

    /**
     * 구독 해지 예약
     *
     * <p>즉시 해지가 아닌 PENDING_CANCEL 상태로 전환합니다.
     * 현재 기간 종료 후 무료 전환됩니다.
     * 해지 예약 이벤트를 subscription-events 토픽에 발행합니다.</p>
     *
     * @param memberId       회원 식별자
     * @param subscriptionId 구독 도메인 식별자
     * @param request        해지 요청
     * @return 해지 예약 응답
     */
    CancelSubscriptionResponse cancelSubscription(String memberId, String subscriptionId,
                                                   CancelSubscriptionRequest request);

    /**
     * 7일 무료 연장 (해지 전 복귀 유도, 회원당 1회 한정)
     *
     * <p>trialExtensionUsed=false인 경우에만 연장 가능합니다.
     * 연장 이력을 결제 DB에 기록하고, 구독 연장 이벤트를 발행합니다.</p>
     *
     * @param memberId 회원 식별자
     * @return 연장 응답
     */
    ExtendTrialResponse extendTrial(String memberId);
}
