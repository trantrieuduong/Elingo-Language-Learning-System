package com.elingo.learning.lesson.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request payload for deleting one or several segments of a lesson")
public record DeleteLessonSegmentsRequest(

        @Schema(description = "Ids of the segments to delete (1–200, all must belong to the lesson)",
                example = "[11, 12, 15]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "INVALID_REQUEST")
        @Size(max = 200, message = "INVALID_REQUEST")
        Set<@NotNull(message = "INVALID_REQUEST") @Positive(message = "INVALID_REQUEST") Long> segmentIds
) {}
