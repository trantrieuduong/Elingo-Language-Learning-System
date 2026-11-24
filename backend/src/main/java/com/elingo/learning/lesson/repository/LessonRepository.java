package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection;
import com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection;
import com.elingo.learning.lesson.dto.projection.TopLessonProjection;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

    Optional<Lesson> findByIdAndStatus(Long id, LessonStatus status);
    Optional<Lesson> findBySlugAndStatus(String slug, LessonStatus status);

    @Query("SELECT l.status AS status, COUNT(l.id) AS countVal FROM Lesson l GROUP BY l.status")
    List<LessonStatusCountProjection> countLessonsByStatus();

    @Query("SELECT l.isPremium AS isPremium, COUNT(l.id) AS countVal FROM Lesson l GROUP BY l.isPremium")
    List<LessonPremiumCountProjection> countLessonsByPremium();

    @Query(value = """
            SELECT
                l.id AS lessonId, l.title AS title, l.thumbnail_url AS thumbnailUrl,
                l.status AS status, l.is_premium AS isPremium,
                COUNT(DISTINCT ulp.user_id) AS learnerCount
            FROM lessons l
            LEFT JOIN user_lesson_progress ulp ON ulp.lesson_id = l.id
            GROUP BY l.id, l.title, l.thumbnail_url, l.status, l.is_premium
            ORDER BY learnerCount DESC, l.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopLessonProjection> findTopLessonsByCompletion(@Param("limit") int limit);
}
