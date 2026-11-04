package com.elingo.premium.dto.response;

import com.elingo.premium.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record MySubscriptionResponse(
        String planName,
        SubscriptionStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long remainingDays
) {
}

