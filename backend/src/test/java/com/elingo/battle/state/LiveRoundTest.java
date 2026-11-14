package com.elingo.battle.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class LiveRoundTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;

    private LiveQuestion question;
    private LiveRound round;

    @BeforeEach
    void setUp() {
        question = new LiveQuestion(1001L, 2, "apple", "tao", 15000, List.of());
        round = new LiveRound(2, question, 1_000L, 16_000L);
    }

    @Test
    @DisplayName("New round keeps the constructor data and has no answers")
    void newRound() {
        assertThat(round.getRoundNo()).isEqualTo(2);
        assertThat(round.getQuestion()).isSameAs(question);
        assertThat(round.getStartedAt()).isEqualTo(1_000L);
        assertThat(round.getDeadlineAt()).isEqualTo(16_000L);
        assertThat(round.isEnded()).isFalse();
        assertThat(round.answerCount()).isZero();
        assertThat(round.hasAnswered(USER_A)).isFalse();
        assertThat(round.getAnswer(USER_A)).isNull();
    }

    @Test
    @DisplayName("putAnswer stores the answer of the user")
    void putAnswer() {
        LiveAnswer answer = new LiveAnswer(USER_A, 5L, null, true, 2000, 140);

        round.putAnswer(answer);

        assertThat(round.hasAnswered(USER_A)).isTrue();
        assertThat(round.hasAnswered(USER_B)).isFalse();
        assertThat(round.getAnswer(USER_A)).isSameAs(answer);
        assertThat(round.answerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("answerCount counts the answers of both users")
    void answerCount() {
        round.putAnswer(new LiveAnswer(USER_A, 5L, null, true, 2000, 140));
        round.putAnswer(new LiveAnswer(USER_B, null, "apple", false, 3000, 0));

        assertThat(round.answerCount()).isEqualTo(2);
        assertThat(round.getAnswer(USER_B).typedAnswer()).isEqualTo("apple");
    }

    @Test
    @DisplayName("putAnswer replaces an existing answer of the same user")
    void putAnswer_ReplacesSameUser() {
        round.putAnswer(new LiveAnswer(USER_A, 5L, null, false, 2000, 0));
        LiveAnswer second = new LiveAnswer(USER_A, 6L, null, true, 3000, 130);

        round.putAnswer(second);

        assertThat(round.answerCount()).isEqualTo(1);
        assertThat(round.getAnswer(USER_A)).isSameAs(second);
    }

    @Test
    @DisplayName("markEnded marks the round as ended")
    void markEnded() {
        round.markEnded();

        assertThat(round.isEnded()).isTrue();
    }
}
