package com.elingo.admin.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockUserRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String reason
) {
}
