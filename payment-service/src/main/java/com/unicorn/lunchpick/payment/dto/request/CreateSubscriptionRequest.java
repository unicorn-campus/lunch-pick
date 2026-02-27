package com.unicorn.lunchpick.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 구독 결제 요청 DTO
 *
 * <p>전자상거래법 준수: autoRenewalAgreed, withdrawalRightAcknowledged 모두 true 필수.</p>
 *
 * @param planId                       구독 플랜 (PREMIUM_MONTHLY / PREMIUM_ANNUAL)
 * @param paymentMethod                결제 수단
 * @param autoRenewalAgreed            자동 갱신 동의
 * @param withdrawalRightAcknowledged  청약철회권 7일 고지 확인
 */
public record CreateSubscriptionRequest(

        @NotBlank(message = "구독 플랜은 필수입니다.")
        String planId,

        @NotNull(message = "결제 수단은 필수입니다.")
        @Valid
        PaymentMethodDto paymentMethod,

        @NotNull(message = "자동 갱신 동의는 필수입니다.")
        Boolean autoRenewalAgreed,

        @NotNull(message = "청약철회권 고지 확인은 필수입니다.")
        Boolean withdrawalRightAcknowledged
) {

        @AssertTrue(message = "자동 갱신에 동의해야 구독이 가능합니다.")
        public boolean isAutoRenewalAgreedValid() {
                return autoRenewalAgreed != null && autoRenewalAgreed;
        }

        @AssertTrue(message = "청약철회권 고지를 확인해야 구독이 가능합니다.")
        public boolean isWithdrawalRightAcknowledgedValid() {
                return withdrawalRightAcknowledged != null && withdrawalRightAcknowledged;
        }
}
