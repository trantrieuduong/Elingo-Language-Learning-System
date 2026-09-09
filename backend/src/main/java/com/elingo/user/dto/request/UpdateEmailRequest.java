package com.elingo.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Email(message = "EMAIL_INVALID")
        String email,

        @NotBlank(message = "INVALID_REQUEST")
        String newEmailOtp,

        @NotBlank(message = "INVALID_REQUEST")
        String oldEmailOtp
) {
}
