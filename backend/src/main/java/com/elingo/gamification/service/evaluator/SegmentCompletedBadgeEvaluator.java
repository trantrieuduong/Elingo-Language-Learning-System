package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeEvaluator;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SegmentCompletedBadgeEvaluator implements BadgeEvaluator {

    private final UserSegmentProgressRepository segmentProgressRepository;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.SEGMENT_COMPLETED;
    }

    @Override
    public long getCurrentProgress(long userId) {
        return segmentProgressRepository.countCompletedSegmentsByUserId(userId);
    }
}
