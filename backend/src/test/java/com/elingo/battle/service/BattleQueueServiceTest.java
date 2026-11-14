package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.QueueJoinedResponse;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleQueueServiceImpl;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleQueueServiceTest {

    @Mock
    private BattleEventPublisher eventPublisher;

    @Mock
    private BattleMatchService battleMatchService;

    @Mock
    private ThreadPoolTaskScheduler battleScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @InjectMocks
    private BattleQueueServiceImpl queueService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final BattleMode MODE = BattleMode.MULTIPLE_CHOICE;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(queueService, "battleProperties", PROPS);

        lenient().doReturn(scheduledFuture)
                .when(battleScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    // ========================================================================
    // join
    // ========================================================================
    @Nested
    @DisplayName("join")
    class JoinTests {

        @Test
        @DisplayName("Join queue successfully when no opponent is waiting")
        void join_Success_WaitingForOpponent() {
            Instant before = Instant.now();

            queueService.join(USER_A, MODE);

            assertThat(queueService.isUserInQueue(USER_A)).isTrue();
            verify(eventPublisher).sendToUser(USER_A, BattleEvents.QUEUE_JOINED, new QueueJoinedResponse(MODE));
            verify(battleMatchService, never()).createRandomMatch(anyLong(), anyLong(), any());

            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(battleScheduler).schedule(any(Runnable.class), instantCaptor.capture());
            long delayMs = Duration.between(before, instantCaptor.getValue()).toMillis();
            assertThat(delayMs).isBetween(PROPS.queueTimeoutMs(), PROPS.queueTimeoutMs() + 5000);
        }

        @Test
        @DisplayName("Join queue successfully and match with the waiting opponent")
        void join_Success_MatchedWithOpponent() {
            queueService.join(USER_A, MODE);
            queueService.join(USER_B, MODE);

            // Người vào sau là userA của createRandomMatch, người chờ trước là đối thủ
            verify(battleMatchService).createRandomMatch(USER_B, USER_A, MODE);
            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            assertThat(queueService.isUserInQueue(USER_B)).isFalse();
            // Timeout của người chờ trước phải bị hủy
            verify(scheduledFuture).cancel(false);
            // Người vào sau không được báo queue:joined vì ghép trận ngay
            verify(eventPublisher, never()).sendToUser(eq(USER_B), eq(BattleEvents.QUEUE_JOINED), any());
        }

        @Test
        @DisplayName("Join queue does not match users waiting in different modes")
        void join_Success_DifferentModesNotMatched() {
            queueService.join(USER_A, BattleMode.MULTIPLE_CHOICE);
            queueService.join(USER_B, BattleMode.TYPING);

            assertThat(queueService.isUserInQueue(USER_A)).isTrue();
            assertThat(queueService.isUserInQueue(USER_B)).isTrue();
            verify(battleMatchService, never()).createRandomMatch(anyLong(), anyLong(), any());
            verify(eventPublisher).sendToUser(USER_B, BattleEvents.QUEUE_JOINED,
                    new QueueJoinedResponse(BattleMode.TYPING));
        }

        @Test
        @DisplayName("Join queue is idempotent when user is already waiting")
        void join_Success_Idempotent() {
            queueService.join(USER_A, MODE);
            queueService.join(USER_A, MODE);

            verify(eventPublisher, times(2)).sendToUser(USER_A, BattleEvents.QUEUE_JOINED, new QueueJoinedResponse(MODE));
            // Chỉ lên lịch timeout một lần
            verify(battleScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
            verify(battleMatchService, never()).createRandomMatch(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Join queue again with another mode keeps the original waiting mode")
        void join_Success_RejoinWithOtherModeKeepsOriginalMode() {
            queueService.join(USER_A, BattleMode.MULTIPLE_CHOICE);
            queueService.join(USER_A, BattleMode.TYPING);

            verify(eventPublisher, times(2)).sendToUser(USER_A, BattleEvents.QUEUE_JOINED,
                    new QueueJoinedResponse(BattleMode.MULTIPLE_CHOICE));
            verify(eventPublisher, never()).sendToUser(USER_A, BattleEvents.QUEUE_JOINED,
                    new QueueJoinedResponse(BattleMode.TYPING));
            assertThat(queueService.isUserInQueue(USER_A)).isTrue();
        }

        @Test
        @DisplayName("Join queue failed: User is already in an active match")
        void join_Fail_AlreadyInActiveMatch() {
            when(battleMatchService.isUserInActiveMatch(USER_A)).thenReturn(true);

            assertThatThrownBy(() -> queueService.join(USER_A, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(battleScheduler);
        }

        @Test
        @DisplayName("Join queue sends error to both users when match creation fails with business error")
        void join_Fail_MatchCreationBusinessError() {
            doThrow(new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS))
                    .when(battleMatchService).createRandomMatch(USER_B, USER_A, MODE);

            queueService.join(USER_A, MODE);
            queueService.join(USER_B, MODE);

            verify(eventPublisher).sendError(USER_A, AppError.BATTLE_NOT_ENOUGH_CARDS.getCode(),
                    AppError.BATTLE_NOT_ENOUGH_CARDS.getMessage());
            verify(eventPublisher).sendError(USER_B, AppError.BATTLE_NOT_ENOUGH_CARDS.getCode(),
                    AppError.BATTLE_NOT_ENOUGH_CARDS.getMessage());
            // Hai người đã bị gỡ khỏi hàng đợi
            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            assertThat(queueService.isUserInQueue(USER_B)).isFalse();
        }

        @Test
        @DisplayName("Join queue sends internal error to both users when match creation fails unexpectedly")
        void join_Fail_MatchCreationUnexpectedError() {
            doThrow(new IllegalStateException("boom"))
                    .when(battleMatchService).createRandomMatch(USER_B, USER_A, MODE);

            queueService.join(USER_A, MODE);
            queueService.join(USER_B, MODE);

            verify(eventPublisher).sendError(USER_A, AppError.INTERNAL_SERVER_ERROR.getCode(),
                    AppError.INTERNAL_SERVER_ERROR.getMessage());
            verify(eventPublisher).sendError(USER_B, AppError.INTERNAL_SERVER_ERROR.getCode(),
                    AppError.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    // ========================================================================
    // leave
    // ========================================================================
    @Nested
    @DisplayName("leave")
    class LeaveTests {

        @Test
        @DisplayName("Leave queue successfully")
        void leave_Success() {
            queueService.join(USER_A, MODE);

            queueService.leave(USER_A);

            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            verify(scheduledFuture).cancel(false);
            verify(eventPublisher).sendToUser(USER_A, BattleEvents.QUEUE_LEFT, Collections.emptyMap());
        }

        @Test
        @DisplayName("Leave queue does nothing when user is not waiting")
        void leave_Success_NotInQueue() {
            queueService.leave(USER_A);

            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(scheduledFuture);
        }

        @Test
        @DisplayName("Leave queue removes user so later players are not matched with them")
        void leave_Success_NotMatchedAfterLeaving() {
            queueService.join(USER_A, MODE);
            queueService.leave(USER_A);

            queueService.join(USER_B, MODE);

            verify(battleMatchService, never()).createRandomMatch(anyLong(), anyLong(), any());
            assertThat(queueService.isUserInQueue(USER_B)).isTrue();
        }
    }

    // ========================================================================
    // removeOnDisconnect
    // ========================================================================
    @Nested
    @DisplayName("removeOnDisconnect")
    class RemoveOnDisconnectTests {

        @Test
        @DisplayName("Remove user from queue on disconnect without notifying them")
        void removeOnDisconnect_Success() {
            queueService.join(USER_A, MODE);

            queueService.removeOnDisconnect(USER_A);

            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            verify(scheduledFuture).cancel(false);
            verify(eventPublisher, never()).sendToUser(eq(USER_A), eq(BattleEvents.QUEUE_LEFT), any());
        }

        @Test
        @DisplayName("Remove on disconnect does nothing when user is not waiting")
        void removeOnDisconnect_Success_NotInQueue() {
            queueService.removeOnDisconnect(USER_A);

            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(battleScheduler);
        }
    }

    // ========================================================================
    // isUserInQueue
    // ========================================================================
    @Nested
    @DisplayName("isUserInQueue")
    class IsUserInQueueTests {

        @Test
        @DisplayName("User is not in queue by default")
        void isUserInQueue_False() {
            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
        }

        @Test
        @DisplayName("User is in queue after joining")
        void isUserInQueue_True() {
            queueService.join(USER_A, MODE);

            assertThat(queueService.isUserInQueue(USER_A)).isTrue();
        }
    }

    // ========================================================================
    // queue timeout
    // ========================================================================
    @Nested
    @DisplayName("queue timeout")
    class QueueTimeoutTests {

        private Runnable captureTimeoutTask() {
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(battleScheduler).schedule(captor.capture(), any(Instant.class));
            return captor.getValue();
        }

        @Test
        @DisplayName("Queue timeout removes waiting user and notifies them")
        void timeout_Success() {
            queueService.join(USER_A, MODE);
            Runnable timeoutTask = captureTimeoutTask();

            timeoutTask.run();

            assertThat(queueService.isUserInQueue(USER_A)).isFalse();
            verify(eventPublisher).sendToUser(USER_A, BattleEvents.QUEUE_TIMEOUT, Collections.emptyMap());
        }

        @Test
        @DisplayName("Queue timeout does nothing when user has already left")
        void timeout_Success_UserAlreadyLeft() {
            queueService.join(USER_A, MODE);
            Runnable timeoutTask = captureTimeoutTask();
            queueService.leave(USER_A);

            timeoutTask.run();

            verify(eventPublisher, never()).sendToUser(eq(USER_A), eq(BattleEvents.QUEUE_TIMEOUT), any());
        }

        @Test
        @DisplayName("Queue timeout does not notify user who has been matched")
        void timeout_Success_UserAlreadyMatched() {
            queueService.join(USER_A, MODE);
            Runnable timeoutTask = captureTimeoutTask();
            queueService.join(USER_B, MODE);

            timeoutTask.run();

            verify(eventPublisher, never()).sendToUser(eq(USER_A), eq(BattleEvents.QUEUE_TIMEOUT), any());
            verify(battleMatchService).createRandomMatch(USER_B, USER_A, MODE);
        }

        @Test
        @DisplayName("User can join queue again after timeout")
        void timeout_Success_CanJoinAgain() {
            queueService.join(USER_A, MODE);
            Runnable timeoutTask = captureTimeoutTask();
            timeoutTask.run();

            queueService.join(USER_A, MODE);

            assertThat(queueService.isUserInQueue(USER_A)).isTrue();
            verify(eventPublisher, times(2)).sendToUser(USER_A, BattleEvents.QUEUE_JOINED, new QueueJoinedResponse(MODE));
        }
    }
}
