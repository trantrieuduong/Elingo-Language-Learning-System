package com.elingo.premium.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckoutResponse(
        Long transactionId,
        String providerRefCode,
        BigDecimal amount,
        String currency,
        String bankName,
        String bankAccount,
        String bankAccountName,
        String qrCodeUrl,
        LocalDateTime createdAt
) {
}

