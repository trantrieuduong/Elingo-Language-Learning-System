package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.ActiveMatchResponse;
import com.elingo.battle.dto.response.MatchRejoinedResponse;
import com.elingo.battle.dto.response.MatchSnapshotResponse;
import com.elingo.battle.dto.response.OpponentDisconnectedResponse;
import com.elingo.battle.dto.response.OpponentReconnectedResponse;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleConnectionServiceImpl;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveMatch;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleConnectionServiceTest {

    @Mock
    private BattleLiveRegistry registry;

    @Mock
    private BattleEngineService engineService;

    @Mock
    private BattleEventPublisher publisher;

    @Mock
    private ThreadPoolTaskScheduler battleScheduler;

    @Mock
    private ScheduledFuture<?> graceFuture;

    @InjectMocks
    private BattleConnectionServiceImpl connectionService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long MATCH_ID = 100L;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    private LiveMatch liveMatch;

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(connectionService, "props", PROPS);

        lenient().doReturn(graceFuture)
                .when(battleScheduler).schedule(any(Runnable.class), any(Instant.class));

        liveMatch = new LiveMatch(MATCH_ID, BattleMode.MULTIPLE_CHOICE, BattleMatchType.RANDOM, 10,
                USER_A, USER_B, Map.of(USER_A, 11L, USER_B, 12L), List.of());
    }

    private Runnable captureGraceTask() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(battleScheduler).schedule(captor.capture(), any(Instant.class));
        return captor.getValue();
    }

    // ========================================================================
    // onUserOffline
    // ========================================================================
    @Nested
    @DisplayName("onUserOffline")
    class OnUserOfflineTests {

        @Test
        @DisplayName("User offline starts grace period and notifies opponent")
        void onUserOffline_Success() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            Instant before = Instant.now();

            connectionService.onUserOffline(USER_A);

            assertThat(liveMatch.isDisconnected(USER_A)).isTrue();
            assertThat(liveMatch.isDisconnected(USER_B)).isFalse();
            verify(publisher).sendToUser(USER_B, BattleEvents.MATCH_OPPONENT_DISCONNECTED,
                    new OpponentDisconnectedResponse(MATCH_ID, PROPS.reconnectGraceMs()));

            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(battleScheduler).schedule(any(Runnable.class), instantCaptor.capture());
            long delayMs = Duration.between(before, instantCaptor.getValue()).toMillis();
            assertThat(delayMs).isBetween(PROPS.reconnectGraceMs(), PROPS.reconnectGraceMs() + 5000);
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("User offline does nothing when user is not in any match")
        void onUserOffline_Success_NoMatch() {
            when(registry.getByUser(USER_A)).thenReturn(null);

            connectionService.onUserOffline(USER_A);

            verifyNoInteractions(publisher);
            verifyNoInteractions(battleScheduler);
        }

        @Test
        @DisplayName("User offline does nothing when match is not in progress")
        void onUserOffline_Success_MatchNotInProgress() {
            liveMatch.setStatus(BattleMatchStatus.FINISHED);
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            connectionService.onUserOffline(USER_A);

            assertThat(liveMatch.isDisconnected(USER_A)).isFalse();
            verifyNoInteractions(publisher);
            verifyNoInteractions(battleScheduler);
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("User offline is ignored when user is already marked as disconnected")
        void onUserOffline_Success_AlreadyDisconnected() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            connectionService.onUserOffline(USER_A);
            connectionService.onUserOffline(USER_A);

            verify(publisher, times(1)).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_OPPONENT_DISCONNECTED), any());
            verify(battleScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    // ========================================================================
    // grace period expired
    // ========================================================================
    @Nested
    @DisplayName("grace period expired")
    class GraceExpiredTests {

        @Test
        @DisplayName("Forfeit the disconnected user when grace period expires")
        void graceExpired_Success_Forfeit() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            connectionService.onUserOffline(USER_A);
            Runnable graceTask = captureGraceTask();

            graceTask.run();

            verify(engineService).forfeit(liveMatch, USER_A);
            verify(engineService, never()).cancel(any(), any());
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Cancel the match when both users are disconnected and grace period expires")
        void graceExpired_Success_BothDisconnected() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            when(registry.getByUser(USER_B)).thenReturn(liveMatch);
            connectionService.onUserOffline(USER_A);
            connectionService.onUserOffline(USER_B);

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(battleScheduler, times(2)).schedule(captor.capture(), any(Instant.class));
            Runnable graceTaskOfA = captor.getAllValues().get(0);

            graceTaskOfA.run();

            verify(engineService).cancel(liveMatch, "BOTH_DISCONNECTED");
            verify(engineService, never()).forfeit(any(), any());
        }

        @Test
        @DisplayName("Grace expiry does nothing when user has reconnected")
        void graceExpired_Success_UserReconnected() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            connectionService.onUserOffline(USER_A);
            Runnable graceTask = captureGraceTask();
            connectionService.rejoin(USER_A, MATCH_ID);

            graceTask.run();

            verify(engineService, never()).forfeit(any(), any());
            verify(engineService, never()).cancel(any(), any());
        }

        @Test
        @DisplayName("Grace expiry does nothing when match is no longer in progress")
        void graceExpired_Success_MatchAlreadyEnded() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            connectionService.onUserOffline(USER_A);
            Runnable graceTask = captureGraceTask();
            liveMatch.setStatus(BattleMatchStatus.FINISHED);

            graceTask.run();

            verify(engineService, never()).forfeit(any(), any());
            verify(engineService, never()).cancel(any(), any());
        }

        @Test
        @DisplayName("Grace expiry cancels the match when forfeit fails unexpectedly")
        void graceExpired_Fail_ForfeitError() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            connectionService.onUserOffline(USER_A);
            Runnable graceTask = captureGraceTask();
            doThrow(new IllegalStateException("boom")).when(engineService).forfeit(liveMatch, USER_A);

            graceTask.run();

            verify(engineService).cancel(liveMatch, "INTERNAL_ERROR");
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // rejoin
    // ========================================================================
    @Nested
    @DisplayName("rejoin")
    class RejoinTests {

        private MatchRejoinedResponse buildRejoinedResponse() {
            return new MatchRejoinedResponse(new MatchSnapshotResponse(
                    MATCH_ID, BattleMode.MULTIPLE_CHOICE, BattleMatchType.RANDOM, 10,
                    List.of(), List.of(), 0, null, false, null));
        }

        @Test
        @DisplayName("Rejoin successfully after being disconnected")
        void rejoin_Success_AfterDisconnect() {
            MatchRejoinedResponse rejoined = buildRejoinedResponse();
            liveMatch.markDisconnected(USER_A);
            liveMatch.putGrace(USER_A, graceFuture);
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            when(engineService.snapshot(liveMatch, USER_A)).thenReturn(rejoined);

            connectionService.rejoin(USER_A, MATCH_ID);

            assertThat(liveMatch.isDisconnected(USER_A)).isFalse();
            // Hủy bộ đếm forfeit
            verify(graceFuture).cancel(false);
            verify(publisher).sendToUser(USER_A, BattleEvents.MATCH_REJOINED, rejoined);
            verify(publisher).sendToUser(USER_B, BattleEvents.MATCH_OPPONENT_RECONNECTED,
                    new OpponentReconnectedResponse(MATCH_ID));
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Rejoin successfully without notifying opponent when user was not marked disconnected")
        void rejoin_Success_NotDisconnected() {
            MatchRejoinedResponse rejoined = buildRejoinedResponse();
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);
            when(engineService.snapshot(liveMatch, USER_A)).thenReturn(rejoined);

            connectionService.rejoin(USER_A, MATCH_ID);

            verify(publisher).sendToUser(USER_A, BattleEvents.MATCH_REJOINED, rejoined);
            verify(publisher, never()).sendToUser(eq(USER_B), eq(BattleEvents.MATCH_OPPONENT_RECONNECTED), any());
        }

        @Test
        @DisplayName("Rejoin failed: User is not in any match")
        void rejoin_Fail_NoMatch() {
            when(registry.getByUser(USER_A)).thenReturn(null);

            assertThatThrownBy(() -> connectionService.rejoin(USER_A, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verifyNoInteractions(publisher);
            verifyNoInteractions(engineService);
        }

        @Test
        @DisplayName("Rejoin failed: Match id does not match the user's current match")
        void rejoin_Fail_MatchIdMismatch() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            assertThatThrownBy(() -> connectionService.rejoin(USER_A, 999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verifyNoInteractions(publisher);
            verifyNoInteractions(engineService);
        }

        @Test
        @DisplayName("Rejoin failed: Match is not in progress and lock is released")
        void rejoin_Fail_MatchNotInProgress() {
            liveMatch.setStatus(BattleMatchStatus.CANCELLED);
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            assertThatThrownBy(() -> connectionService.rejoin(USER_A, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verifyNoInteractions(publisher);
            assertThat(liveMatch.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // sendActiveMatch
    // ========================================================================
    @Nested
    @DisplayName("sendActiveMatch")
    class SendActiveMatchTests {

        @Test
        @DisplayName("Send active match when user is in a match in progress")
        void sendActiveMatch_Success_InProgress() {
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            connectionService.sendActiveMatch(USER_A);

            verify(publisher).sendToUser(USER_A, BattleEvents.ACTIVE_MATCH, new ActiveMatchResponse(MATCH_ID, "IN_PROGRESS"));
        }

        @Test
        @DisplayName("Send NONE when user is not in any match")
        void sendActiveMatch_Success_NoMatch() {
            when(registry.getByUser(USER_A)).thenReturn(null);

            connectionService.sendActiveMatch(USER_A);

            verify(publisher).sendToUser(USER_A, BattleEvents.ACTIVE_MATCH, new ActiveMatchResponse(null, "NONE"));
        }

        @Test
        @DisplayName("Send NONE when the match is no longer in progress")
        void sendActiveMatch_Success_MatchEnded() {
            liveMatch.setStatus(BattleMatchStatus.FINISHED);
            when(registry.getByUser(USER_A)).thenReturn(liveMatch);

            connectionService.sendActiveMatch(USER_A);

            verify(publisher).sendToUser(USER_A, BattleEvents.ACTIVE_MATCH, new ActiveMatchResponse(null, "NONE"));
        }
    }
}
