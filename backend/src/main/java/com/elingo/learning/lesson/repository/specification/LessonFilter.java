package com.elingo.learning.lesson.repository.specification;

public record LessonFilter(
        String cefrCode,
        String tagCode,
        String keyword
) {
}
