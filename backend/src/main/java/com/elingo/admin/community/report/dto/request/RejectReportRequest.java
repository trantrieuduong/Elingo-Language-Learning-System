package com.elingo.admin.community.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for rejecting a pending user report")
public record RejectReportRequest(
        @Schema(
                description = "Administrative reason for rejecting the report",
                example = "Reported content does not violate community guidelines",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String resolutionNote
) {
}
