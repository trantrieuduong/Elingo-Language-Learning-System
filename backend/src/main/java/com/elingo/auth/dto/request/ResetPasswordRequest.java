package com.elingo.auth.dto.request;

import com.elingo.auth.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @Email(message = "EMAIL_INVALID")
        String email,

        @NotBlank(message = "INVALID_REQUEST")
        String otp,

        @ValidPassword
        String newPassword,

        @ValidPassword
        String confirmPassword
) {
}
