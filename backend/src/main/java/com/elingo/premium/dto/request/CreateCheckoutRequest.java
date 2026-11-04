package com.elingo.premium.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCheckoutRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long planId
) {
}
