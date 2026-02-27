package com.unicorn.lunchpick.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.payment.dto.request.CancelSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.request.CreateSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.response.CancelSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.CreateSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.ExtendTrialResponse;
import com.unicorn.lunchpick.payment.dto.response.SubscriptionPlanDto;
import com.unicorn.lunchpick.payment.dto.response.SubscriptionPlansResponse;
import com.unicorn.lunchpick.payment.exception.PaymentException;
import com.unicorn.lunchpick.payment.messaging.SubscriptionEventPublisher;
import com.unicorn.lunchpick.payment.pg.PgGateway;
import com.unicorn.lunchpick.payment.repository.entity.PaymentHistoryEntity;
import com.unicorn.lunchpick.payment.repository.entity.SubscriptionEntity;
import com.unicorn.lunchpick.payment.repository.jpa.PaymentHistoryRepository;
import com.unicorn.lunchpick.payment.repository.jpa.SubscriptionRepository;
import com.unicorn.lunchpick.payment.service.SubscriptionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 구독 서비스 구현체
 *
 * <p>구독 플랜 조회(Redis 캐시), 구독 결제(Resilience4j CB + 중복 결제 Lock),
 * 구독 해지 예약, 7일 무료 연장을 구현합니다.</p>
 *
 * <p><b>Redis 캐시 키 패턴:</b></p>
 * <ul>
 *   <li>{@code plan:list} — 구독 플랜 목록 캐시 (TTL 1시간)</li>
 *   <li>{@code subscription:active:{memberId}} — 활성 구독 캐시 (TTL 10분)</li>
 *   <li>{@code payment:lock:{memberId}} — 중복 결제 방지 Lock (TTL 30초)</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String PLAN_LIST_CACHE_KEY = "plan:list";
    private static final String ACTIVE_SUBSCRIPTION_CACHE_PREFIX = "subscription:active:";
    private static final String PAYMENT_LOCK_PREFIX = "payment:lock:";

    private static final Duration PLAN_CACHE_TTL = Duration.ofHours(1);
    private static final Duration ACTIVE_SUBSCRIPTION_TTL = Duration.ofMinutes(10);
    private static final Duration PAYMENT_LOCK_TTL = Duration.ofSeconds(30);

    private static final int MONTHLY_PRICE = 4900;
    private static final int ANNUAL_PRICE = 46800;
    private static final int TRIAL_EXTENSION_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SubscriptionEventPublisher eventPublisher;
    private final PgGateway pgGateway;

    /**
     * {@inheritDoc}
     *
     * <p>Redis {@code plan:list} 캐시 우선 조회. 캐시 미스 시 정적 플랜 정보 반환 후 캐싱.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlansResponse getSubscriptionPlans(String memberId) {
        String cached = stringRedisTemplate.opsForValue().get(PLAN_LIST_CACHE_KEY);
        if (cached != null) {
            log.debug("구독 플랜 캐시 히트");
            return deserializePlansResponse(cached);
        }

        String currentPlan = resolveCurrentPlan(memberId);
        List<SubscriptionPlanDto> plans = buildStaticPlans();

        SubscriptionPlansResponse response = SubscriptionPlansResponse.builder()
                .plans(plans)
                .currentPlan(currentPlan)
                .promotionMessage(null)
                .build();

        cachePlansResponse(response);
        log.debug("구독 플랜 조회 완료 — memberId: {}, currentPlan: {}", memberId, currentPlan);
        return response;
    }

    /**
     * {@inheritDoc}
     *
     * <p>중복 결제 방지 Lock 획득 → 이미 활성 구독 여부 확인 → PG 결제 호출(CB 보호)
     * → payment_history INSERT → subscription INSERT → 이벤트 발행.</p>
     */
    @Override
    @Transactional
    @CircuitBreaker(name = "pg-gateway", fallbackMethod = "createSubscriptionFallback")
    public CreateSubscriptionResponse createSubscription(String memberId, CreateSubscriptionRequest request) {
        // 중복 결제 방지 Lock
        String lockKey = PAYMENT_LOCK_PREFIX + memberId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", PAYMENT_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw PaymentException.duplicatePaymentLock();
        }

        try {
            // 이미 활성 구독 여부 확인
            if (subscriptionRepository.existsByMemberIdAndStatus(memberId, "ACTIVE")) {
                throw PaymentException.subscriptionAlreadyActive();
            }

            // 플랜별 금액 결정
            int amount = resolveAmount(request.planId());

            // PG 결제 요청 (카드 유효성 검증 포함 — MockPgGateway 또는 실제 PG 구현체)
            String orderId = UUID.randomUUID().toString();
            log.info("PG 결제 요청 — memberId: {}, planId: {}, amount: {}, orderId: {}",
                    memberId, request.planId(), amount, orderId);
            String pgTransactionId = pgGateway.approve(request.paymentMethod(), amount, orderId);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime withdrawalDeadline = now.plusDays(7);
            LocalDateTime nextBillingAt = "PREMIUM_ANNUAL".equals(request.planId())
                    ? now.plusYears(1) : now.plusMonths(1);

            // payment_history INSERT (INSERT ONLY)
            String paymentId = UUID.randomUUID().toString();
            String subscriptionId = UUID.randomUUID().toString();

            PaymentHistoryEntity paymentHistory = PaymentHistoryEntity.builder()
                    .paymentId(paymentId)
                    .memberId(memberId)
                    .subscriptionId(subscriptionId)
                    .planId(request.planId())
                    .amount(amount)
                    .status("SUCCESS")
                    .pgTransactionId(pgTransactionId)
                    .autoRenewalAgreed(request.autoRenewalAgreed())
                    .withdrawalRightAcknowledged(request.withdrawalRightAcknowledged())
                    .withdrawalDeadline(withdrawalDeadline)
                    .requestedAt(now)
                    .approvedAt(now)
                    .build();
            paymentHistoryRepository.save(paymentHistory);

            // subscription INSERT
            SubscriptionEntity subscription = SubscriptionEntity.builder()
                    .subscriptionId(subscriptionId)
                    .memberId(memberId)
                    .planId(request.planId())
                    .status("ACTIVE")
                    .startedAt(now)
                    .nextBillingAt(nextBillingAt)
                    .currentPeriodEndsAt(nextBillingAt)
                    .trialExtensionUsed(false)
                    .build();
            subscriptionRepository.save(subscription);

            // 활성 구독 캐시 무효화
            stringRedisTemplate.delete(ACTIVE_SUBSCRIPTION_CACHE_PREFIX + memberId);

            // 구독 활성화 이벤트 발행
            eventPublisher.publishActivated(memberId, subscriptionId, request.planId(), nextBillingAt);

            log.info("구독 결제 완료 — memberId: {}, subscriptionId: {}, planId: {}",
                    memberId, subscriptionId, request.planId());

            return CreateSubscriptionResponse.builder()
                    .subscriptionId(subscriptionId)
                    .planId(request.planId())
                    .status("ACTIVE")
                    .startedAt(now)
                    .nextBillingAt(nextBillingAt)
                    .amount(amount)
                    .transactionId(pgTransactionId)
                    .message("프리미엄이 활성화되었어요! 이제 모든 기능을 사용할 수 있어요.")
                    .withdrawalDeadline(withdrawalDeadline)
                    .build();
        } finally {
            // Lock 해제
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * PG 게이트웨이 장애 시 폴백
     */
    public CreateSubscriptionResponse createSubscriptionFallback(String memberId,
                                                                   CreateSubscriptionRequest request,
                                                                   Throwable t) {
        log.warn("PG 게이트웨이 장애 — 결제 실패 처리 — memberId: {}, cause: {}", memberId, t.getMessage());
        throw PaymentException.paymentFailed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CancelSubscriptionResponse cancelSubscription(String memberId, String subscriptionId,
                                                          CancelSubscriptionRequest request) {
        SubscriptionEntity subscription = subscriptionRepository
                .findBySubscriptionIdAndMemberId(subscriptionId, memberId)
                .orElseThrow(PaymentException::subscriptionNotFound);

        subscription.cancel(request.cancelReason(), request.cancelReasonDetail());

        // 활성 구독 캐시 무효화
        stringRedisTemplate.delete(ACTIVE_SUBSCRIPTION_CACHE_PREFIX + memberId);

        // 해지 예약 이벤트 발행
        eventPublisher.publishPendingCancel(memberId, subscriptionId,
                subscription.getPlanId(), subscription.getCurrentPeriodEndsAt());

        log.info("구독 해지 예약 완료 — memberId: {}, subscriptionId: {}, reason: {}",
                memberId, subscriptionId, request.cancelReason());

        LocalDateTime currentPeriodEndsAt = subscription.getCurrentPeriodEndsAt();
        LocalDateTime freePlanStartsAt = currentPeriodEndsAt != null
                ? currentPeriodEndsAt.plusDays(1) : LocalDateTime.now();

        return CancelSubscriptionResponse.builder()
                .subscriptionId(subscriptionId)
                .status("PENDING_CANCEL")
                .currentPeriodEndsAt(currentPeriodEndsAt)
                .freePlanStartsAt(freePlanStartsAt)
                .message("해지가 예약되었어요. 기간 만료 시까지 프리미엄을 이용할 수 있어요.")
                .dataWarningMessage("무료 전환 후 30일 이전 기록은 열람할 수 없어요.")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ExtendTrialResponse extendTrial(String memberId) {
        SubscriptionEntity subscription = subscriptionRepository
                .findByMemberIdAndStatus(memberId, "ACTIVE")
                .orElseThrow(PaymentException::subscriptionNotFound);

        if (subscription.isTrialExtensionUsed()) {
            throw PaymentException.trialExtensionAlreadyUsed();
        }

        LocalDateTime newExpiresAt = subscription.getCurrentPeriodEndsAt() != null
                ? subscription.getCurrentPeriodEndsAt().plusDays(TRIAL_EXTENSION_DAYS)
                : LocalDateTime.now().plusDays(TRIAL_EXTENSION_DAYS);

        subscription.extendTrial(newExpiresAt);

        // 활성 구독 캐시 무효화
        stringRedisTemplate.delete(ACTIVE_SUBSCRIPTION_CACHE_PREFIX + memberId);

        // 연장 이벤트 발행
        eventPublisher.publishTrialExtended(memberId, subscription.getSubscriptionId(),
                subscription.getPlanId(), newExpiresAt);

        log.info("7일 무료 연장 완료 — memberId: {}, newExpiresAt: {}", memberId, newExpiresAt);

        return ExtendTrialResponse.builder()
                .message("7일이 연장되었어요. 계속 사용해보세요!")
                .newExpiresAt(newExpiresAt)
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int resolveAmount(String planId) {
        return "PREMIUM_ANNUAL".equals(planId) ? ANNUAL_PRICE : MONTHLY_PRICE;
    }

    private String resolveCurrentPlan(String memberId) {
        return subscriptionRepository.findByMemberIdAndStatus(memberId, "ACTIVE")
                .map(SubscriptionEntity::getPlanId)
                .orElse("FREE");
    }

    private List<SubscriptionPlanDto> buildStaticPlans() {
        return List.of(
                SubscriptionPlanDto.builder()
                        .planId("FREE")
                        .planName("무료")
                        .pricePerMonth(0)
                        .totalPrice(0)
                        .billingCycle("MONTHLY")
                        .discountRate(0.0)
                        .features(List.of("AI 개인화 추천 3개/일", "식사 이력 30일", "기본 인사이트"))
                        .build(),
                SubscriptionPlanDto.builder()
                        .planId("PREMIUM_MONTHLY")
                        .planName("프리미엄 월간")
                        .pricePerMonth(MONTHLY_PRICE)
                        .totalPrice(MONTHLY_PRICE)
                        .billingCycle("MONTHLY")
                        .discountRate(0.0)
                        .features(List.of("AI 개인화 추천 3개/일", "식사 이력 무제한",
                                "상세 취향 인사이트", "추천 이유 상세 확인", "광고 없음"))
                        .build(),
                SubscriptionPlanDto.builder()
                        .planId("PREMIUM_ANNUAL")
                        .planName("프리미엄 연간")
                        .pricePerMonth(3900)
                        .totalPrice(ANNUAL_PRICE)
                        .billingCycle("ANNUAL")
                        .discountRate(20.0)
                        .features(List.of("AI 개인화 추천 3개/일", "식사 이력 무제한",
                                "상세 취향 인사이트", "추천 이유 상세 확인", "광고 없음", "연간 20% 할인"))
                        .build()
        );
    }

    private void cachePlansResponse(SubscriptionPlansResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(PLAN_LIST_CACHE_KEY, json, PLAN_CACHE_TTL);
        } catch (JsonProcessingException ex) {
            log.warn("구독 플랜 캐시 직렬화 실패 — 캐시 스킵", ex);
        }
    }

    private SubscriptionPlansResponse deserializePlansResponse(String json) {
        try {
            return objectMapper.readValue(json, SubscriptionPlansResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("구독 플랜 캐시 역직렬화 실패 — 캐시 스킵", ex);
            return null;
        }
    }
}
