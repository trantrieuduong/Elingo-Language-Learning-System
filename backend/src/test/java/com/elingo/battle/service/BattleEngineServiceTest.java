package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.MatchCancelledResponse;
import com.elingo.battle.dto.response.MatchFinishedResponse;
import com.elingo.battle.dto.response.MatchQuestionResponse;
import com.elingo.battle.dto.response.MatchRejoinedResponse;
import com.elingo.battle.dto.response.OpponentAnsweredResponse;
import com.elingo.battle.dto.response.OptionResponse;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.dto.response.PlayerRoundResultResponse;
import com.elingo.battle.dto.response.RoundResultResponse;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleEngineServiceImpl;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveAnswer;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.battle.state.LiveRound;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.battle.websocket.BattleEvents;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleEngineServiceTest {

    @Mock
    private BattleLiveRegistry registry;

    @Mock
    private BattleEventPublisher publisher;

    @Mock
    private BattleMatchPersistenceService persistence;

    @Mock
    private ThreadPoolTaskScheduler battleScheduler;

    @Mock
    private BattleXpService xpService;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private ScheduledFuture<?> graceFuture;

    @Captor
    private ArgumentCaptor<Map<Long, Integer>> scoresCaptor;

    @Captor
    private ArgumentCaptor<Map<Long, Integer>> correctCountsCaptor;

    @Captor
    private ArgumentCaptor<List<RoundAnswerRecord>> recordsCaptor;

    @InjectMocks
    private BattleEngineServiceImpl engineService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long MATCH_ID = 100L;
    private static final Long QUESTION_1_ID = 1001L;
    private static final Long QUESTION_2_ID = 1002L;
    private static final int TIME_LIMIT_MS = 15000;

    private static final BattleProperties PROPS = new BattleProperties(
            2, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    /** Các task đã được lên lịch, test tự chạy tay theo thứ tự để mô phỏng game loop */
    private final List<Runnable> scheduledTasks = new ArrayList<>();

    private LiveQuestion question1;
    private LiveQuestion question2;
    private LiveMatch match;

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(engineService, "props", PROPS);

        lenient().doAnswer(inv -> {
            scheduledTasks.add(inv.getArgument(0));
            return scheduledFuture;
        }).when(battleScheduler).schedule(any(Runnable.class), any(Instant.class));

        question1 = new LiveQuestion(QUESTION_1_ID, 1, "apple", "tao", TIME_LIMIT_MS, List.of(
                new LiveQuestion.LiveOption(5001L, "tao", true),
                new LiveQuestion.LiveOption(5002L, "cam", false),
                new LiveQuestion.LiveOption(5003L, "chuoi", false),
                new LiveQuestion.LiveOption(5004L, "nho", false)));
        question2 = new LiveQuestion(QUESTION_2_ID, 2, "banana", "chuoi", TIME_LIMIT_MS, List.of(
                new LiveQuestion.LiveOption(6001L, "tao", false),
                new LiveQuestion.LiveOption(6002L, "cam", false),
                new LiveQuestion.LiveOption(6003L, "chuoi", true),
                new LiveQuestion.LiveOption(6004L, "nho", false)));

        match = buildMatch(BattleMode.MULTIPLE_CHOICE, List.of(question1, question2));
    }

    private LiveMatch buildMatch(BattleMode mode, List<LiveQuestion> questions) {
        LiveMatch m = new LiveMatch(MATCH_ID, mode, BattleMatchType.RANDOM, PROPS.totalRounds(),
                USER_A, USER_B, Map.of(USER_A, 11L, USER_B, 12L), questions);
        m.setPlayers(List.of(
                new PlayerResponse(USER_A, "User A", null),
                new PlayerResponse(USER_B, "User B", null)));
        return m;
    }

    /** Gắn một vòng đang chạy (chưa kết thúc) vào match để test submitAnswer độc lập với game loop */
    private LiveRound attachRound(LiveQuestion question, long startedAt, long deadlineAt) {
        LiveRound round = new LiveRound(question.roundNo(), question, startedAt, deadlineAt);
        match.setCurrentRound(round);
        return round;
    }

    private LiveRound attachActiveRound() {
        long now = System.currentTimeMillis();
        return attachRound(question1, now, now + TIME_LIMIT_MS);
    }

    private BattleAnswerRequest mcRequest(LiveQuestion question, Long optionId) {
        return new BattleAnswerRequest(MATCH_ID, question.questionId(), optionId, null);
    }

    private void assertErrorOf(Runnable action, AppError expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(expected));
    }

    /** start() rồi chạy task đếm ngược -> vòng 1 bắt đầu. Sau đó scheduledTasks = [countdown, timeout vòng 1] */
    private void startAndBeginRoundOne() {
        engineService.start(match);
        scheduledTasks.get(0).run();
    }

    // ========================================================================
    // start
    // ========================================================================
    @Nested
    @DisplayName("start")
    class StartTests {

        @Test
        @DisplayName("Start schedules the countdown without sending any event yet")
        void start_Success_SchedulesCountdown() {
            Instant before = Instant.now();

            engineService.start(match);

            assertThat(scheduledTasks).hasSize(1);
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(battleScheduler).schedule(any(Runnable.class), instantCaptor.capture());
            long delayMs = Duration.between(before, instantCaptor.getValue()).toMillis();
            assertThat(delayMs).isBetween(PROPS.countdownMs(), PROPS.countdownMs() + 5000);
            assertThat((Object) match.getPendingFuture()).isSameAs(scheduledFuture);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("First round starts after countdown and sends multiple choice question to both users")
        void start_Success_StartsFirstRound() {
            startAndBeginRoundOne();

            ArgumentCaptor<MatchQuestionResponse> captorA = ArgumentCaptor.forClass(MatchQuestionResponse.class);
            ArgumentCaptor<MatchQuestionResponse> captorB = ArgumentCaptor.forClass(MatchQuestionResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_QUESTION), captorA.capture());
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_QUESTION), captorB.capture());

            MatchQuestionResponse payload = captorA.getValue();
            assertThat(payload).isEqualTo(captorB.getValue());
            assertThat(payload.matchId()).isEqualTo(MATCH_ID);
            assertThat(payload.questionId()).isEqualTo(QUESTION_1_ID);
            assertThat(payload.roundNo()).isEqualTo(1);
            assertThat(payload.totalRounds()).isEqualTo(PROPS.totalRounds());
            assertThat(payload.term()).isEqualTo("apple");
            assertThat(payload.mode()).isEqualTo(BattleMode.MULTIPLE_CHOICE);
            assertThat(payload.timeLimitMs()).isEqualTo(TIME_LIMIT_MS);
            assertThat(payload.deadlineAt() - payload.serverNow()).isEqualTo(TIME_LIMIT_MS);
            assertThat(payload.options()).extracting(OptionResponse::id)
                    .containsExactly(5001L, 5002L, 5003L, 5004L);
            assertThat(payload.options()).extracting(OptionResponse::text)
                    .containsExactly("tao", "cam", "chuoi", "nho");
            assertThat(payload.answerLength()).isNull();
            assertThat(payload.answerPattern()).isNull();
            assertThat(payload.firstChar()).isNull();

            assertThat(match.getCurrentRound()).isNotNull();
            assertThat(match.getCurrentRound().getRoundNo()).isEqualTo(1);
            // Đã lên lịch tiếp task hết giờ của vòng 1
            assertThat(scheduledTasks).hasSize(2);
        }

        @Test
        @DisplayName("First round of typing match sends answer hints and no options")
        void start_Success_TypingQuestionPayload() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));

            startAndBeginRoundOne();

            ArgumentCaptor<MatchQuestionResponse> captor = ArgumentCaptor.forClass(MatchQuestionResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_QUESTION), captor.capture());
            MatchQuestionResponse payload = captor.getValue();
            assertThat(payload.mode()).isEqualTo(BattleMode.TYPING);
            assertThat(payload.term()).isEqualTo("kem");
            assertThat(payload.options()).isNull();
            assertThat(payload.answerLength()).isEqualTo(9);
            assertThat(payload.answerPattern()).containsExactly(3, 5);
            assertThat(payload.firstChar()).isEqualTo("i");
        }

        @Test
        @DisplayName("Countdown does nothing when match is no longer in progress")
        void start_Success_MatchCancelledBeforeCountdownEnds() {
            engineService.start(match);
            match.setStatus(BattleMatchStatus.CANCELLED);

            scheduledTasks.get(0).run();

            verifyNoInteractions(publisher);
            assertThat(match.getCurrentRound()).isNull();
        }
    }

    // ========================================================================
    // submitAnswer
    // ========================================================================
    @Nested
    @DisplayName("submitAnswer")
    class SubmitAnswerTests {

        @Test
        @DisplayName("Submit correct multiple choice answer successfully")
        void submitAnswer_Success_CorrectOption() {
            LiveRound round = attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);

            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));

            LiveAnswer answer = round.getAnswer(USER_A);
            assertThat(answer).isNotNull();
            assertThat(answer.correct()).isTrue();
            assertThat(answer.selectedOptionId()).isEqualTo(5001L);
            assertThat(answer.typedAnswer()).isNull();
            // 100 điểm cơ bản + tối đa 50 điểm tốc độ
            assertThat(answer.points()).isBetween(100, 150);
            verify(publisher).sendToUser(USER_B, BattleEvents.MATCH_OPPONENT_ANSWERED,
                    new OpponentAnsweredResponse(MATCH_ID, 1));
            // Mới một người trả lời nên vòng chưa kết thúc
            assertThat(round.isEnded()).isFalse();
            verify(persistence, never()).saveRoundAnswers(any());
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Submit wrong multiple choice answer gives zero points")
        void submitAnswer_Success_WrongOption() {
            LiveRound round = attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);

            engineService.submitAnswer(USER_A, mcRequest(question1, 5002L));

            LiveAnswer answer = round.getAnswer(USER_A);
            assertThat(answer.correct()).isFalse();
            assertThat(answer.points()).isZero();
        }

        @Test
        @DisplayName("Submit correct typing answer ignores case and extra spaces")
        void submitAnswer_Success_TypingCorrect() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));
            long now = System.currentTimeMillis();
            LiveRound round = attachRound(typing, now, now + TIME_LIMIT_MS);
            when(registry.getByUser(USER_A)).thenReturn(match);

            engineService.submitAnswer(USER_A, new BattleAnswerRequest(MATCH_ID, 2001L, null, "  Ice   CREAM "));

            LiveAnswer answer = round.getAnswer(USER_A);
            assertThat(answer.correct()).isTrue();
            assertThat(answer.selectedOptionId()).isNull();
            assertThat(answer.typedAnswer()).isEqualTo("Ice   CREAM");
            assertThat(answer.points()).isBetween(100, 150);
        }

        @Test
        @DisplayName("Submit wrong typing answer gives zero points")
        void submitAnswer_Success_TypingWrong() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));
            long now = System.currentTimeMillis();
            LiveRound round = attachRound(typing, now, now + TIME_LIMIT_MS);
            when(registry.getByUser(USER_A)).thenReturn(match);

            engineService.submitAnswer(USER_A, new BattleAnswerRequest(MATCH_ID, 2001L, null, "ice cram"));

            assertThat(round.getAnswer(USER_A).correct()).isFalse();
            assertThat(round.getAnswer(USER_A).points()).isZero();
        }

        @Test
        @DisplayName("Submit answer slightly after deadline is accepted within grace period")
        void submitAnswer_Success_WithinGracePeriod() {
            long now = System.currentTimeMillis();
            // Đã quá hạn 500ms nhưng còn trong answerGraceMs (1000ms)
            LiveRound round = attachRound(question1, now - TIME_LIMIT_MS - 500, now - 500);
            when(registry.getByUser(USER_A)).thenReturn(match);

            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));

            LiveAnswer answer = round.getAnswer(USER_A);
            assertThat(answer).isNotNull();
            // Thời gian trả lời bị chặn ở giới hạn nên không còn điểm tốc độ
            assertThat(answer.responseTimeMs()).isEqualTo(TIME_LIMIT_MS);
            assertThat(answer.points()).isEqualTo(100);
        }

        @Test
        @DisplayName("Round ends immediately when both users have answered")
        void submitAnswer_Success_BothAnsweredEndsRound() {
            attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);
            when(registry.getByUser(USER_B)).thenReturn(match);

            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));
            engineService.submitAnswer(USER_B, mcRequest(question1, 5002L));

            assertThat(match.getCurrentRound().isEnded()).isTrue();
            assertThat(match.getScore(USER_A)).isBetween(100, 150);
            assertThat(match.getCorrectCount(USER_A)).isEqualTo(1);
            assertThat(match.getScore(USER_B)).isZero();
            assertThat(match.getCorrectCount(USER_B)).isZero();

            verify(persistence).saveRoundAnswers(recordsCaptor.capture());
            List<RoundAnswerRecord> records = recordsCaptor.getValue();
            assertThat(records).hasSize(2);
            assertThat(records.get(0).questionId()).isEqualTo(QUESTION_1_ID);
            assertThat(records.get(0).participantId()).isEqualTo(11L);
            assertThat(records.get(0).selectedOptionId()).isEqualTo(5001L);
            assertThat(records.get(0).correct()).isTrue();
            assertThat(records.get(1).participantId()).isEqualTo(12L);
            assertThat(records.get(1).selectedOptionId()).isEqualTo(5002L);
            assertThat(records.get(1).correct()).isFalse();

            ArgumentCaptor<RoundResultResponse> resultCaptor = ArgumentCaptor.forClass(RoundResultResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_ROUND_RESULT), resultCaptor.capture());
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_ROUND_RESULT), any(RoundResultResponse.class));
            RoundResultResponse roundResult = resultCaptor.getValue();
            assertThat(roundResult.matchId()).isEqualTo(MATCH_ID);
            assertThat(roundResult.roundNo()).isEqualTo(1);
            assertThat(roundResult.correctAnswer()).isEqualTo("tao");
            assertThat(roundResult.results()).hasSize(2);
            assertThat(roundResult.results().get(0).userId()).isEqualTo(USER_A);
            assertThat(roundResult.results().get(0).answered()).isTrue();
            assertThat(roundResult.results().get(0).isCorrect()).isTrue();
            assertThat(roundResult.results().get(1).userId()).isEqualTo(USER_B);
            assertThat(roundResult.results().get(1).answered()).isTrue();
            assertThat(roundResult.results().get(1).isCorrect()).isFalse();
            assertThat(roundResult.results().get(1).pointsGained()).isZero();
            assertThat(match.getLastRoundResult()).isEqualTo(roundResult);

            // Đã lên lịch chuyển sang vòng kế tiếp
            assertThat(scheduledTasks).hasSize(1);
        }

        @Test
        @DisplayName("Submit answer failed: User is not in any match")
        void submitAnswer_Fail_NoMatch() {
            when(registry.getByUser(USER_A)).thenReturn(null);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5001L)),
                    AppError.BATTLE_PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Submit answer failed: Match id does not match user's current match")
        void submitAnswer_Fail_MatchIdMismatch() {
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A,
                            new BattleAnswerRequest(999L, QUESTION_1_ID, 5001L, null)),
                    AppError.BATTLE_PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Submit answer failed: No round has started yet")
        void submitAnswer_Fail_NoCurrentRound() {
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5001L)),
                    AppError.BATTLE_QUESTION_NOT_FOUND);
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Submit answer failed: Question id does not match current round")
        void submitAnswer_Fail_QuestionMismatch() {
            attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question2, 6003L)),
                    AppError.BATTLE_QUESTION_NOT_FOUND);
        }

        @Test
        @DisplayName("Submit answer failed: Match is not in progress")
        void submitAnswer_Fail_MatchNotInProgress() {
            attachActiveRound();
            match.setStatus(BattleMatchStatus.CANCELLED);
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5001L)),
                    AppError.BATTLE_QUESTION_NOT_FOUND);
        }

        @Test
        @DisplayName("Submit answer failed: Round has already ended")
        void submitAnswer_Fail_RoundEnded() {
            LiveRound round = attachActiveRound();
            round.markEnded();
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5001L)),
                    AppError.BATTLE_QUESTION_NOT_FOUND);
        }

        @Test
        @DisplayName("Submit answer failed: Question already answered")
        void submitAnswer_Fail_AlreadyAnswered() {
            LiveRound round = attachActiveRound();
            round.putAnswer(new LiveAnswer(USER_A, 5001L, null, true, 1000, 140));
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5002L)),
                    AppError.BATTLE_QUESTION_ALREADY_ANSWERED);

            // Đáp án đầu tiên được giữ nguyên
            assertThat(round.getAnswer(USER_A).selectedOptionId()).isEqualTo(5001L);
        }

        @Test
        @DisplayName("Submit answer failed: Deadline and grace period have passed")
        void submitAnswer_Fail_Timeout() {
            long now = System.currentTimeMillis();
            LiveRound round = attachRound(question1, now - TIME_LIMIT_MS - 5000, now - 5000);
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 5001L)),
                    AppError.BATTLE_QUESTION_TIMEOUT);

            assertThat(round.hasAnswered(USER_A)).isFalse();
        }

        @Test
        @DisplayName("Submit answer failed: Multiple choice answer without selected option")
        void submitAnswer_Fail_MissingOption() {
            LiveRound round = attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, null)),
                    AppError.BATTLE_ANSWER_INVALID);

            assertThat(round.hasAnswered(USER_A)).isFalse();
            verify(publisher, never()).sendToUser(any(), anyString(), any());
        }

        @Test
        @DisplayName("Submit answer failed: Selected option does not belong to the question")
        void submitAnswer_Fail_UnknownOption() {
            LiveRound round = attachActiveRound();
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A, mcRequest(question1, 9999L)),
                    AppError.BATTLE_ANSWER_INVALID);

            assertThat(round.hasAnswered(USER_A)).isFalse();
        }

        @Test
        @DisplayName("Submit answer failed: Typing answer is blank")
        void submitAnswer_Fail_BlankTypedAnswer() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));
            long now = System.currentTimeMillis();
            LiveRound round = attachRound(typing, now, now + TIME_LIMIT_MS);
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A,
                            new BattleAnswerRequest(MATCH_ID, 2001L, null, "   ")),
                    AppError.BATTLE_ANSWER_INVALID);

            assertThat(round.hasAnswered(USER_A)).isFalse();
        }

        @Test
        @DisplayName("Submit answer failed: Typing answer is missing")
        void submitAnswer_Fail_NullTypedAnswer() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));
            long now = System.currentTimeMillis();
            attachRound(typing, now, now + TIME_LIMIT_MS);
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A,
                            new BattleAnswerRequest(MATCH_ID, 2001L, null, null)),
                    AppError.BATTLE_ANSWER_INVALID);
        }

        @Test
        @DisplayName("Submit answer failed: Typing answer is too long")
        void submitAnswer_Fail_TypedAnswerTooLong() {
            LiveQuestion typing = new LiveQuestion(2001L, 1, "kem", "ice cream", TIME_LIMIT_MS, List.of());
            match = buildMatch(BattleMode.TYPING, List.of(typing));
            long now = System.currentTimeMillis();
            LiveRound round = attachRound(typing, now, now + TIME_LIMIT_MS);
            when(registry.getByUser(USER_A)).thenReturn(match);

            assertErrorOf(() -> engineService.submitAnswer(USER_A,
                            new BattleAnswerRequest(MATCH_ID, 2001L, null, "a".repeat(101))),
                    AppError.BATTLE_ANSWER_INVALID);

            assertThat(round.hasAnswered(USER_A)).isFalse();
        }
    }

    // ========================================================================
    // game loop (endRound / afterRound / finish)
    // ========================================================================
    @Nested
    @DisplayName("game loop")
    class GameLoopTests {

        @Test
        @DisplayName("Round timeout records unanswered player as incorrect with full time")
        void roundTimeout_Success_UnansweredPlayer() {
            when(registry.getByUser(USER_A)).thenReturn(match);
            startAndBeginRoundOne();
            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));

            // scheduledTasks[1] là task hết giờ vòng 1
            scheduledTasks.get(1).run();

            assertThat(match.getCurrentRound().isEnded()).isTrue();
            verify(persistence).saveRoundAnswers(recordsCaptor.capture());
            List<RoundAnswerRecord> records = recordsCaptor.getValue();
            assertThat(records.get(0).correct()).isTrue();
            assertThat(records.get(1).participantId()).isEqualTo(12L);
            assertThat(records.get(1).selectedOptionId()).isNull();
            assertThat(records.get(1).typedAnswer()).isNull();
            assertThat(records.get(1).correct()).isFalse();
            assertThat(records.get(1).responseTimeMs()).isEqualTo(TIME_LIMIT_MS);

            ArgumentCaptor<RoundResultResponse> captor = ArgumentCaptor.forClass(RoundResultResponse.class);
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_ROUND_RESULT), captor.capture());
            PlayerRoundResultResponse resultB = captor.getValue().results().get(1);
            assertThat(resultB.userId()).isEqualTo(USER_B);
            assertThat(resultB.answered()).isFalse();
            assertThat(resultB.isCorrect()).isFalse();
            assertThat(resultB.pointsGained()).isZero();
            assertThat(resultB.responseTimeMs()).isEqualTo(TIME_LIMIT_MS);
        }

        @Test
        @DisplayName("Ending an already ended round is ignored")
        void endRound_Success_Idempotent() {
            when(registry.getByUser(USER_A)).thenReturn(match);
            when(registry.getByUser(USER_B)).thenReturn(match);
            startAndBeginRoundOne();
            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));
            engineService.submitAnswer(USER_B, mcRequest(question1, 5002L));

            // Task hết giờ cũ vẫn chạy (cancel không kịp) -> không được xử lý lần hai
            scheduledTasks.get(1).run();

            verify(persistence, times(1)).saveRoundAnswers(any());
            verify(publisher, times(1)).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_ROUND_RESULT), any());
        }

        @Test
        @DisplayName("Second round starts after the gap and resets the last round result")
        void afterRound_Success_StartsNextRound() {
            startAndBeginRoundOne();
            scheduledTasks.get(1).run();
            assertThat(match.getLastRoundResult()).isNotNull();

            // scheduledTasks[2] là task afterRound của vòng 1
            scheduledTasks.get(2).run();

            assertThat(match.getCurrentRound().getRoundNo()).isEqualTo(2);
            assertThat(match.getCurrentRound().isEnded()).isFalse();
            assertThat(match.getLastRoundResult()).isNull();
            ArgumentCaptor<MatchQuestionResponse> captor = ArgumentCaptor.forClass(MatchQuestionResponse.class);
            verify(publisher, times(2)).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_QUESTION), captor.capture());
            assertThat(captor.getAllValues().get(1).questionId()).isEqualTo(QUESTION_2_ID);
            assertThat(captor.getAllValues().get(1).roundNo()).isEqualTo(2);
            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Match finishes with a winner after the last round")
        void finish_Success_WithWinner() {
            when(registry.getByUser(USER_A)).thenReturn(match);
            startAndBeginRoundOne();
            engineService.submitAnswer(USER_A, mcRequest(question1, 5001L));
            scheduledTasks.get(1).run();   // hết giờ vòng 1
            scheduledTasks.get(2).run();   // bắt đầu vòng 2
            engineService.submitAnswer(USER_A, mcRequest(question2, 6003L));
            scheduledTasks.get(3).run();   // hết giờ vòng 2
            scheduledTasks.get(4).run();   // afterRound vòng 2 -> kết thúc

            verify(persistence).finishMatch(eq(MATCH_ID), eq(USER_A), scoresCaptor.capture(), correctCountsCaptor.capture());
            assertThat(scoresCaptor.getValue().get(USER_A)).isBetween(200, 300);
            assertThat(scoresCaptor.getValue().get(USER_B)).isZero();
            assertThat(correctCountsCaptor.getValue().get(USER_A)).isEqualTo(2);
            assertThat(correctCountsCaptor.getValue().get(USER_B)).isZero();

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);

            ArgumentCaptor<MatchFinishedResponse> captor = ArgumentCaptor.forClass(MatchFinishedResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_FINISHED), captor.capture());
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_FINISHED), any(MatchFinishedResponse.class));
            MatchFinishedResponse finished = captor.getValue();
            assertThat(finished.matchId()).isEqualTo(MATCH_ID);
            assertThat(finished.winnerId()).isEqualTo(USER_A);
            assertThat(finished.reason()).isEqualTo("COMPLETED");
            assertThat(finished.players()).hasSize(2);
            assertThat(finished.players().get(0).userId()).isEqualTo(USER_A);
            assertThat(finished.players().get(0).isWinner()).isTrue();
            assertThat(finished.players().get(0).correctCount()).isEqualTo(2);
            assertThat(finished.players().get(1).userId()).isEqualTo(USER_B);
            assertThat(finished.players().get(1).isWinner()).isFalse();

            verify(registry).remove(match);
            verify(xpService).awardMatchXp(match, USER_A, "COMPLETED");
        }

        @Test
        @DisplayName("Match finishes as a draw when scores are equal")
        void finish_Success_Draw() {
            startAndBeginRoundOne();
            scheduledTasks.get(1).run();
            scheduledTasks.get(2).run();
            scheduledTasks.get(3).run();
            scheduledTasks.get(4).run();

            verify(persistence).finishMatch(eq(MATCH_ID), isNull(), scoresCaptor.capture(), correctCountsCaptor.capture());
            assertThat(scoresCaptor.getValue().get(USER_A)).isZero();
            assertThat(scoresCaptor.getValue().get(USER_B)).isZero();

            ArgumentCaptor<MatchFinishedResponse> captor = ArgumentCaptor.forClass(MatchFinishedResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_FINISHED), captor.capture());
            assertThat(captor.getValue().winnerId()).isNull();
            assertThat(captor.getValue().players()).allSatisfy(p -> assertThat(p.isWinner()).isFalse());
            verify(xpService).awardMatchXp(match, null, "COMPLETED");
        }

        @Test
        @DisplayName("Finishing the match still succeeds when awarding XP fails")
        void finish_Success_XpFailureIgnored() {
            doThrow(new IllegalStateException("xp down")).when(xpService).awardMatchXp(any(), any(), anyString());
            startAndBeginRoundOne();
            scheduledTasks.get(1).run();
            scheduledTasks.get(2).run();
            scheduledTasks.get(3).run();

            assertThatCode(() -> scheduledTasks.get(4).run()).doesNotThrowAnyException();

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
            verify(registry).remove(match);
        }

        @Test
        @DisplayName("Engine error during a round cancels the match")
        void gameLoop_Fail_UnexpectedErrorCancelsMatch() {
            doThrow(new IllegalStateException("db down")).when(persistence).saveRoundAnswers(any());
            startAndBeginRoundOne();

            // Task hết giờ vòng 1 gặp lỗi khi lưu đáp án -> runSafely hủy trận
            scheduledTasks.get(1).run();

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.CANCELLED);
            verify(persistence).updateMatchStatus(MATCH_ID, BattleMatchStatus.CANCELLED);
            verify(publisher).sendToUser(USER_A, BattleEvents.MATCH_CANCELLED,
                    new MatchCancelledResponse(MATCH_ID, "INTERNAL_ERROR"));
            verify(publisher).sendToUser(USER_B, BattleEvents.MATCH_CANCELLED,
                    new MatchCancelledResponse(MATCH_ID, "INTERNAL_ERROR"));
            verify(registry).remove(match);
            assertThat(match.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // forfeit
    // ========================================================================
    @Nested
    @DisplayName("forfeit")
    class ForfeitTests {

        @Test
        @DisplayName("Forfeit finishes the match and the opponent wins")
        void forfeit_Success() {
            match.setPendingFuture(scheduledFuture);
            match.putGrace(USER_A, graceFuture);
            match.addScore(USER_B, 120, true);

            engineService.forfeit(match, USER_A);

            verify(scheduledFuture).cancel(false);
            verify(graceFuture).cancel(false);
            verify(persistence).finishMatch(eq(MATCH_ID), eq(USER_B), scoresCaptor.capture(), correctCountsCaptor.capture());
            assertThat(scoresCaptor.getValue().get(USER_B)).isEqualTo(120);
            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);

            ArgumentCaptor<MatchFinishedResponse> captor = ArgumentCaptor.forClass(MatchFinishedResponse.class);
            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_FINISHED), captor.capture());
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_FINISHED), any(MatchFinishedResponse.class));
            assertThat(captor.getValue().winnerId()).isEqualTo(USER_B);
            assertThat(captor.getValue().reason()).isEqualTo("FORFEIT");
            verify(registry).remove(match);
            verify(xpService).awardMatchXp(match, USER_B, "FORFEIT");
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Forfeit does nothing when match is not in progress")
        void forfeit_Success_MatchAlreadyEnded() {
            match.setStatus(BattleMatchStatus.FINISHED);

            engineService.forfeit(match, USER_A);

            verifyNoInteractions(persistence);
            verifyNoInteractions(publisher);
            verifyNoInteractions(xpService);
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("Forfeit still finishes the match when awarding XP fails")
        void forfeit_Success_XpFailureIgnored() {
            doThrow(new IllegalStateException("xp down")).when(xpService).awardMatchXp(match, USER_B, "FORFEIT");

            assertThatCode(() -> engineService.forfeit(match, USER_A)).doesNotThrowAnyException();

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
            verify(registry).remove(match);
        }
    }

    // ========================================================================
    // cancel
    // ========================================================================
    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("Cancel marks the match cancelled and notifies both users")
        void cancel_Success() {
            match.setPendingFuture(scheduledFuture);
            match.putGrace(USER_A, graceFuture);

            engineService.cancel(match, "BOTH_DISCONNECTED");

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.CANCELLED);
            verify(scheduledFuture).cancel(false);
            verify(graceFuture).cancel(false);
            verify(persistence).updateMatchStatus(MATCH_ID, BattleMatchStatus.CANCELLED);
            MatchCancelledResponse payload = new MatchCancelledResponse(MATCH_ID, "BOTH_DISCONNECTED");
            verify(publisher).sendToUser(USER_A, BattleEvents.MATCH_CANCELLED, payload);
            verify(publisher).sendToUser(USER_B, BattleEvents.MATCH_CANCELLED, payload);
            verify(registry).remove(match);
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Cancel does nothing when match is not in progress")
        void cancel_Success_MatchAlreadyEnded() {
            match.setStatus(BattleMatchStatus.FINISHED);

            engineService.cancel(match, "INTERNAL_ERROR");

            verifyNoInteractions(persistence);
            verifyNoInteractions(publisher);
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("Cancel still notifies users and cleans up when persisting status fails")
        void cancel_Success_PersistFailureIgnored() {
            doThrow(new IllegalStateException("db down"))
                    .when(persistence).updateMatchStatus(MATCH_ID, BattleMatchStatus.CANCELLED);

            assertThatCode(() -> engineService.cancel(match, "INTERNAL_ERROR")).doesNotThrowAnyException();

            verify(publisher).sendToUser(eq(USER_A), eq(BattleEvents.MATCH_CANCELLED), any(MatchCancelledResponse.class));
            verify(publisher).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_CANCELLED), any(MatchCancelledResponse.class));
            verify(registry).remove(match);
        }
    }

    // ========================================================================
    // snapshot
    // ========================================================================
    @Nested
    @DisplayName("snapshot")
    class SnapshotTests {

        @Test
        @DisplayName("Snapshot before any round has started")
        void snapshot_Success_NoRoundYet() {
            MatchRejoinedResponse response = engineService.snapshot(match, USER_A);

            assertThat(response.snapshot().matchId()).isEqualTo(MATCH_ID);
            assertThat(response.snapshot().mode()).isEqualTo(BattleMode.MULTIPLE_CHOICE);
            assertThat(response.snapshot().matchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(response.snapshot().totalRounds()).isEqualTo(PROPS.totalRounds());
            assertThat(response.snapshot().players()).hasSize(2);
            assertThat(response.snapshot().roundNo()).isZero();
            assertThat(response.snapshot().question()).isNull();
            assertThat(response.snapshot().answered()).isFalse();
            assertThat(response.snapshot().lastRoundResult()).isNull();
            assertThat(response.snapshot().scores()).hasSize(2);
            assertThat(response.snapshot().scores()).allSatisfy(s -> {
                assertThat(s.score()).isZero();
                assertThat(s.correctCount()).isZero();
            });
            assertThat(match.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Snapshot during a round includes the current question and answered flag")
        void snapshot_Success_InRound() {
            LiveRound round = attachActiveRound();
            round.putAnswer(new LiveAnswer(USER_A, 5001L, null, true, 1000, 140));

            MatchRejoinedResponse forA = engineService.snapshot(match, USER_A);
            MatchRejoinedResponse forB = engineService.snapshot(match, USER_B);

            assertThat(forA.snapshot().roundNo()).isEqualTo(1);
            assertThat(forA.snapshot().question()).isNotNull();
            assertThat(forA.snapshot().question().questionId()).isEqualTo(QUESTION_1_ID);
            assertThat(forA.snapshot().question().options()).hasSize(4);
            assertThat(forA.snapshot().answered()).isTrue();
            assertThat(forB.snapshot().question()).isNotNull();
            assertThat(forB.snapshot().answered()).isFalse();
        }

        @Test
        @DisplayName("Snapshot after a round ended has no question but includes the last round result")
        void snapshot_Success_RoundEnded() {
            LiveRound round = attachActiveRound();
            round.markEnded();
            match.addScore(USER_A, 140, true);
            RoundResultResponse lastResult = new RoundResultResponse(MATCH_ID, 1, "tao", List.of());
            match.setLastRoundResult(lastResult);

            MatchRejoinedResponse response = engineService.snapshot(match, USER_A);

            assertThat(response.snapshot().roundNo()).isEqualTo(1);
            assertThat(response.snapshot().question()).isNull();
            assertThat(response.snapshot().answered()).isFalse();
            assertThat(response.snapshot().lastRoundResult()).isEqualTo(lastResult);
            assertThat(response.snapshot().scores().get(0).userId()).isEqualTo(USER_A);
            assertThat(response.snapshot().scores().get(0).score()).isEqualTo(140);
            assertThat(response.snapshot().scores().get(0).correctCount()).isEqualTo(1);
        }
    }
}
