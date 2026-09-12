package com.elingo.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendResetPasswordOtpRequest(
        @NotBlank(message = "EMAIL_INVALID")
        @Email(message = "EMAIL_INVALID")
        String email
) {
}
