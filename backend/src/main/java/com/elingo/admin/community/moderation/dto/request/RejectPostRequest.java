package com.elingo.admin.community.moderation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for manually rejecting a community post")
public record RejectPostRequest(
        @Schema(
                description = "Administrative reason for rejecting the post",
                example = "Contains unsolicited promotional spam links",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String note
) {
}
