package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.LessonSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonSegmentRepository extends JpaRepository<LessonSegment, Long> {
    List<LessonSegment> findAllByLessonIdOrderByStartMsAsc(Long lessonId);

    Optional<LessonSegment> findByIdAndLessonId(Long id, Long lessonId);

    long countByLessonId(Long lessonId);
}
