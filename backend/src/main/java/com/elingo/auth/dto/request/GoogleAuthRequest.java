package com.elingo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "GOOGLE_TOKEN_INVALID")
        String idToken
) {
}
