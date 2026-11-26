package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    interface LessonProgressStats {
        long getCompletedDictationLessons();
        long getInProgressDictationLessons();
        long getCompletedShadowingLessons();
        long getInProgressShadowingLessons();
    }

    Optional<UserLessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<UserLessonProgress> findAllByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

    @Query("""
            SELECT count(p) FROM UserLessonProgress p
            WHERE p.user.id = :userId
              AND (p.dictationStatus = com.elingo.learning.lesson.entity.ProgressStatus.COMPLETED
                   OR p.shadowingStatus = com.elingo.learning.lesson.entity.ProgressStatus.COMPLETED)
            """)
    long countCompletedLessonsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT
              coalesce(sum(case when p.dictationStatus = com.elingo.learning.lesson.entity.ProgressStatus.COMPLETED then 1 else 0 end), 0) as completedDictationLessons,
              coalesce(sum(case when p.dictationStatus = com.elingo.learning.lesson.entity.ProgressStatus.IN_PROGRESS then 1 else 0 end), 0) as inProgressDictationLessons,
              coalesce(sum(case when p.shadowingStatus = com.elingo.learning.lesson.entity.ProgressStatus.COMPLETED then 1 else 0 end), 0) as completedShadowingLessons,
              coalesce(sum(case when p.shadowingStatus = com.elingo.learning.lesson.entity.ProgressStatus.IN_PROGRESS then 1 else 0 end), 0) as inProgressShadowingLessons
            FROM UserLessonProgress p
            WHERE p.user.id = :userId
            """)
    LessonProgressStats getLessonProgressStats(@Param("userId") Long userId);
}
