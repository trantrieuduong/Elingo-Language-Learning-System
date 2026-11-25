package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.dto.projection.LessonSegmentCountProjection;
import com.elingo.learning.lesson.entity.LessonSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonSegmentRepository extends JpaRepository<LessonSegment, Long> {
    List<LessonSegment> findAllByLessonIdOrderByStartMsAsc(Long lessonId);

    Optional<LessonSegment> findByIdAndLessonId(Long id, Long lessonId);

    long countByLessonId(Long lessonId);

    @Query("""
            SELECT s.lesson.id AS lessonId, COUNT(s.id) AS countVal
            FROM LessonSegment s
            WHERE s.lesson.id IN :lessonIds
            GROUP BY s.lesson.id
            """)
    List<LessonSegmentCountProjection> countByLessonIds(@Param("lessonIds") Collection<Long> lessonIds);
}
