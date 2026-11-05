package com.elingo.premium.gateway;

import java.math.BigDecimal;

public record WebhookEvent(
        String eventId, // Id event của provider.
        String refCode,
        BigDecimal amount,
        boolean success,
        String rawPayload
) {}

