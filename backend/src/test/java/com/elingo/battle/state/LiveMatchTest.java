package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LiveMatchTest {

    @Mock
    private ScheduledFuture<?> firstFuture;

    @Mock
    private ScheduledFuture<?> secondFuture;

    private static final Long MATCH_ID = 100L;
    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long OTHER_USER = 3L;

    private LiveMatch match;

    @BeforeEach
    void setUp() {
        match = new LiveMatch(MATCH_ID, BattleMode.MULTIPLE_CHOICE, BattleMatchType.RANDOM, 10,
                USER_A, USER_B, Map.of(USER_A, 11L, USER_B, 12L), List.of());
    }

    // ========================================================================
    // initial state
    // ========================================================================
    @Nested
    @DisplayName("initial state")
    class InitialStateTests {

        @Test
        @DisplayName("New match keeps the constructor data")
        void initialState_Data() {
            assertThat(match.getMatchId()).isEqualTo(MATCH_ID);
            assertThat(match.getMode()).isEqualTo(BattleMode.MULTIPLE_CHOICE);
            assertThat(match.getMatchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(match.getTotalRounds()).isEqualTo(10);
            assertThat(match.getUserAId()).isEqualTo(USER_A);
            assertThat(match.getUserBId()).isEqualTo(USER_B);
        }

        @Test
        @DisplayName("New match is in progress with no round, no result and no pending task")
        void initialState_Defaults() {
            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);
            assertThat(match.getCurrentRound()).isNull();
            assertThat(match.getLastRoundResult()).isNull();
            assertThat((Object) match.getPendingFuture()).isNull();
            assertThat(match.getPlayers()).isNull();
        }

        @Test
        @DisplayName("New match starts with zero scores and correct counts for both users")
        void initialState_ScoresAreZero() {
            assertThat(match.getScore(USER_A)).isZero();
            assertThat(match.getScore(USER_B)).isZero();
            assertThat(match.getCorrectCount(USER_A)).isZero();
            assertThat(match.getCorrectCount(USER_B)).isZero();
            assertThat(match.getScores()).containsOnlyKeys(USER_A, USER_B);
        }

        @Test
        @DisplayName("New match has no disconnected user")
        void initialState_NoDisconnectedUser() {
            assertThat(match.isDisconnected(USER_A)).isFalse();
            assertThat(match.isDisconnected(USER_B)).isFalse();
        }
    }

    // ========================================================================
    // users
    // ========================================================================
    @Nested
    @DisplayName("users")
    class UserTests {

        @Test
        @DisplayName("hasUser is true only for the two players")
        void hasUser() {
            assertThat(match.hasUser(USER_A)).isTrue();
            assertThat(match.hasUser(USER_B)).isTrue();
            assertThat(match.hasUser(OTHER_USER)).isFalse();
        }

        @Test
        @DisplayName("opponentOf returns the other player")
        void opponentOf_Success() {
            assertThat(match.opponentOf(USER_A)).isEqualTo(USER_B);
            assertThat(match.opponentOf(USER_B)).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("opponentOf returns null for a user who is not in the match")
        void opponentOf_UnknownUser() {
            assertThat(match.opponentOf(OTHER_USER)).isNull();
        }

        @Test
        @DisplayName("getParticipantId returns the participant id of a player")
        void getParticipantId() {
            assertThat(match.getParticipantId(USER_A)).isEqualTo(11L);
            assertThat(match.getParticipantId(USER_B)).isEqualTo(12L);
            assertThat(match.getParticipantId(OTHER_USER)).isNull();
        }
    }

    // ========================================================================
    // scores
    // ========================================================================
    @Nested
    @DisplayName("scores")
    class ScoreTests {

        @Test
        @DisplayName("addScore with a correct answer adds points and increases the correct count")
        void addScore_Correct() {
            match.addScore(USER_A, 140, true);

            assertThat(match.getScore(USER_A)).isEqualTo(140);
            assertThat(match.getCorrectCount(USER_A)).isEqualTo(1);
            assertThat(match.getScore(USER_B)).isZero();
            assertThat(match.getCorrectCount(USER_B)).isZero();
        }

        @Test
        @DisplayName("addScore with a wrong answer does not increase the correct count")
        void addScore_Wrong() {
            match.addScore(USER_A, 0, false);

            assertThat(match.getScore(USER_A)).isZero();
            assertThat(match.getCorrectCount(USER_A)).isZero();
        }

        @Test
        @DisplayName("addScore accumulates over rounds")
        void addScore_Accumulates() {
            match.addScore(USER_A, 140, true);
            match.addScore(USER_A, 0, false);
            match.addScore(USER_A, 120, true);

            assertThat(match.getScore(USER_A)).isEqualTo(260);
            assertThat(match.getCorrectCount(USER_A)).isEqualTo(2);
        }

        @Test
        @DisplayName("getScore and getCorrectCount default to zero for unknown users")
        void scores_UnknownUser() {
            assertThat(match.getScore(OTHER_USER)).isZero();
            assertThat(match.getCorrectCount(OTHER_USER)).isZero();
        }
    }

    // ========================================================================
    // connection
    // ========================================================================
    @Nested
    @DisplayName("connection")
    class ConnectionTests {

        @Test
        @DisplayName("markDisconnected returns true the first time only")
        void markDisconnected() {
            assertThat(match.markDisconnected(USER_A)).isTrue();
            assertThat(match.markDisconnected(USER_A)).isFalse();
            assertThat(match.isDisconnected(USER_A)).isTrue();
            assertThat(match.isDisconnected(USER_B)).isFalse();
        }

        @Test
        @DisplayName("markConnected returns true only when the user was disconnected")
        void markConnected() {
            match.markDisconnected(USER_A);

            assertThat(match.markConnected(USER_A)).isTrue();
            assertThat(match.markConnected(USER_A)).isFalse();
            assertThat(match.isDisconnected(USER_A)).isFalse();
        }

        @Test
        @DisplayName("markConnected returns false for a user who never disconnected")
        void markConnected_NeverDisconnected() {
            assertThat(match.markConnected(USER_B)).isFalse();
        }

        @Test
        @DisplayName("markConnected cancels the grace period task of the user")
        void markConnected_CancelsGrace() {
            match.markDisconnected(USER_A);
            match.putGrace(USER_A, firstFuture);

            match.markConnected(USER_A);

            verify(firstFuture).cancel(false);
        }
    }

    // ========================================================================
    // grace futures
    // ========================================================================
    @Nested
    @DisplayName("grace futures")
    class GraceTests {

        @Test
        @DisplayName("putGrace cancels the previous task of the same user")
        void putGrace_ReplacesPrevious() {
            match.putGrace(USER_A, firstFuture);

            match.putGrace(USER_A, secondFuture);

            verify(firstFuture).cancel(false);
            verify(secondFuture, never()).cancel(false);
        }

        @Test
        @DisplayName("cancelGrace cancels the task of the user and only once")
        void cancelGrace() {
            match.putGrace(USER_A, firstFuture);

            match.cancelGrace(USER_A);
            match.cancelGrace(USER_A);

            verify(firstFuture).cancel(false);
        }

        @Test
        @DisplayName("cancelGrace does nothing when the user has no task")
        void cancelGrace_NoTask() {
            match.putGrace(USER_B, secondFuture);

            match.cancelGrace(USER_A);

            verify(secondFuture, never()).cancel(false);
        }

        @Test
        @DisplayName("cancelAllGraces cancels the tasks of both users and clears them")
        void cancelAllGraces() {
            match.putGrace(USER_A, firstFuture);
            match.putGrace(USER_B, secondFuture);

            match.cancelAllGraces();
            match.cancelAllGraces();

            verify(firstFuture).cancel(false);
            verify(secondFuture).cancel(false);
        }

        @Test
        @DisplayName("Grace tasks of different users are independent")
        void graceTasksAreIndependent() {
            match.putGrace(USER_A, firstFuture);
            match.putGrace(USER_B, secondFuture);

            match.cancelGrace(USER_A);

            verify(firstFuture).cancel(false);
            verify(secondFuture, never()).cancel(false);
        }
    }

    // ========================================================================
    // locking
    // ========================================================================
    @Nested
    @DisplayName("locking")
    class LockTests {

        @Test
        @DisplayName("lock and unlock acquire and release the match lock")
        void lockAndUnlock() {
            match.lock();
            assertThat(match.getLock().isHeldByCurrentThread()).isTrue();

            match.unlock();
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("The lock is reentrant")
        void lock_Reentrant() {
            match.lock();
            match.lock();
            assertThat(match.getLock().getHoldCount()).isEqualTo(2);

            match.unlock();
            match.unlock();
            assertThat(match.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // mutable state
    // ========================================================================
    @Test
    @DisplayName("Status can be updated")
    void setStatus() {
        match.setStatus(BattleMatchStatus.FINISHED);

        assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
    }
}
