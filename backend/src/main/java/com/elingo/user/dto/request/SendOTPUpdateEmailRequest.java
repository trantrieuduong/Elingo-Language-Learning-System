package com.elingo.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOTPUpdateEmailRequest(
        @NotBlank(message = "EMAIL_INVALID")
        @Email(message = "EMAIL_INVALID")
        String newEmail
) {
}
