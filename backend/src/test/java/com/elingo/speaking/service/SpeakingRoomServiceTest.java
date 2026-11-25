package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.ActiveRoomResponse;
import com.elingo.speaking.dto.response.BanRemovedResponse;
import com.elingo.speaking.dto.response.HostChangedResponse;
import com.elingo.speaking.dto.response.PeerLeftResponse;
import com.elingo.speaking.dto.response.RoomClosedResponse;
import com.elingo.speaking.dto.response.RoomCreatedResponse;
import com.elingo.speaking.dto.response.RoomKickedResponse;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.event.SpeakingMemberRemovedEvent;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.EndResult;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.LeaveResult;
import com.elingo.speaking.service.impl.SpeakingRoomServiceImpl;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingRoomServiceTest {

    @Mock
    private SpeakingRoomPersistenceService persistence;

    @Mock
    private SpeakingLiveRegistry registry;

    @Mock
    private SpeakingEventPublisher publisher;

    @Mock
    private SpeakingSignalingService signalingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SpeakingRoomServiceImpl roomService;

    private static final String ROOM_CODE = "ABC234";
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long OTHER_ID = 3L;

    private LiveSpeakingRoom liveRoom;

    @BeforeEach
    void setUp() {
        liveRoom = new LiveSpeakingRoom(ROOM_CODE, 100L, HOST_ID, "Topic", (short) 4, false, 1000L);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private LivePeer addPeer(Long userId, ParticipantRole role) {
        LivePeer peer = new LivePeer(userId, "User " + userId, null, role);
        liveRoom.putPeer(peer);
        return peer;
    }

    private SpeakingRoomDetailResponse detail(Long hostId) {
        return new SpeakingRoomDetailResponse(ROOM_CODE, "Topic", (short) 4, SpeakingRoomStatus.ACTIVE,
                false, hostId, null, LocalDateTime.now(), List.of());
    }

    private LeaveResult leaveResult(Long newHostId, boolean roomClosed) {
        return new LeaveResult(ROOM_CODE, "Topic", 90L, newHostId, roomClosed);
    }

    private void sentTo(Long userId, String event, Object payload) {
        verify(publisher).sendToUser(userId, event, payload);
    }

    private void notSentTo(Long userId, String event) {
        verify(publisher, never()).sendToUser(eq(userId), eq(event), any());
    }

    private static void assertAppError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, AppError expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("appError", expected);
    }

    // ========================================================================
    // createRoom
    // ========================================================================
    @Nested
    @DisplayName("createRoom")
    class CreateRoomTests {

        @Test
        @DisplayName("Create room persists the room and tells the creator the room code")
        void createRoom_Success() {
            when(persistence.createRoom(HOST_ID, "Travel", 3, true)).thenReturn(detail(HOST_ID));

            roomService.createRoom(HOST_ID, new CreateSpeakingRoomRequest("Travel", 3, true));

            sentTo(HOST_ID, SpeakingEvents.ROOM_CREATED, new RoomCreatedResponse(ROOM_CODE));
        }

        @Test
        @DisplayName("Create room passes optional fields through as null")
        void createRoom_Success_OptionalFieldsNull() {
            when(persistence.createRoom(HOST_ID, null, null, null)).thenReturn(detail(HOST_ID));

            roomService.createRoom(HOST_ID, new CreateSpeakingRoomRequest(null, null, null));

            verify(persistence).createRoom(HOST_ID, null, null, null);
            sentTo(HOST_ID, SpeakingEvents.ROOM_CREATED, new RoomCreatedResponse(ROOM_CODE));
        }

        @Test
        @DisplayName("Create room failed: Persistence rejects the request")
        void createRoom_Fail_PersistenceError() {
            when(persistence.createRoom(HOST_ID, "Travel", 3, true))
                    .thenThrow(new AppException(AppError.SPEAKING_ALREADY_IN_ROOM));

            assertAppError(() -> roomService.createRoom(HOST_ID, new CreateSpeakingRoomRequest("Travel", 3, true)),
                    AppError.SPEAKING_ALREADY_IN_ROOM);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Create room can be called repeatedly by the same user")
        void createRoom_Success_RepeatedCalls() {
            when(persistence.createRoom(HOST_ID, "Travel", 3, true)).thenReturn(detail(HOST_ID));
            CreateSpeakingRoomRequest request = new CreateSpeakingRoomRequest("Travel", 3, true);

            assertThatCode(() -> {
                roomService.createRoom(HOST_ID, request);
                roomService.createRoom(HOST_ID, request);
            }).doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // joinRoom
    // ========================================================================
    @Nested
    @DisplayName("joinRoom")
    class JoinRoomTests {

        private GoOnlineContext context() {
            SpeakingPeerResponse me = new SpeakingPeerResponse(GUEST_ID, "User 2", null, ParticipantRole.GUEST);
            return new GoOnlineContext(100L, ROOM_CODE, "Topic", HOST_ID, (short) 4, false, 1000L, me, List.of(me));
        }

        @Test
        @DisplayName("Join room persists the join then brings the user online")
        void joinRoom_Success() {
            GoOnlineContext ctx = context();
            when(persistence.joinRoom(GUEST_ID, ROOM_CODE)).thenReturn(ctx);

            roomService.joinRoom(GUEST_ID, ROOM_CODE);

            InOrder order = inOrder(persistence, signalingService);
            order.verify(persistence).joinRoom(GUEST_ID, ROOM_CODE);
            order.verify(signalingService).goOnline(GUEST_ID, ctx);
        }

        @Test
        @DisplayName("Join room failed: Persistence rejects the join")
        void joinRoom_Fail_PersistenceError() {
            when(persistence.joinRoom(GUEST_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_FULL));

            assertAppError(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_FULL);

            verifyNoInteractions(signalingService);
        }

        @Test
        @DisplayName("Join room propagates a failure while bringing the user online")
        void joinRoom_Fail_GoOnlineError() {
            GoOnlineContext ctx = context();
            when(persistence.joinRoom(GUEST_ID, ROOM_CODE)).thenReturn(ctx);
            org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(signalingService).goOnline(GUEST_ID, ctx);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========================================================================
    // leaveRoom
    // ========================================================================
    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoomTests {

        @Test
        @DisplayName("Guest leaving tells the others, removes the peer and confirms to the guest")
        void leaveRoom_Success_Guest() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            assertThat(liveRoom.getPeer(GUEST_ID)).isNull();
            assertThat(liveRoom.getPeer(HOST_ID)).isNotNull();
            verify(registry).unbindUser(GUEST_ID, ROOM_CODE);
            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(GUEST_ID, "LEFT"));
            notSentTo(GUEST_ID, SpeakingEvents.PEER_LEFT);
            sentTo(GUEST_ID, SpeakingEvents.ROOM_LEFT, new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 90L));
            notSentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE);
            verify(eventPublisher).publishEvent(new SpeakingMemberRemovedEvent(ROOM_CODE));
            verify(registry, never()).removeRoom(any());
            assertThat(liveRoom.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Host leaving moves the host role in memory and announces the new host")
        void leaveRoom_Success_HostHandsOver() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            LivePeer guest = addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(HOST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(HOST_ID, ROOM_CODE)).thenReturn(leaveResult(GUEST_ID, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(HOST_ID);

            assertThat(liveRoom.getHostId()).isEqualTo(GUEST_ID);
            assertThat(guest.getRole()).isEqualTo(ParticipantRole.HOST);
            sentTo(GUEST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(HOST_ID, "LEFT"));
            sentTo(GUEST_ID, SpeakingEvents.HOST_CHANGED, new HostChangedResponse(GUEST_ID));
            sentTo(HOST_ID, SpeakingEvents.ROOM_LEFT, new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 90L));
        }

        @Test
        @DisplayName("Host leaving does not fail when the new host is not tracked in memory")
        void leaveRoom_Success_NewHostNotInMemory() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(OTHER_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(HOST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(HOST_ID, ROOM_CODE)).thenReturn(leaveResult(GUEST_ID, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(HOST_ID);

            assertThat(liveRoom.getHostId()).isEqualTo(GUEST_ID);
            sentTo(OTHER_ID, SpeakingEvents.HOST_CHANGED, new HostChangedResponse(GUEST_ID));
        }

        @Test
        @DisplayName("Last member leaving closes the room in memory")
        void leaveRoom_Success_LastMemberClosesRoom() {
            addPeer(GUEST_ID, ParticipantRole.HOST);
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            liveRoom.putGrace(OTHER_ID, grace);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, true));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            verify(grace).cancel(false);
            verify(registry).removeRoom(liveRoom);
            verify(eventPublisher, never()).publishEvent(any(Object.class));
            sentTo(GUEST_ID, SpeakingEvents.ROOM_LEFT, new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 90L));
        }

        @Test
        @DisplayName("A closed room tells the peers still online that it is empty")
        void leaveRoom_Success_ClosedRoomNotifiesRemainingPeers() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, true));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            sentTo(HOST_ID, SpeakingEvents.ROOM_CLOSED, new RoomClosedResponse(ROOM_CODE, "EMPTY"));
            verify(registry).removeRoom(liveRoom);
        }

        @Test
        @DisplayName("Leaving removes the room from memory when no peer is left even if it stays open")
        void leaveRoom_Success_EmptyMemoryRoomIsRemoved() {
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            verify(registry).removeRoom(liveRoom);
            verify(eventPublisher).publishEvent(new SpeakingMemberRemovedEvent(ROOM_CODE));
        }

        @Test
        @DisplayName("Leaving cancels the reconnect grace of the leaving user")
        void leaveRoom_Success_CancelsGrace() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            liveRoom.putGrace(GUEST_ID, grace);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            verify(grace).cancel(false);
            assertThat(liveRoom.getGraceFutures()).doesNotContainKey(GUEST_ID);
        }

        @Test
        @DisplayName("Leaving does not notify peers who are offline")
        void leaveRoom_Success_OfflinePeersNotNotified() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(OTHER_ID, ParticipantRole.GUEST).setOnline(false);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.leaveRoom(GUEST_ID);

            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(GUEST_ID, "LEFT"));
            notSentTo(OTHER_ID, SpeakingEvents.PEER_LEFT);
        }

        @Test
        @DisplayName("Leaving still confirms to the user when the room is not in memory")
        void leaveRoom_Success_RoomNotInMemory() {
            when(persistence.findActiveRoomCode(GUEST_ID)).thenReturn(ROOM_CODE);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(null);

            roomService.leaveRoom(GUEST_ID);

            sentTo(GUEST_ID, SpeakingEvents.ROOM_LEFT, new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 90L));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Leave room failed: User is not in any room")
        void leaveRoom_Fail_NotInRoom() {
            when(persistence.findActiveRoomCode(GUEST_ID))
                    .thenThrow(new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));

            assertAppError(() -> roomService.leaveRoom(GUEST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);

            verify(persistence, never()).leaveRoom(anyLong(), anyString());
            verifyNoInteractions(publisher);
        }
    }

    // ========================================================================
    // removeMember
    // ========================================================================
    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("Remove member by timeout clears the active room of the user and tells the others")
        void removeMember_Success_Timeout() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST).setOnline(false);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.removeMember(GUEST_ID, ROOM_CODE, "TIMEOUT");

            assertThat(liveRoom.getPeer(GUEST_ID)).isNull();
            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(GUEST_ID, "TIMEOUT"));
            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
            notSentTo(GUEST_ID, SpeakingEvents.ROOM_LEFT);
            verify(eventPublisher).publishEvent(new SpeakingMemberRemovedEvent(ROOM_CODE));
        }

        @Test
        @DisplayName("Remove member with another reason does not clear the active room")
        void removeMember_Success_OtherReason() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenReturn(leaveResult(null, false));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.removeMember(GUEST_ID, ROOM_CODE, "DISCONNECTED");

            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(GUEST_ID, "DISCONNECTED"));
            notSentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE);
        }

        @Test
        @DisplayName("Remove member ignores a member who already left or a room that is gone")
        void removeMember_Success_AlreadyGone() {
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE))
                    .thenThrow(new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));

            assertThatCode(() -> roomService.removeMember(GUEST_ID, ROOM_CODE, "TIMEOUT")).doesNotThrowAnyException();

            verifyNoInteractions(publisher, eventPublisher, registry);
        }

        @Test
        @DisplayName("Remove member propagates unexpected failures")
        void removeMember_Fail_UnexpectedError() {
            when(persistence.leaveRoom(GUEST_ID, ROOM_CODE)).thenThrow(new IllegalStateException("db down"));

            assertThatThrownBy(() -> roomService.removeMember(GUEST_ID, ROOM_CODE, "TIMEOUT"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========================================================================
    // transferHost
    // ========================================================================
    @Nested
    @DisplayName("transferHost")
    class TransferHostTests {

        @Test
        @DisplayName("Transfer host swaps the roles in memory and tells every online peer")
        void transferHost_Success() {
            LivePeer oldHost = addPeer(HOST_ID, ParticipantRole.HOST);
            LivePeer newHost = addPeer(GUEST_ID, ParticipantRole.GUEST);
            addPeer(OTHER_ID, ParticipantRole.GUEST).setOnline(false);
            when(persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(GUEST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.transferHost(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(liveRoom.getHostId()).isEqualTo(GUEST_ID);
            assertThat(oldHost.getRole()).isEqualTo(ParticipantRole.GUEST);
            assertThat(newHost.getRole()).isEqualTo(ParticipantRole.HOST);
            sentTo(HOST_ID, SpeakingEvents.HOST_CHANGED, new HostChangedResponse(GUEST_ID));
            sentTo(GUEST_ID, SpeakingEvents.HOST_CHANGED, new HostChangedResponse(GUEST_ID));
            notSentTo(OTHER_ID, SpeakingEvents.HOST_CHANGED);
            assertThat(liveRoom.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Transfer host tolerates peers that are missing from memory")
        void transferHost_Success_PeersNotInMemory() {
            addPeer(OTHER_ID, ParticipantRole.GUEST);
            when(persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(GUEST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.transferHost(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(liveRoom.getHostId()).isEqualTo(GUEST_ID);
            sentTo(OTHER_ID, SpeakingEvents.HOST_CHANGED, new HostChangedResponse(GUEST_ID));
        }

        @Test
        @DisplayName("Transfer host does nothing in memory when the room is not loaded")
        void transferHost_Success_RoomNotInMemory() {
            when(persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(GUEST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(null);

            roomService.transferHost(HOST_ID, ROOM_CODE, GUEST_ID);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Transfer host failed: Caller is not the host")
        void transferHost_Fail_NotHost() {
            when(persistence.transferHost(GUEST_ID, ROOM_CODE, OTHER_ID))
                    .thenThrow(new AppException(AppError.SPEAKING_NOT_HOST));

            assertAppError(() -> roomService.transferHost(GUEST_ID, ROOM_CODE, OTHER_ID), AppError.SPEAKING_NOT_HOST);

            verifyNoInteractions(publisher, registry);
        }
    }

    // ========================================================================
    // endRoom
    // ========================================================================
    @Nested
    @DisplayName("endRoom")
    class EndRoomTests {

        private final EndResult endResult = new EndResult(ROOM_CODE, "Topic", 300L);

        @Test
        @DisplayName("End room closes the room for every member and confirms to the host")
        void endRoom_Success() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            addPeer(OTHER_ID, ParticipantRole.GUEST).setOnline(false);
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            liveRoom.putGrace(OTHER_ID, grace);
            when(persistence.endRoom(HOST_ID, ROOM_CODE)).thenReturn(endResult);
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.endRoom(HOST_ID, ROOM_CODE);

            RoomClosedResponse closed = new RoomClosedResponse(ROOM_CODE, "ENDED_BY_HOST");
            ActiveRoomResponse noActiveRoom = new ActiveRoomResponse(null);
            sentTo(GUEST_ID, SpeakingEvents.ROOM_CLOSED, closed);
            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, noActiveRoom);
            // Người đang offline không nhận room:closed nhưng vẫn được xóa active room
            notSentTo(OTHER_ID, SpeakingEvents.ROOM_CLOSED);
            sentTo(OTHER_ID, SpeakingEvents.ROOM_ACTIVE, noActiveRoom);
            // Host không nhận room:closed mà nhận room:left
            notSentTo(HOST_ID, SpeakingEvents.ROOM_CLOSED);
            verify(grace).cancel(false);
            verify(registry).unbindUser(HOST_ID, ROOM_CODE);
            verify(registry).removeRoom(liveRoom);
            assertThat(liveRoom.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("End room tells the host the room is no longer active and then sends the summary")
        void endRoom_Success_HostMessagesInOrder() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            when(persistence.endRoom(HOST_ID, ROOM_CODE)).thenReturn(endResult);
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.endRoom(HOST_ID, ROOM_CODE);

            InOrder order = inOrder(publisher);
            order.verify(publisher).sendToUser(HOST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
            order.verify(publisher).sendToUser(HOST_ID, SpeakingEvents.ROOM_LEFT,
                    new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 300L));
        }

        @Test
        @DisplayName("End room still confirms to the host when the room is not in memory")
        void endRoom_Success_RoomNotInMemory() {
            when(persistence.endRoom(HOST_ID, ROOM_CODE)).thenReturn(endResult);
            when(registry.get(ROOM_CODE)).thenReturn(null);

            roomService.endRoom(HOST_ID, ROOM_CODE);

            sentTo(HOST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
            sentTo(HOST_ID, SpeakingEvents.ROOM_LEFT, new SpeakingSessionSummaryResponse(ROOM_CODE, "Topic", 300L));
            verify(registry, never()).removeRoom(any());
        }

        @Test
        @DisplayName("End room failed: Caller is not the host")
        void endRoom_Fail_NotHost() {
            when(persistence.endRoom(GUEST_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_NOT_HOST));

            assertAppError(() -> roomService.endRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_NOT_HOST);

            verifyNoInteractions(publisher, registry);
        }
    }

    // ========================================================================
    // kickMember
    // ========================================================================
    @Nested
    @DisplayName("kickMember")
    class KickMemberTests {

        @Test
        @DisplayName("Kick member removes an online target and tells everyone who remains")
        void kickMember_Success_OnlineTarget() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST);
            addPeer(OTHER_ID, ParticipantRole.GUEST);
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            liveRoom.putGrace(GUEST_ID, grace);
            when(persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(HOST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.kickMember(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(liveRoom.getPeer(GUEST_ID)).isNull();
            verify(grace).cancel(false);
            sentTo(GUEST_ID, SpeakingEvents.ROOM_KICKED, new RoomKickedResponse(ROOM_CODE));
            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
            verify(registry).unbindUser(GUEST_ID, ROOM_CODE);
            PeerLeftResponse kicked = new PeerLeftResponse(GUEST_ID, "KICKED");
            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, kicked);
            sentTo(OTHER_ID, SpeakingEvents.PEER_LEFT, kicked);
            notSentTo(GUEST_ID, SpeakingEvents.PEER_LEFT);
            verify(eventPublisher).publishEvent(new SpeakingMemberRemovedEvent(ROOM_CODE));
            assertThat(liveRoom.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Kick member does not send room:kicked to a target who is offline")
        void kickMember_Success_OfflineTarget() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            addPeer(GUEST_ID, ParticipantRole.GUEST).setOnline(false);
            when(persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(HOST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.kickMember(HOST_ID, ROOM_CODE, GUEST_ID);

            notSentTo(GUEST_ID, SpeakingEvents.ROOM_KICKED);
            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
            sentTo(HOST_ID, SpeakingEvents.PEER_LEFT, new PeerLeftResponse(GUEST_ID, "KICKED"));
            assertThat(liveRoom.getPeer(GUEST_ID)).isNull();
        }

        @Test
        @DisplayName("Kick member still publishes the removal event when the target is not in memory")
        void kickMember_Success_TargetNotInMemory() {
            addPeer(HOST_ID, ParticipantRole.HOST);
            when(persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(HOST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(liveRoom);

            roomService.kickMember(HOST_ID, ROOM_CODE, GUEST_ID);

            verifyNoInteractions(publisher);
            verify(eventPublisher).publishEvent(new SpeakingMemberRemovedEvent(ROOM_CODE));
        }

        @Test
        @DisplayName("Kick member does nothing in memory when the room is not loaded")
        void kickMember_Success_RoomNotInMemory() {
            when(persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(detail(HOST_ID));
            when(registry.get(ROOM_CODE)).thenReturn(null);

            roomService.kickMember(HOST_ID, ROOM_CODE, GUEST_ID);

            verifyNoInteractions(publisher, eventPublisher);
        }

        @Test
        @DisplayName("Kick member failed: Caller is not the host")
        void kickMember_Fail_NotHost() {
            when(persistence.kickMember(GUEST_ID, ROOM_CODE, OTHER_ID))
                    .thenThrow(new AppException(AppError.SPEAKING_NOT_HOST));

            assertAppError(() -> roomService.kickMember(GUEST_ID, ROOM_CODE, OTHER_ID), AppError.SPEAKING_NOT_HOST);

            verifyNoInteractions(publisher, registry, eventPublisher);
        }
    }

    // ========================================================================
    // listBans / unbanMember
    // ========================================================================
    @Nested
    @DisplayName("bans")
    class BanTests {

        @Test
        @DisplayName("List bans returns what the persistence layer returns")
        void listBans_Success() {
            List<SpeakingBannedUserResponse> bans = List.of(
                    new SpeakingBannedUserResponse(GUEST_ID, "User 2", null, LocalDateTime.now()));
            when(persistence.listBans(HOST_ID, ROOM_CODE)).thenReturn(bans);

            assertThat(roomService.listBans(HOST_ID, ROOM_CODE)).isSameAs(bans);
        }

        @Test
        @DisplayName("List bans failed: Caller is not the host")
        void listBans_Fail_NotHost() {
            when(persistence.listBans(GUEST_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_NOT_HOST));

            assertAppError(() -> roomService.listBans(GUEST_ID, ROOM_CODE), AppError.SPEAKING_NOT_HOST);
        }

        @Test
        @DisplayName("Unban member tells only the host that the ban was removed")
        void unbanMember_Success() {
            when(persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID)).thenReturn(List.of());

            roomService.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID);

            sentTo(HOST_ID, SpeakingEvents.BAN_REMOVED, new BanRemovedResponse(GUEST_ID));
            notSentTo(GUEST_ID, SpeakingEvents.BAN_REMOVED);
        }

        @Test
        @DisplayName("Unban member failed: Target was never kicked")
        void unbanMember_Fail_NotBanned() {
            when(persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID))
                    .thenThrow(new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));

            assertAppError(() -> roomService.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID),
                    AppError.SPEAKING_PARTICIPANT_NOT_FOUND);

            verifyNoInteractions(publisher);
        }
    }

    // ========================================================================
    // queries
    // ========================================================================
    @Nested
    @DisplayName("queries")
    class QueryTests {

        @Test
        @DisplayName("Get room returns the room detail from the persistence layer")
        void getRoom_Success() {
            SpeakingRoomDetailResponse detail = detail(HOST_ID);
            when(persistence.getRoomDetail(ROOM_CODE)).thenReturn(detail);

            assertThat(roomService.getRoom(ROOM_CODE)).isSameAs(detail);
        }

        @Test
        @DisplayName("Get room failed: Room not found")
        void getRoom_Fail_RoomNotFound() {
            when(persistence.getRoomDetail(ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));

            assertAppError(() -> roomService.getRoom(ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Send active room tells the user which room they are in")
        void sendActiveRoom_Success() {
            when(persistence.findActiveRoomCodeOrNull(GUEST_ID)).thenReturn(ROOM_CODE);

            roomService.sendActiveRoom(GUEST_ID);

            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(ROOM_CODE));
        }

        @Test
        @DisplayName("Send active room tells the user they are not in any room")
        void sendActiveRoom_Success_NoActiveRoom() {
            when(persistence.findActiveRoomCodeOrNull(GUEST_ID)).thenReturn(null);

            roomService.sendActiveRoom(GUEST_ID);

            sentTo(GUEST_ID, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
        }

        @Test
        @DisplayName("Get open rooms returns the page from the persistence layer")
        void getOpenRooms_Success() {
            SpeakingRoomSearchRequest request = new SpeakingRoomSearchRequest("travel", 4);
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<SpeakingRoomResponse> page = PageResponse.of(new PageImpl<>(List.of(), pageable, 0));
            when(persistence.getOpenRooms(request, pageable)).thenReturn(page);

            assertThat(roomService.getOpenRooms(request, pageable)).isSameAs(page);
        }
    }
}
