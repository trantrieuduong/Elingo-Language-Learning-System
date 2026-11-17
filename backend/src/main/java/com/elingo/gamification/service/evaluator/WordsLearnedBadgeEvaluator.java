package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeEvaluator;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WordsLearnedBadgeEvaluator implements BadgeEvaluator {

    private final UserCardStateRepository userCardStateRepository;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.WORDS_LEARNED;
    }

    @Override
    public long getCurrentProgress(long userId) {
        return userCardStateRepository.countLearnedCardsByUserId(userId);
    }
}
