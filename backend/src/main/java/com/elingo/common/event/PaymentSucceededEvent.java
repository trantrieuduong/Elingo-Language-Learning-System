package com.elingo.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSucceededEvent(
        Long transactionId,
        Long userId,
        Long planId,
        BigDecimal amount,
        LocalDateTime paidAt
) {}

