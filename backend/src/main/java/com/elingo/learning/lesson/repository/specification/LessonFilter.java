package com.elingo.learning.lesson.repository.specification;

import java.util.Set;

public record LessonFilter(
        Set<Long> cefrLevelIds,
        Set<Long> tagIds,
        String keyword
) {
}
