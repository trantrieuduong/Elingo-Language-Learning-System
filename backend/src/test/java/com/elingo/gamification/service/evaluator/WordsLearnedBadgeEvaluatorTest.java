package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WordsLearnedBadgeEvaluatorTest {

    @Mock
    private UserCardStateRepository userCardStateRepository;

    @InjectMocks
    private WordsLearnedBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports WORDS_LEARNED condition type")
    void supportsType_ReturnsWordsLearned() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.WORDS_LEARNED);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns learned cards count from repository")
        void getCurrentProgress_ReturnsCountFromRepo() {
            when(userCardStateRepository.countLearnedCardsByUserId(USER_ID)).thenReturn(50L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(50L);
            verify(userCardStateRepository).countLearnedCardsByUserId(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when repository returns 0")
        void getCurrentProgress_WhenNoCardsLearned_ReturnsZero() {
            when(userCardStateRepository.countLearnedCardsByUserId(USER_ID)).thenReturn(0L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(userCardStateRepository).countLearnedCardsByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("isEligible")
    class IsEligibleTests {

        @Test
        @DisplayName("Returns true when progress equals or exceeds condition value")
        void isEligible_WhenProgressMeetsOrExceedsCondition_ReturnsTrue() {
            Badge badge = Badge.builder()
                    .conditionValue(50)
                    .build();

            assertThat(evaluator.isEligible(50L, badge)).isTrue();
            assertThat(evaluator.isEligible(100L, badge)).isTrue();
        }

        @Test
        @DisplayName("Returns false when progress is strictly below condition value")
        void isEligible_WhenProgressBelowCondition_ReturnsFalse() {
            Badge badge = Badge.builder()
                    .conditionValue(50)
                    .build();

            assertThat(evaluator.isEligible(49L, badge)).isFalse();
            assertThat(evaluator.isEligible(0L, badge)).isFalse();
        }
    }
}
