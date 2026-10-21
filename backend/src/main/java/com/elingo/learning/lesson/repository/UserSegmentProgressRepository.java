package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.UserSegmentProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSegmentProgressRepository extends JpaRepository<UserSegmentProgress, Long> {
    Optional<UserSegmentProgress> findByUserIdAndSegmentId(Long userId, Long segmentId);

    List<UserSegmentProgress> findAllByUserIdAndLessonId(Long userId, Long lessonId);

    long countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(Long userId, Long lessonId, Integer value);

    long countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(Long userId, Long lessonId, Integer value);
}
