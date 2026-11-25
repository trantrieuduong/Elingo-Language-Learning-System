package com.elingo.learning.lesson.repository.specification;

import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.entity.LessonStatus;

import java.util.Set;

public record LessonFilter(
        Set<Long> cefrLevelIds,
        Set<Long> tagIds,
        String keyword,
        LessonStatus status, // null = mọi trạng thái
        Boolean isPremium // null = cả hai
) {
    // Learner: luôn chỉ PUBLISHED, không lọc premium
    public static LessonFilter forLearner(Set<Long> cefrLevelIds, Set<Long> tagIds, String keyword) {
        return new LessonFilter(cefrLevelIds, tagIds, keyword, LessonStatus.PUBLISHED, null);
    }

    public static LessonFilter fromAdminRequest(AdminLessonSearchRequest request) {
        return new LessonFilter(request.cefrLevelIds(), request.tagIds(), request.keyword(),
                request.status(), request.isPremium());
    }
}
