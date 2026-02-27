package com.unicorn.lunchpick.payment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 결제 수단 DTO
 *
 * @param type           결제 수단 유형 (CREDIT_CARD / DEBIT_CARD)
 * @param cardNumber     카드 번호 (16자리, 1234-5678-9012-3456 형식)
 * @param expiryMonth    카드 유효기간 월 (1~12)
 * @param expiryYear     카드 유효기간 연도
 * @param cvc            CVC 코드 (3~4자리)
 * @param cardholderName 카드 소유자 이름 (선택)
 */
public record PaymentMethodDto(

        @NotBlank(message = "결제 수단 유형은 필수입니다.")
        String type,

        @NotBlank(message = "카드 번호는 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$", message = "카드 번호 형식이 올바르지 않습니다.")
        String cardNumber,

        @NotNull(message = "카드 유효기간 월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        Integer expiryMonth,

        @NotNull(message = "카드 유효기간 연도는 필수입니다.")
        Integer expiryYear,

        @NotBlank(message = "CVC는 필수입니다.")
        @Pattern(regexp = "^\\d{3,4}$", message = "CVC는 3~4자리 숫자여야 합니다.")
        String cvc,

        String cardholderName
) {
}
