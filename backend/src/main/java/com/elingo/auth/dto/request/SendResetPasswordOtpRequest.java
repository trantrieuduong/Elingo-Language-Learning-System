package com.elingo.auth.dto.request;

import jakarta.validation.constraints.Email;

public record SendResetPasswordOtpRequest(
        @Email(message = "EMAIL_INVALID")
        String email
) {
}
