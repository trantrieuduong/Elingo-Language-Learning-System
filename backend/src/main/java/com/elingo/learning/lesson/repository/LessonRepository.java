package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    Optional<Lesson> findBySlugAndStatus(String slug, LessonStatus status);
}
