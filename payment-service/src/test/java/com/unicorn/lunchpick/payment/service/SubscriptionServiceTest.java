package com.unicorn.lunchpick.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unicorn.lunchpick.payment.dto.request.CancelSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.request.CreateSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.request.PaymentMethodDto;
import com.unicorn.lunchpick.payment.dto.response.CancelSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.CreateSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.ExtendTrialResponse;
import com.unicorn.lunchpick.payment.dto.response.SubscriptionPlansResponse;
import com.unicorn.lunchpick.payment.exception.PaymentException;
import com.unicorn.lunchpick.payment.messaging.SubscriptionEventPublisher;
import com.unicorn.lunchpick.payment.pg.PgGateway;
import com.unicorn.lunchpick.payment.repository.entity.SubscriptionEntity;
import com.unicorn.lunchpick.payment.repository.jpa.PaymentHistoryRepository;
import com.unicorn.lunchpick.payment.repository.jpa.SubscriptionRepository;
import com.unicorn.lunchpick.payment.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * SubscriptionServiceImpl 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = createObjectMapper();

    @Mock
    private SubscriptionEventPublisher eventPublisher;

    @Mock
    private PgGateway pgGateway;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private static final String MEMBER_ID = "member-001";
    private static final String SUBSCRIPTION_ID = "sub-001";

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    // -------------------------------------------------------------------------
    // getSubscriptionPlans
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 플랜 조회 — 캐시 미스 시 정적 플랜 3개 반환")
    void getSubscriptionPlans_cacheMiss_returnsThreePlans() {
        // Given
        given(valueOperations.get("plan:list")).willReturn(null);
        given(subscriptionRepository.findByMemberIdAndStatus(MEMBER_ID, "ACTIVE"))
                .willReturn(Optional.empty());

        // When
        SubscriptionPlansResponse response = subscriptionService.getSubscriptionPlans(MEMBER_ID);

        // Then
        assertThat(response.plans()).hasSize(3);
        assertThat(response.currentPlan()).isEqualTo("FREE");
        assertThat(response.plans()).extracting("planId")
                .containsExactly("FREE", "PREMIUM_MONTHLY", "PREMIUM_ANNUAL");
    }

    @Test
    @DisplayName("구독 플랜 조회 — 활성 구독 있으면 currentPlan = PREMIUM_MONTHLY")
    void getSubscriptionPlans_activeSubscription_returnsCurrentPlan() {
        // Given
        given(valueOperations.get("plan:list")).willReturn(null);
        SubscriptionEntity activeSubscription = SubscriptionEntity.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .memberId(MEMBER_ID)
                .planId("PREMIUM_MONTHLY")
                .status("ACTIVE")
                .startedAt(LocalDateTime.now())
                .build();
        given(subscriptionRepository.findByMemberIdAndStatus(MEMBER_ID, "ACTIVE"))
                .willReturn(Optional.of(activeSubscription));

        // When
        SubscriptionPlansResponse response = subscriptionService.getSubscriptionPlans(MEMBER_ID);

        // Then
        assertThat(response.currentPlan()).isEqualTo("PREMIUM_MONTHLY");
    }

    // -------------------------------------------------------------------------
    // createSubscription
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 결제 — 정상 결제 시 ACTIVE 구독 생성")
    void createSubscription_success_returnsActiveSubscription() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "1234-5678-9012-3456", 12, 2028, "123", "김성한"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(false);
        given(pgGateway.approve(any(), any(Integer.class), anyString())).willReturn("pg-txn-abcd1234");
        given(paymentHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When
        CreateSubscriptionResponse response = subscriptionService.createSubscription(MEMBER_ID, request);

        // Then
        assertThat(response.planId()).isEqualTo("PREMIUM_MONTHLY");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.amount()).isEqualTo(4900);
        assertThat(response.message()).contains("프리미엄이 활성화");
        assertThat(response.withdrawalDeadline()).isNotNull();
        then(paymentHistoryRepository).should().save(any());
        then(subscriptionRepository).should().save(any());
        then(eventPublisher).should().publishActivated(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("구독 결제 — 유효하지 않은 카드(0000-0000-0000-0000) 결제 시 INVALID_PAYMENT_INFO 예외")
    void createSubscription_invalidCardNumber_throwsException() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "0000-0000-0000-0000", 12, 2028, "123", "테스트"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(false);
        given(pgGateway.approve(any(), any(Integer.class), anyString()))
                .willThrow(PaymentException.invalidPaymentInfo());
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(MEMBER_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("카드 정보를 다시 확인");
        then(paymentHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("구독 결제 — 만료 카드(expiryYear < 현재) 결제 시 INVALID_PAYMENT_INFO 예외")
    void createSubscription_expiredCard_throwsException() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "1234-5678-9012-3456", 1, 2020, "123", "테스트"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(false);
        given(pgGateway.approve(any(), any(Integer.class), anyString()))
                .willThrow(PaymentException.invalidPaymentInfo());
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(MEMBER_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("카드 정보를 다시 확인");
        then(paymentHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("구독 결제 — 결제 거절 카드(4000-0000-0000-0002) 결제 시 PAYMENT_FAILED 예외")
    void createSubscription_declinedCard_throwsException() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "4000-0000-0000-0002", 12, 2028, "123", "테스트"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(false);
        given(pgGateway.approve(any(), any(Integer.class), anyString()))
                .willThrow(PaymentException.paymentFailed());
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(MEMBER_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("결제가 실패했어요");
        then(paymentHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("구독 결제 — 이미 활성 구독 존재 시 SUBSCRIPTION_ALREADY_ACTIVE 예외")
    void createSubscription_alreadyActive_throwsException() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "1234-5678-9012-3456", 12, 2028, "123", "김성한"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(true);
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(MEMBER_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("이미 프리미엄 구독이 활성화");
        then(paymentHistoryRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("구독 결제 — Lock 획득 실패 시 DUPLICATE_PAYMENT_LOCK 예외")
    void createSubscription_lockFailed_throwsException() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_MONTHLY",
                new PaymentMethodDto("CREDIT_CARD", "1234-5678-9012-3456", 12, 2028, "123", "김성한"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(MEMBER_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("결제가 이미 진행 중");
        then(subscriptionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("구독 결제 — PREMIUM_ANNUAL 플랜 결제 시 금액 46800원")
    void createSubscription_annualPlan_correctAmount() {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "PREMIUM_ANNUAL",
                new PaymentMethodDto("CREDIT_CARD", "1234-5678-9012-3456", 12, 2028, "123", "김성한"),
                true,
                true
        );
        given(valueOperations.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
        given(subscriptionRepository.existsByMemberIdAndStatus(MEMBER_ID, "ACTIVE")).willReturn(false);
        given(pgGateway.approve(any(), any(Integer.class), anyString())).willReturn("pg-txn-efgh5678");
        given(paymentHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When
        CreateSubscriptionResponse response = subscriptionService.createSubscription(MEMBER_ID, request);

        // Then
        assertThat(response.amount()).isEqualTo(46800);
        assertThat(response.planId()).isEqualTo("PREMIUM_ANNUAL");
    }

    // -------------------------------------------------------------------------
    // cancelSubscription
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 해지 — PENDING_CANCEL 상태로 전환")
    void cancelSubscription_success_returnsPendingCancel() {
        // Given
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .memberId(MEMBER_ID)
                .planId("PREMIUM_MONTHLY")
                .status("ACTIVE")
                .startedAt(LocalDateTime.now())
                .currentPeriodEndsAt(LocalDateTime.now().plusMonths(1))
                .build();
        given(subscriptionRepository.findBySubscriptionIdAndMemberId(SUBSCRIPTION_ID, MEMBER_ID))
                .willReturn(Optional.of(subscription));
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        CancelSubscriptionRequest request = new CancelSubscriptionRequest("COST", "가격이 부담돼요.");

        // When
        CancelSubscriptionResponse response = subscriptionService
                .cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID, request);

        // Then
        assertThat(response.status()).isEqualTo("PENDING_CANCEL");
        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.dataWarningMessage()).contains("30일 이전 기록");
        then(eventPublisher).should().publishPendingCancel(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("구독 해지 — 구독 없으면 SUBSCRIPTION_NOT_FOUND 예외")
    void cancelSubscription_notFound_throwsException() {
        // Given
        given(subscriptionRepository.findBySubscriptionIdAndMemberId(SUBSCRIPTION_ID, MEMBER_ID))
                .willReturn(Optional.empty());

        CancelSubscriptionRequest request = new CancelSubscriptionRequest("COST", null);

        // When & Then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(MEMBER_ID, SUBSCRIPTION_ID, request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("활성 구독 정보를 찾을 수 없습니다");
    }

    // -------------------------------------------------------------------------
    // extendTrial
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("무료 연장 — 1회 한정 7일 연장 성공")
    void extendTrial_success_extends7Days() {
        // Given
        LocalDateTime currentEnd = LocalDateTime.now().plusMonths(1);
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .memberId(MEMBER_ID)
                .planId("PREMIUM_MONTHLY")
                .status("ACTIVE")
                .startedAt(LocalDateTime.now())
                .currentPeriodEndsAt(currentEnd)
                .trialExtensionUsed(false)
                .build();
        given(subscriptionRepository.findByMemberIdAndStatus(MEMBER_ID, "ACTIVE"))
                .willReturn(Optional.of(subscription));
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // When
        ExtendTrialResponse response = subscriptionService.extendTrial(MEMBER_ID);

        // Then
        assertThat(response.message()).contains("7일이 연장");
        assertThat(response.newExpiresAt()).isEqualTo(currentEnd.plusDays(7));
        then(eventPublisher).should().publishTrialExtended(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("무료 연장 — 이미 사용한 경우 TRIAL_EXTENSION_ALREADY_USED 예외")
    void extendTrial_alreadyUsed_throwsException() {
        // Given
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .memberId(MEMBER_ID)
                .planId("PREMIUM_MONTHLY")
                .status("ACTIVE")
                .startedAt(LocalDateTime.now())
                .trialExtensionUsed(true)
                .build();
        given(subscriptionRepository.findByMemberIdAndStatus(MEMBER_ID, "ACTIVE"))
                .willReturn(Optional.of(subscription));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.extendTrial(MEMBER_ID))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("1회만 사용 가능");
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
