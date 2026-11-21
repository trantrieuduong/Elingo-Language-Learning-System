package com.elingo.admin.gamification.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateXpPolicyRequest(
        @NotNull(message = "INVALID_REQUEST")
        @PositiveOrZero(message = "INVALID_REQUEST")
        Integer xp,

        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer dailyLimit
) {}
