package com.elingo.learning.lesson.repository.specification;

import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class LessonSpecifications {

    private static final String ID = "id";

    private LessonSpecifications() {
    }

    public static Specification<Lesson> withFilter(LessonFilter filter) {
        return Specification.allOf(
                isPublished(),
                hasCefrLevelId(filter.cefrLevelId()),
                hasTagId(filter.tagId()),
                matchesKeyword(filter.keyword())
        );
    }

    private static Specification<Lesson> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), LessonStatus.PUBLISHED);
    }

    private static Specification<Lesson> hasCefrLevelId(Long cefrLevelId) {
        if (cefrLevelId == null) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var subRoot = sub.correlate(root);
            var join = subRoot.join("cefrLevels");
            sub.select(cb.literal(1L)).where(cb.equal(join.get(ID), cefrLevelId));
            return cb.exists(sub);
        };
    }

    private static Specification<Lesson> hasTagId(Long tagId) {
        if (tagId == null) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var subRoot = sub.correlate(root);
            var join = subRoot.join("tags");
            sub.select(cb.literal(1L)).where(cb.equal(join.get(ID), tagId));
            return cb.exists(sub);
        };
    }

    private static Specification<Lesson> matchesKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
