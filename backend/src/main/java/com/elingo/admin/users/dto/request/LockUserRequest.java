package com.elingo.admin.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for locking a user account")
public record LockUserRequest(
        @Schema(
                description = "Administrative reason for locking or deactivating the user account",
                example = "Violation of terms: sending unsolicited spam messages",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String reason
) {
}
