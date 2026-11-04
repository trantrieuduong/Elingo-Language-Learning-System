package com.elingo.premium.dto.response;

import com.elingo.premium.entity.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long id,
        String providerRefCode,
        String planName,
        BigDecimal amount,
        String currency,
        PaymentTransactionStatus status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}

