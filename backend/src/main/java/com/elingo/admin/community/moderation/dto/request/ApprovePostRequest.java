package com.elingo.admin.community.moderation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for manually approving a community post")
public record ApprovePostRequest(
        @Schema(
                description = "Optional administrative note for approving the post",
                example = "Content verified safe after manual inspection",
                maxLength = 500
        )
        @Size(max = 500, message = "INVALID_REQUEST")
        String note
) {
}
