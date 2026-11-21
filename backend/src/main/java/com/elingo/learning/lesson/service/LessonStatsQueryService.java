package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.repository.LessonRepository;

import java.util.List;

public interface LessonStatsQueryService {
    List<LessonRepository.LessonStatusCountProjection> countLessonsByStatus();
    List<LessonRepository.LessonPremiumCountProjection> countLessonsByPremium();
    List<LessonRepository.TopLessonProjection> getTopLessonsByCompletion(int limit);
}
