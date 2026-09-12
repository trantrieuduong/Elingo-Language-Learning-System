package com.elingo.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank(message = "EMAIL_INVALID")
        @Email(message = "EMAIL_INVALID")
        String newEmail,

        @NotBlank(message = "INVALID_REQUEST")
        String newEmailOtp,

        @NotBlank(message = "INVALID_REQUEST")
        String password
) {
}
