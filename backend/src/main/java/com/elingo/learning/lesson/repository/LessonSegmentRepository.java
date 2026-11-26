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

    // Kiểm tra xem có segment nào của lesson chồng lấn với [startMs, endMs) không
    boolean existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(
            Long lessonId, Integer endMs, Integer startMs);

    // Kiểm tra chồng lấn khi sửa (loại trừ chính segment đang sửa)
    boolean existsByLessonIdAndIdNotAndStartMsLessThanAndEndMsGreaterThan(
            Long lessonId, Long id, Integer endMs, Integer startMs);
    List<LessonSegment> findAllByLessonIdOrderByStartMsAsc(Long lessonId);

    Optional<LessonSegment> findByIdAndLessonId(Long id, Long lessonId);

    long countByLessonId(Long lessonId);

    List<LessonSegment> findAllByIdInAndLessonId(Collection<Long> ids, Long lessonId);

    @Query("SELECT MAX(s.endMs) FROM LessonSegment s WHERE s.lesson.id = :lessonId")
    Optional<Integer> findMaxEndMsByLessonId(@Param("lessonId") Long lessonId);

    @Query("""
            SELECT s.lesson.id AS lessonId, COUNT(s.id) AS countVal
            FROM LessonSegment s
            WHERE s.lesson.id IN :lessonIds
            GROUP BY s.lesson.id
            """)
    List<LessonSegmentCountProjection> countByLessonIds(@Param("lessonIds") Collection<Long> lessonIds);
}
