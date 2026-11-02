package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<UserLessonProgress> findAllByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);
}
