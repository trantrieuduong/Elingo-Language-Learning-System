package com.elingo.learning.lesson.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record GetPublishedLessonsRequest(
        @Size(max = 6, message = "INVALID_REQUEST")
        Set<@Positive(message = "INVALID_REQUEST") Long> cefrLevelIds,

        @Size(max = 10, message = "INVALID_REQUEST")
        Set<@Positive(message = "INVALID_REQUEST") Long> tagIds,

        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        @Min(value = 1, message = "PAGE_INDEX_INVALID")
        Integer page
) {
}
