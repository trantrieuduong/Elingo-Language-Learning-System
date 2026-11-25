package com.elingo.learning.lesson.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request payload for creating or fully replacing a lesson")
public record LessonRequest(
        @Schema(description = "Lesson title", example = "How to Order Coffee in English",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String title,

        @Schema(description = "Lesson description (null clears it)", example = "A short everyday conversation.", maxLength = 1000)
        @Size(max = 1000, message = "INVALID_REQUEST")
        String description,

        @Schema(description = "YouTube video URL (watch?v=, youtu.be/, /embed/ or /shorts/)",
                example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String sourceUrl,

        @Schema(description = "Video duration in milliseconds, read from the YouTube player by the admin UI",
                example = "212000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer durationMs,

        @Schema(description = "Verified media key (or its CDN URL) of the thumbnail image; null removes the thumbnail",
                example = "system/lessons/5f1c3a7e.png", maxLength = 500)
        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.IMAGE)
        @BuildMediaKey String thumbnailUrl,

        @Schema(description = "Whether the lesson requires a premium subscription", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "INVALID_REQUEST")
        Boolean isPremium,

        @Schema(description = "Tag ids (null is treated as empty)", example = "[1, 3]")
        Set<Long> tagIds,

        @Schema(description = "CEFR level ids (null is treated as empty)", example = "[2]")
        Set<Long> cefrLevelIds
) {
    public LessonRequest {
        title = title == null ? null : title.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
        sourceUrl = sourceUrl == null ? null : sourceUrl.strip();
        thumbnailUrl = (thumbnailUrl == null || thumbnailUrl.isBlank()) ? null : thumbnailUrl.strip();
        tagIds = tagIds == null ? Set.of() : tagIds;
        cefrLevelIds = cefrLevelIds == null ? Set.of() : cefrLevelIds;
    }
}
