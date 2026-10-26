package com.elingo.learning.lesson.repository.specification;

public record LessonFilter(
        Long cefrLevelId,
        Long tagId,
        String keyword
) {
}
