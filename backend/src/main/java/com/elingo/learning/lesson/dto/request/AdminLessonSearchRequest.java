package com.elingo.learning.lesson.dto.request;

import com.elingo.learning.lesson.entity.LessonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request query parameters for searching and filtering lessons in the admin console")
public record AdminLessonSearchRequest(
        @Schema(description = "Keyword to search across lesson title and description",
                example = "coffee", maxLength = 255)
        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        @Schema(description = "Filter by lesson status (omit for all statuses)", example = "DRAFT")
        LessonStatus status,

        @Schema(description = "Filter by premium flag (omit for both)", example = "false")
        Boolean isPremium,

        @Schema(description = "Lessons must have ALL of these tag ids", example = "[1, 3]")
        Set<Long> tagIds,

        @Schema(description = "Lessons must have ALL of these CEFR level ids", example = "[2]")
        Set<Long> cefrLevelIds
) {
    public AdminLessonSearchRequest {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
    }
}
