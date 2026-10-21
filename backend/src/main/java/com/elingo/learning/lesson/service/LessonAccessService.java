package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.entity.Lesson;

public interface LessonAccessService {
    Lesson validateLessonAccess(Long userId, String slug);
}
