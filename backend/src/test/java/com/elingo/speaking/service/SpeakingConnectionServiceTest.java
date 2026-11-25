package com.elingo.speaking.service;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.response.PeerDisconnectedResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.event.SpeakingMemberRemovedEvent;
import com.elingo.speaking.service.impl.SpeakingConnectionServiceImpl;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingConnectionServiceTest {

    @Mock
    private SpeakingLiveRegistry registry;

    @Mock
    private SpeakingEventPublisher publisher;

    @Mock
    private SpeakingRoomService roomService;

    @Mock
    private ThreadPoolTaskScheduler speakingScheduler;

    @Mock
    private ScheduledFuture<?> graceFuture;

    private SpeakingConnectionServiceImpl connectionService;

    private static final String ROOM_CODE = "ABC234";
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long OTHER_GUEST_ID = 3L;

    private static final SpeakingProperties PROPS = new SpeakingProperties(
            4, 4, 16384, List.of("stun:example.org:3478"), 15000L, 30000L, 600000L, 60000L);

    private LiveSpeakingRoom room;

    @BeforeEach
    void setUp() {
        // Constructor có @Qualifier nên dựng trực tiếp thay vì @InjectMocks
        connectionService = new SpeakingConnectionServiceImpl(registry, publisher, roomService, PROPS, speakingScheduler);

        lenient().doReturn(graceFuture)
                .when(speakingScheduler).schedule(any(Runnable.class), any(Instant.class));

        room = new LiveSpeakingRoom(ROOM_CODE, 100L, HOST_ID, "Topic", (short) 4, false, 1000L);
    }

    private LivePeer addPeer(Long userId, ParticipantRole role) {
        LivePeer peer = new LivePeer(userId, "User " + userId, null, role);
        room.putPeer(peer);
        return peer;
    }

    private Runnable captureGraceTask() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(speakingScheduler).schedule(captor.capture(), any(Instant.class));
        return captor.getValue();
    }

    private long delayOfSchedule(int index, Instant before) {
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(speakingScheduler, atLeast(index + 1)).schedule(any(Runnable.class), captor.capture());
        return Duration.between(before, captor.getAllValues().get(index)).toMillis();
    }

    // ========================================================================
    // onUserOffline
    // ========================================================================
    @Nested
    @DisplayName("onUserOffline")
    class OnUserOfflineTests {

        @Test
        @DisplayName("Guest going offline starts the reconnect grace and notifies the other online peers")
        void onUserOffline_Success_Guest() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            addPeer(OTHER_GUEST_ID, ParticipantRole.GUEST);
            when(registry.getByUser(GUEST_ID)).thenReturn(room);
            Instant before = Instant.now();

            connectionService.onUserOffline(GUEST_ID);

            assertThat(room.isOnline(GUEST_ID)).isFalse();
            assertThat(room.getGraceFutures()).containsKey(GUEST_ID);
            PeerDisconnectedResponse expected = new PeerDisconnectedResponse(GUEST_ID, PROPS.reconnectGraceMs());
            verify(publisher).sendToUser(HOST_ID, SpeakingEvents.PEER_DISCONNECTED, expected);
            verify(publisher).sendToUser(OTHER_GUEST_ID, SpeakingEvents.PEER_DISCONNECTED, expected);
            verify(publisher, never()).sendToUser(eq(GUEST_ID), any(String.class), any());

            long delayMs = delayOfSchedule(0, before);
            assertThat(delayMs).isBetween(PROPS.reconnectGraceMs(), PROPS.reconnectGraceMs() + 5000);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Host going offline uses the longer host reconnect grace")
        void onUserOffline_Success_Host() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(registry.getByUser(HOST_ID)).thenReturn(room);
            Instant before = Instant.now();

            connectionService.onUserOffline(HOST_ID);

            verify(publisher).sendToUser(GUEST_ID, SpeakingEvents.PEER_DISCONNECTED,
                    new PeerDisconnectedResponse(HOST_ID, PROPS.hostReconnectGraceMs()));
            long delayMs = delayOfSchedule(0, before);
            assertThat(delayMs).isBetween(PROPS.hostReconnectGraceMs(), PROPS.hostReconnectGraceMs() + 5000);
        }

        @Test
        @DisplayName("Host going offline while alone shortens the wait to the regular grace")
        void onUserOffline_Success_HostAloneShortensGrace() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            when(registry.getByUser(HOST_ID)).thenReturn(room);
            ScheduledFuture<?> hostGrace = org.mockito.Mockito.mock(ScheduledFuture.class);
            ScheduledFuture<?> shortGrace = org.mockito.Mockito.mock(ScheduledFuture.class);
            doReturn2(hostGrace, shortGrace);
            Instant before = Instant.now();

            connectionService.onUserOffline(HOST_ID);

            verify(hostGrace).cancel(false);
            assertThat(room.getGraceFutures()).containsEntry(HOST_ID, shortGrace);
            assertThat(delayOfSchedule(0, before)).isGreaterThanOrEqualTo(PROPS.hostReconnectGraceMs());
            assertThat(delayOfSchedule(1, before)).isBetween(PROPS.reconnectGraceMs(), PROPS.reconnectGraceMs() + 5000);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Guest going offline while alone with an online host does not shorten any grace")
        void onUserOffline_Success_GuestDoesNotShortenGrace() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(registry.getByUser(GUEST_ID)).thenReturn(room);

            connectionService.onUserOffline(GUEST_ID);

            verify(speakingScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        @DisplayName("User offline does nothing when the user is not in any room")
        void onUserOffline_Success_NoRoom() {
            when(registry.getByUser(GUEST_ID)).thenReturn(null);

            connectionService.onUserOffline(GUEST_ID);

            verifyNoInteractions(publisher, speakingScheduler);
        }

        @Test
        @DisplayName("User offline does nothing when the user is no longer a peer of the room")
        void onUserOffline_Success_PeerMissing() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            when(registry.getByUser(GUEST_ID)).thenReturn(room);

            connectionService.onUserOffline(GUEST_ID);

            verifyNoInteractions(publisher, speakingScheduler);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("User offline does nothing when the peer is already offline")
        void onUserOffline_Success_AlreadyOffline() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST).setOnline(false);
            when(registry.getByUser(GUEST_ID)).thenReturn(room);

            connectionService.onUserOffline(GUEST_ID);

            verifyNoInteractions(publisher, speakingScheduler);
            assertThat(room.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // grace expiry
    // ========================================================================
    @Nested
    @DisplayName("grace expiry")
    class GraceExpiryTests {

        private Runnable goOfflineAndCaptureGraceTask() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(registry.getByUser(GUEST_ID)).thenReturn(room);
            connectionService.onUserOffline(GUEST_ID);
            return captureGraceTask();
        }

        @Test
        @DisplayName("Grace expiry removes the member with the TIMEOUT reason when they are still offline")
        void graceExpired_Success() {
            Runnable task = goOfflineAndCaptureGraceTask();
            when(registry.get(ROOM_CODE)).thenReturn(room);

            task.run();

            verify(roomService).removeMember(GUEST_ID, ROOM_CODE, "TIMEOUT");
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Grace expiry keeps the member who came back online")
        void graceExpired_Success_BackOnline() {
            Runnable task = goOfflineAndCaptureGraceTask();
            room.getPeer(GUEST_ID).setOnline(true);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            task.run();

            verify(roomService, never()).removeMember(any(), any(), any());
        }

        @Test
        @DisplayName("Grace expiry does nothing when the member already left the room")
        void graceExpired_Success_PeerAlreadyGone() {
            Runnable task = goOfflineAndCaptureGraceTask();
            room.removePeer(GUEST_ID);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            task.run();

            verify(roomService, never()).removeMember(any(), any(), any());
        }

        @Test
        @DisplayName("Grace expiry does nothing when the room no longer exists")
        void graceExpired_Success_RoomGone() {
            Runnable task = goOfflineAndCaptureGraceTask();
            when(registry.get(ROOM_CODE)).thenReturn(null);

            task.run();

            verify(roomService, never()).removeMember(any(), any(), any());
        }

        @Test
        @DisplayName("Grace expiry swallows a failure while removing the member")
        void graceExpired_Success_FailureIsSwallowed() {
            Runnable task = goOfflineAndCaptureGraceTask();
            when(registry.get(ROOM_CODE)).thenReturn(room);
            doThrow(new IllegalStateException("db down")).when(roomService).removeMember(GUEST_ID, ROOM_CODE, "TIMEOUT");

            assertThatCode(task::run).doesNotThrowAnyException();

            assertThat(room.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // onMemberRemoved
    // ========================================================================
    @Nested
    @DisplayName("onMemberRemoved")
    class OnMemberRemovedTests {

        @Test
        @DisplayName("Member removed shortens the wait when only an offline host remains")
        void onMemberRemoved_Success_ShortensHostGrace() {
            addPeer(HOST_ID, ParticipantRole.HOST).setOnline(false);
            when(registry.get(ROOM_CODE)).thenReturn(room);
            Instant before = Instant.now();

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            assertThat(room.getGraceFutures()).containsEntry(HOST_ID, graceFuture);
            long delayMs = delayOfSchedule(0, before);
            assertThat(delayMs).isBetween(PROPS.reconnectGraceMs(), PROPS.reconnectGraceMs() + 5000);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Member removed replaces and cancels the previous host grace")
        void onMemberRemoved_Success_CancelsPreviousGrace() {
            addPeer(HOST_ID, ParticipantRole.HOST).setOnline(false);
            ScheduledFuture<?> previous = org.mockito.Mockito.mock(ScheduledFuture.class);
            room.putGrace(HOST_ID, previous);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verify(previous).cancel(false);
            assertThat(room.getGraceFutures()).containsEntry(HOST_ID, graceFuture);
        }

        @Test
        @DisplayName("Member removed does nothing when the room is not in memory")
        void onMemberRemoved_Success_NoRoom() {
            when(registry.get(ROOM_CODE)).thenReturn(null);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verifyNoInteractions(speakingScheduler);
        }

        @Test
        @DisplayName("Member removed does nothing while the host is online")
        void onMemberRemoved_Success_HostOnline() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verifyNoInteractions(speakingScheduler);
        }

        @Test
        @DisplayName("Member removed does nothing while other peers are still in the room")
        void onMemberRemoved_Success_OtherPeersRemain() {
            addPeer(HOST_ID, ParticipantRole.HOST).setOnline(false);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verifyNoInteractions(speakingScheduler);
        }

        @Test
        @DisplayName("Member removed does nothing when the host is not a peer of the room")
        void onMemberRemoved_Success_HostMissing() {
            addPeer(GUEST_ID, ParticipantRole.GUEST).setOnline(false);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verifyNoInteractions(speakingScheduler);
        }

        @Test
        @DisplayName("Member removed does nothing when the room has no host id")
        void onMemberRemoved_Success_NoHostId() {
            addPeer(GUEST_ID, ParticipantRole.GUEST).setOnline(false);
            room.setHostId(null);
            when(registry.get(ROOM_CODE)).thenReturn(room);

            connectionService.onMemberRemoved(new SpeakingMemberRemovedEvent(ROOM_CODE));

            verifyNoInteractions(speakingScheduler);
        }
    }

    // Trả lần lượt hai future cho hai lần schedule liên tiếp (ghi đè stub lenient mặc định)
    private void doReturn2(ScheduledFuture<?> first, ScheduledFuture<?> second) {
        org.mockito.Mockito.doReturn(first).doReturn(second)
                .when(speakingScheduler).schedule(any(Runnable.class), any(Instant.class));
    }
}
