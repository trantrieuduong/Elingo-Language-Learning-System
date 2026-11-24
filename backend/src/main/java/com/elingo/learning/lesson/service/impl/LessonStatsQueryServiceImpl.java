package com.elingo.learning.lesson.service.impl;

import com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection;
import com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection;
import com.elingo.learning.lesson.dto.projection.TopLessonProjection;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.service.LessonStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonStatsQueryServiceImpl implements LessonStatsQueryService {

    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LessonStatusCountProjection> countLessonsByStatus() {
        return lessonRepository.countLessonsByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPremiumCountProjection> countLessonsByPremium() {
        return lessonRepository.countLessonsByPremium();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopLessonProjection> getTopLessonsByCompletion(int limit) {
        return lessonRepository.findTopLessonsByCompletion(Math.max(1, limit));
    }
}
