package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.UserSegmentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserSegmentProgressRepository extends JpaRepository<UserSegmentProgress, Long> {
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

    // Xóa tiến độ segment khi admin xóa segment (dùng sau khi đã xóa word accuracy)
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM UserSegmentProgress p WHERE p.segment.id IN :segmentIds")
    void deleteAllBySegmentIds(@Param("segmentIds") Collection<Long> segmentIds);
}
