package com.elingo.community.dto.response;

import com.elingo.community.entity.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response payload containing full post details and complete moderation history")
public record AdminPostModerationDetailResponse(
        @Schema(description = "Unique identifier of the post", example = "105")
        Long id,

        @Schema(description = "Identifier of the post group", example = "1")
        Long groupId,

        @Schema(description = "Name of the post group", example = "English Grammar")
        String groupName,

        @Schema(description = "Author details")
        AuthorResponse author,

        @Schema(description = "Title of the post", example = "Tips for IELTS Speaking Part 2")
        String title,

        @Schema(description = "Body content of the post")
        String content,

        @Schema(description = "Current moderation status of the post", example = "NEEDS_REVIEW")
        PostStatus status,

        @Schema(description = "Total number of likes", example = "0")
        Integer likeCount,

        @Schema(description = "Total number of comments", example = "0")
        Integer commentCount,

        @Schema(description = "Media attachments of the post")
        List<PostMediaResponse> medias,

        @Schema(description = "Complete chronological moderation log history for this post")
        List<PostModerationLogResponse> moderationLogs,

        @Schema(description = "Timestamp when the post was created", example = "2026-03-15T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the post was last updated", example = "2026-03-15T09:30:00")
        LocalDateTime updatedAt
) {
}
