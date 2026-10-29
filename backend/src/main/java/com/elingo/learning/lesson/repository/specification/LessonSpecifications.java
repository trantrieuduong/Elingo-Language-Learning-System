package com.elingo.learning.lesson.repository.specification;

import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class LessonSpecifications {

    private static final String ID = "id";
    private static final String TAGS = "tags";
    private static final String CEFR_LEVELS = "cefrLevels";
    private static final char LIKE_ESCAPE = '!';

    private LessonSpecifications() {
    }

    public static Specification<Lesson> withFilter(LessonFilter filter) {
        return Specification.allOf(
                isPublished(),
                hasCefrLevels(filter.cefrLevelIds()),
                hasTags(filter.tagIds()),
                matchesKeyword(filter.keyword())
        );
    }

    private static Specification<Lesson> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), LessonStatus.PUBLISHED);
    }

    private static Specification<Lesson> hasCefrLevels(Collection<Long> cefrLevelIds) {
        return matchAll(CEFR_LEVELS, cefrLevelIds);
    }

    private static Specification<Lesson> hasTags(Collection<Long> tagIds) {
        return matchAll(TAGS, tagIds);
    }

    private static Specification<Lesson> matchAll(String attribute, Collection<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) {
                return null; // null = "không có điều kiện này"
            }
            Set<Long> distinctIds = new HashSet<>(ids); // bỏ trùng trước khi đếm
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Lesson> subLesson = sub.correlate(root);
            Join<Lesson, Object> item = subLesson.join(attribute);
            sub.select(cb.countDistinct(item.get(ID))).where(item.get(ID).in(distinctIds));
            return cb.equal(sub, (long) distinctIds.size());
        };
    }

    private static Specification<Lesson> matchesKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)
            );
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
