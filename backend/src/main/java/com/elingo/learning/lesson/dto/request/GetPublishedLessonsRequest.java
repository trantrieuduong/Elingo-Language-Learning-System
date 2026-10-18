package com.elingo.learning.lesson.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GetPublishedLessonsRequest(
        @Size(max = 10, message = "CEFR_LEVEL_CODE_INVALID")
        String cefrCode,

        @Size(max = 100, message = "TAG_CODE_INVALID")
        String tagCode,

        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        @Min(value = 1, message = "PAGE_INDEX_INVALID")
        Integer page
) {
}
