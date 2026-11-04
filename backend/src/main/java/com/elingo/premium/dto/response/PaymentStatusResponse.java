package com.elingo.premium.dto.response;

import com.elingo.premium.entity.PaymentTransactionStatus;

import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long transactionId,
        String providerRefCode,
        PaymentTransactionStatus status,
        boolean isSuccess,
        LocalDateTime paidAt,
        String planName
) {
}

