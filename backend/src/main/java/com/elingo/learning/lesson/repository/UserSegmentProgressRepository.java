package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.UserSegmentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSegmentProgressRepository extends JpaRepository<UserSegmentProgress, Long> {
    interface SegmentProgressStats {
        long getCompletedSegments();
        long getCompletedDictationSegments();
        long getCompletedShadowingSegments();
        long getDictationTotalAttempts();
        long getShadowingTotalAttempts();
        Double getAverageDictationScore();
        Double getAverageShadowingScore();
        long getEstimatedStudyMs();
        long getDictationEstimatedStudyMs();
        long getShadowingEstimatedStudyMs();
    }

    Optional<UserSegmentProgress> findByUserIdAndSegmentId(Long userId, Long segmentId);

    List<UserSegmentProgress> findAllByUserIdAndLessonId(Long userId, Long lessonId);

    long countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(Long userId, Long lessonId, Integer value);

    long countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(Long userId, Long lessonId, Integer value);

    @Query("""
            SELECT count(p) FROM UserSegmentProgress p
            WHERE p.user.id = :userId
              AND (p.dictationAttemptCount > 0 OR p.shadowingAttemptCount > 0)
            """)
    long countCompletedSegmentsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT
              coalesce(sum(case when (p.dictationAttemptCount > 0 or p.shadowingAttemptCount > 0) then 1 else 0 end), 0) as completedSegments,
              coalesce(sum(case when p.dictationAttemptCount > 0 then 1 else 0 end), 0) as completedDictationSegments,
              coalesce(sum(case when p.shadowingAttemptCount > 0 then 1 else 0 end), 0) as completedShadowingSegments,
              coalesce(sum(p.dictationAttemptCount), 0) as dictationTotalAttempts,
              coalesce(sum(p.shadowingAttemptCount), 0) as shadowingTotalAttempts,
              avg(case when p.dictationAttemptCount > 0 then p.dictationBestScore else null end) as averageDictationScore,
              avg(case when p.shadowingAttemptCount > 0 then p.shadowingBestScore else null end) as averageShadowingScore,
              coalesce(sum(cast((s.endMs - s.startMs) as long) * (p.dictationAttemptCount + p.shadowingAttemptCount)), 0) as estimatedStudyMs,
              coalesce(sum(cast((s.endMs - s.startMs) as long) * p.dictationAttemptCount), 0) as dictationEstimatedStudyMs,
              coalesce(sum(cast((s.endMs - s.startMs) as long) * p.shadowingAttemptCount), 0) as shadowingEstimatedStudyMs
            FROM UserSegmentProgress p
            JOIN p.segment s
            WHERE p.user.id = :userId
            """)
    SegmentProgressStats getSegmentProgressStats(@Param("userId") Long userId);
}
