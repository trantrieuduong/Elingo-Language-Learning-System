package com.elingo.learning.lesson.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.premium.service.PremiumAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "LESSON-ACCESS-SERVICE")
public class LessonAccessServiceImpl implements LessonAccessService {

    private final LessonRepository lessonRepository;
    private final PremiumAccessService premiumAccessService;

    @Override
    @Transactional(readOnly = true)
    public Lesson validateLessonAccess(Long userId, Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndStatus(lessonId, LessonStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(AppError.LESSON_NOT_FOUND));

        assertCanLearn(lesson, userId);

        return lesson;
    }

    @Override
    @Transactional(readOnly = true)
    public Lesson validateLessonAccess(Long userId, String slug) {
        Lesson lesson = lessonRepository.findBySlugAndStatus(slug, LessonStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(AppError.LESSON_NOT_FOUND));

        assertCanLearn(lesson, userId);

        return lesson;
    }

    private void assertCanLearn(Lesson lesson, Long userId) {
        if (Boolean.TRUE.equals(lesson.getIsPremium())) {
            premiumAccessService.requirePremium(userId);
        }
    }
}
