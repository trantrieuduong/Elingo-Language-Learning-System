package com.elingo.admin.community.report.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for resolving a pending user report")
public record ResolveReportRequest(
        @Schema(
                description = "Optional administrative note explaining how the report was resolved",
                example = "Reviewed reported post and confirmed action taken",
                maxLength = 500
        )
        @Size(max = 500, message = "INVALID_REQUEST")
        String resolutionNote
) {
}
