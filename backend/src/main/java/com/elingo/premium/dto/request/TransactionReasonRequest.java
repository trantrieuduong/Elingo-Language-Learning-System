package com.elingo.premium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload containing administrative reason for transaction confirmation or refund")
public record TransactionReasonRequest(
        @Schema(
                description = "Administrative reason or reconciliation note",
                example = "Verified manual bank transfer statement #FT261125001",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String reason
) {
}
