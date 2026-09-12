package com.elingo.auth.dto.request;

import com.elingo.auth.annotation.ValidPassword;
import com.elingo.auth.annotation.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @ValidUsername
        String username,

        @NotBlank(message = "EMAIL_INVALID")
        @Email(message = "EMAIL_INVALID")
        String email,

        @NotBlank(message = "FULL_NAME_INVALID")
        String fullName,

        @ValidPassword
        String password
) { }
