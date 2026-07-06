package com.lift.domain.lifetransition.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmation(
        String paymentKey,
        String orderId,
        Integer totalAmount,
        String method,
        String status
) {
}
