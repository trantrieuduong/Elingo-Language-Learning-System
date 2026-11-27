package com.elingo.community.dto.response;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.entity.ModerationActorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response payload representing a single post moderation log entry")
public record PostModerationLogResponse(
        @Schema(description = "Unique identifier of the moderation log", example = "201")
        Long id,

        @Schema(description = "Identifier of the moderated post", example = "105")
        Long postId,

        @Schema(description = "Actor type that performed the moderation (AI or ADMIN)", example = "AI")
        ModerationActorType actorType,

        @Schema(description = "Identifier of the administrator if actorType is ADMIN, otherwise null", example = "1")
        Long actorId,

        @Schema(description = "Username of the administrator if actorType is ADMIN, otherwise null", example = "admin_root")
        String actorUsername,

        @Schema(description = "Moderation action applied to the post", example = "FLAG_FOR_REVIEW")
        ModerationAction action,

        @Schema(description = "AI moderation label if applicable", example = "TOXIC")
        String aiLabel,

        @Schema(description = "AI confidence score between 0 and 1", example = "0.7850")
        BigDecimal aiScore,

        @Schema(description = "Administrative or AI note explaining the decision", example = "Borderline score requires manual review")
        String note,

        @Schema(description = "Timestamp when the moderation action occurred", example = "2026-03-15T09:30:00")
        LocalDateTime createdAt
) {
}
