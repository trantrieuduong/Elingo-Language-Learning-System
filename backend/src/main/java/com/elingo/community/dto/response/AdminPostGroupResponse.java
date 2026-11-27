package com.elingo.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload containing community post group details for administration")
public record AdminPostGroupResponse(
        @Schema(description = "Unique identifier of the post group", example = "1")
        Long id,

        @Schema(description = "Name of the post group", example = "English Grammar")
        String groupName,

        @Schema(description = "Description of the post group", example = "Discussions and tips about English grammar rules")
        String description,

        @Schema(description = "Total number of posts belonging to this group", example = "42")
        long postCount,

        @Schema(description = "Timestamp when the post group was created", example = "2026-01-10T08:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the post group was last updated", example = "2026-02-15T12:00:00")
        LocalDateTime updatedAt
) {
}
