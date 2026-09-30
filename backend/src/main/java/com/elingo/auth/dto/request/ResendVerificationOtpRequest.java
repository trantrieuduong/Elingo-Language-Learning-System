package com.elingo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResendVerificationOtpRequest(
        @NotBlank(message = "IDENTIFIER_INVALID")
        String email
) { }
