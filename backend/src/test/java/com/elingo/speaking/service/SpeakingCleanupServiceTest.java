package com.elingo.speaking.service;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.service.impl.SpeakingCleanupServiceImpl;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingCleanupServiceTest {

    @Mock
    private SpeakingRoomPersistenceService persistence;

    @Mock
    private SpeakingLiveRegistry registry;

    @InjectMocks
    private SpeakingCleanupServiceImpl cleanupService;

    private static final String CODE_A = "AAAAAA";
    private static final String CODE_B = "BBBBBB";
    private static final String CODE_C = "CCCCCC";
    private static final Long USER_ID = 1L;

    private static final SpeakingProperties PROPS = new SpeakingProperties(
            4, 4, 16384, List.of("stun:example.org:3478"), 15000L, 30000L, 600000L, 60000L);

    @BeforeEach
    void setUp() {
        // SpeakingProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(cleanupService, "props", PROPS);
    }

    private LiveSpeakingRoom liveRoom(String code) {
        return new LiveSpeakingRoom(code, 1L, USER_ID, "Topic", (short) 4, false, 1000L);
    }

    private LiveSpeakingRoom liveRoomWithPeer(String code) {
        LiveSpeakingRoom room = liveRoom(code);
        room.putPeer(new LivePeer(USER_ID, "User", null, ParticipantRole.HOST));
        return room;
    }

    // ========================================================================
    // cleanStaleRooms
    // ========================================================================
    @Nested
    @DisplayName("cleanStaleRooms")
    class CleanStaleRoomsTests {

        @Test
        @DisplayName("Clean stale rooms closes a stale room that is not in memory")
        void cleanStaleRooms_Success_NotInMemory() {
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of(CODE_A));
            when(registry.get(CODE_A)).thenReturn(null);
            when(persistence.forceCloseRoom(CODE_A)).thenReturn(true);

            int closed = cleanupService.cleanStaleRooms();

            assertThat(closed).isEqualTo(1);
            verify(persistence).forceCloseRoom(CODE_A);
            verify(registry, never()).removeRoom(any());
        }

        @Test
        @DisplayName("Clean stale rooms skips a room that still has peers online or waiting to reconnect")
        void cleanStaleRooms_Success_SkipsRoomWithPeers() {
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of(CODE_A));
            when(registry.get(CODE_A)).thenReturn(liveRoomWithPeer(CODE_A));

            int closed = cleanupService.cleanStaleRooms();

            assertThat(closed).isZero();
            verify(persistence, never()).forceCloseRoom(anyString());
            verify(registry, never()).removeRoom(any());
        }

        @Test
        @DisplayName("Clean stale rooms drops an empty room from memory and cancels its grace timers")
        void cleanStaleRooms_Success_DropsEmptyLiveRoom() {
            LiveSpeakingRoom room = liveRoom(CODE_A);
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            room.putGrace(USER_ID, grace);
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of(CODE_A));
            when(registry.get(CODE_A)).thenReturn(room);
            when(persistence.forceCloseRoom(CODE_A)).thenReturn(true);

            int closed = cleanupService.cleanStaleRooms();

            assertThat(closed).isEqualTo(1);
            verify(grace).cancel(false);
            verify(registry).removeRoom(room);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Clean stale rooms does not count or drop a room that could not be closed")
        void cleanStaleRooms_Success_ForceCloseFailed() {
            LiveSpeakingRoom room = liveRoom(CODE_A);
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of(CODE_A));
            when(registry.get(CODE_A)).thenReturn(room);
            when(persistence.forceCloseRoom(CODE_A)).thenReturn(false);

            int closed = cleanupService.cleanStaleRooms();

            assertThat(closed).isZero();
            verify(registry, never()).removeRoom(any());
        }

        @Test
        @DisplayName("Clean stale rooms only counts the rooms that were really closed")
        void cleanStaleRooms_Success_MixedRooms() {
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class)))
                    .thenReturn(List.of(CODE_A, CODE_B, CODE_C));
            when(registry.get(CODE_A)).thenReturn(liveRoomWithPeer(CODE_A));
            when(registry.get(CODE_B)).thenReturn(null);
            when(registry.get(CODE_C)).thenReturn(null);
            when(persistence.forceCloseRoom(CODE_B)).thenReturn(true);
            when(persistence.forceCloseRoom(CODE_C)).thenReturn(false);

            int closed = cleanupService.cleanStaleRooms();

            assertThat(closed).isEqualTo(1);
            verify(persistence, never()).forceCloseRoom(CODE_A);
        }

        @Test
        @DisplayName("Clean stale rooms returns zero when there are no stale rooms")
        void cleanStaleRooms_Success_NoStaleRooms() {
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of());

            assertThat(cleanupService.cleanStaleRooms()).isZero();

            verify(persistence, never()).forceCloseRoom(anyString());
        }

        @Test
        @DisplayName("Clean stale rooms looks for rooms older than the waiting room time to live")
        void cleanStaleRooms_Success_UsesTtlCutoff() {
            when(persistence.findStaleRoomCodes(any(LocalDateTime.class))).thenReturn(List.of());
            LocalDateTime expected = LocalDateTime.now().minus(Duration.ofMillis(PROPS.waitingRoomTtlMs()));

            cleanupService.cleanStaleRooms();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(persistence).findStaleRoomCodes(captor.capture());
            assertThat(captor.getValue()).isCloseTo(expected, within(5, java.time.temporal.ChronoUnit.SECONDS));
        }
    }

    // ========================================================================
    // cleanAllOnStartup
    // ========================================================================
    @Nested
    @DisplayName("cleanAllOnStartup")
    class CleanAllOnStartupTests {

        @Test
        @DisplayName("Clean all on startup returns the number of rooms that were closed")
        void cleanAllOnStartup_Success() {
            when(persistence.closeAllOpenRooms()).thenReturn(5);

            assertThat(cleanupService.cleanAllOnStartup()).isEqualTo(5);

            verify(persistence).closeAllOpenRooms();
        }

        @Test
        @DisplayName("Clean all on startup returns zero when there are no open rooms")
        void cleanAllOnStartup_Success_NothingToClose() {
            when(persistence.closeAllOpenRooms()).thenReturn(0);

            assertThat(cleanupService.cleanAllOnStartup()).isZero();
        }
    }
}
