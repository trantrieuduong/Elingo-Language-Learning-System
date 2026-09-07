package com.elingo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String username, // or email

        @NotBlank(message = "INVALID_REQUEST")
        String password
) { }
