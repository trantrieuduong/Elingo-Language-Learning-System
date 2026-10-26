package com.elingo.learning.lesson.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GetPublishedLessonsRequest(
        @Positive(message = "INVALID_REQUEST")
        Long cefrLevelId,

        @Positive(message = "INVALID_REQUEST")
        Long tagId,

        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        @Min(value = 1, message = "PAGE_INDEX_INVALID")
        Integer page
) {
}
