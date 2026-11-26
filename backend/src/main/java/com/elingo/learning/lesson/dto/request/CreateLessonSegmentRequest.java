package com.elingo.learning.lesson.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a lesson segment")
public record CreateLessonSegmentRequest(

        @Schema(description = "Lesson that owns the segment", example = "7",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long lessonId,

        @Schema(description = "Start time in milliseconds (inclusive)", example = "1500",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        @NotNull(message = "INVALID_REQUEST")
        @Min(value = 0, message = "INVALID_REQUEST")
        Integer startMs,

        @Schema(description = "End time in milliseconds (must be greater than startMs)", example = "6200",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer endMs,

        @Schema(description = "Original English transcript",
                example = "Hi, can I get a large coffee, please?",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2000)
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 2000, message = "INVALID_REQUEST")
        String transcriptOriginal,

        @Schema(description = "Translation of the transcript",
                example = "Xin chào, cho tôi một ly cà phê lớn nhé?",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2000)
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 2000, message = "INVALID_REQUEST")
        String translation
) {
    public CreateLessonSegmentRequest {
        transcriptOriginal = transcriptOriginal == null ? null : transcriptOriginal.strip();
        translation = translation == null ? null : translation.strip();
    }
}
