package com.elingo.premium.dto;

import com.elingo.premium.entity.WebhookLogStatus;

public record PaymentProcessingResult(
        Long transactionId,
        WebhookLogStatus status,
        String message
) {}

