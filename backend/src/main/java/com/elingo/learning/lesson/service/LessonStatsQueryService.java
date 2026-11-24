package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection;
import com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection;
import com.elingo.learning.lesson.dto.projection.TopLessonProjection;

import java.util.List;

public interface LessonStatsQueryService {
    List<LessonStatusCountProjection> countLessonsByStatus();
    List<LessonPremiumCountProjection> countLessonsByPremium();
    List<TopLessonProjection> getTopLessonsByCompletion(int limit);
}
