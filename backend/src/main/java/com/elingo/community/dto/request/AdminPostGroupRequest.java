package com.elingo.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating or updating a community post group")
public record AdminPostGroupRequest(
        @Schema(
                description = "Unique name of the post group",
                example = "English Grammar",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 255
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 255, message = "INVALID_REQUEST")
        String groupName,

        @Schema(
                description = "Optional description of the post group",
                example = "Discussions and tips about English grammar rules",
                maxLength = 500
        )
        @Size(max = 500, message = "INVALID_REQUEST")
        String description
) {
}
