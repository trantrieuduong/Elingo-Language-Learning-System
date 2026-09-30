package com.elingo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyAccountRequest(
        @NotBlank(message = "IDENTIFIER_INVALID")
        String email,

        @NotBlank(message = "OTP_INVALID")
        String otp
) { }
