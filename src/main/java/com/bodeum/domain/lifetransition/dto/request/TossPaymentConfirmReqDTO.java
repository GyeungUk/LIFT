package com.bodeum.domain.lifetransition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 토스페이먼츠 결제 성공 콜백에서 서버 승인 API로 전달하는 값.
 */
public record TossPaymentConfirmReqDTO(
        @NotBlank
        String paymentKey,

        @NotBlank
        String orderId,

        @NotNull
        @Positive
        Integer amount
) {
}
