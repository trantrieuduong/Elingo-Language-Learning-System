package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeEvaluator;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonsCompletedBadgeEvaluator implements BadgeEvaluator {

    private final UserLessonProgressRepository lessonProgressRepository;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.LESSONS_COMPLETED;
    }

    @Override
    public long getCurrentProgress(long userId) {
        return lessonProgressRepository.countCompletedLessonsByUserId(userId);
    }
}
