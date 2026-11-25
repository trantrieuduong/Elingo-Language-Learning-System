package com.elingo.speaking.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.request.SpeakingChatRequest;
import com.elingo.speaking.dto.request.SpeakingMicRequest;
import com.elingo.speaking.dto.request.SpeakingMuteRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.dto.request.SpeakingSignalType;
import com.elingo.speaking.dto.response.ChatMessageResponse;
import com.elingo.speaking.dto.response.MuteRequestedResponse;
import com.elingo.speaking.dto.response.PeerMicResponse;
import com.elingo.speaking.dto.response.RoomStateResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingSignalResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;
import com.elingo.speaking.service.impl.SpeakingSignalingServiceImpl;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingSignalingServiceTest {

    @Mock
    private SpeakingEventPublisher publisher;

    // Registry là class đơn giản chạy hoàn toàn trong RAM nên dùng bản thật để test sát hành vi
    private SpeakingLiveRegistry registry;
    private SpeakingSignalingServiceImpl signalingService;

    private static final String ROOM_CODE = "ABC234";
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long OTHER_ID = 3L;

    private static final SpeakingProperties PROPS = new SpeakingProperties(
            4, 4, 16384, List.of("stun:example.org:3478"), 15000L, 30000L, 600000L, 60000L);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        registry = new SpeakingLiveRegistry();
        signalingService = new SpeakingSignalingServiceImpl(registry, publisher, PROPS);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private GoOnlineContext ctx(Long hostId, Long userId, ParticipantRole role) {
        SpeakingPeerResponse me = new SpeakingPeerResponse(userId, "User " + userId, "avatar-" + userId, role);
        return new GoOnlineContext(100L, ROOM_CODE, "Topic", hostId, (short) 4, false, 5000L, me, List.of(me));
    }

    private void hostOnline() {
        signalingService.goOnline(HOST_ID, ctx(HOST_ID, HOST_ID, ParticipantRole.HOST));
    }

    private void guestOnline() {
        signalingService.goOnline(GUEST_ID, ctx(HOST_ID, GUEST_ID, ParticipantRole.GUEST));
    }

    // Host + guest đều online, đã xóa các event phát ra lúc vào phòng
    private LiveSpeakingRoom roomWithHostAndGuest() {
        hostOnline();
        guestOnline();
        clearInvocations(publisher);
        return registry.get(ROOM_CODE);
    }

    private JsonNode payload(String json) {
        return objectMapper.readTree(json);
    }

    private static void assertAppError(ThrowingCallable call, AppError expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("appError", expected);
    }

    // ========================================================================
    // goOnline
    // ========================================================================
    @Nested
    @DisplayName("goOnline")
    class GoOnlineTests {

        @Test
        @DisplayName("Go online creates the room in memory, adds the peer and binds the user")
        void goOnline_Success_CreatesRoom() {
            hostOnline();

            LiveSpeakingRoom room = registry.get(ROOM_CODE);
            assertThat(room).isNotNull();
            assertThat(room.getRoomId()).isEqualTo(100L);
            assertThat(room.getHostId()).isEqualTo(HOST_ID);
            assertThat(room.getTopic()).isEqualTo("Topic");
            assertThat(room.getMaxParticipants()).isEqualTo((short) 4);
            assertThat(room.isPrivate()).isFalse();
            assertThat(room.getStartedAtMs()).isEqualTo(5000L);
            LivePeer peer = room.getPeer(HOST_ID);
            assertThat(peer.getDisplayName()).isEqualTo("User 1");
            assertThat(peer.getAvatarUrl()).isEqualTo("avatar-1");
            assertThat(peer.getRole()).isEqualTo(ParticipantRole.HOST);
            assertThat(peer.isOnline()).isTrue();
            assertThat(registry.getByUser(HOST_ID)).isSameAs(room);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Go online sends the room state to the user who joined")
        void goOnline_Success_SendsRoomState() {
            long before = System.currentTimeMillis();

            hostOnline();

            ArgumentCaptor<RoomStateResponse> captor = ArgumentCaptor.forClass(RoomStateResponse.class);
            verify(publisher).sendToUser(eq(HOST_ID), eq(SpeakingEvents.ROOM_STATE), captor.capture());
            RoomStateResponse state = captor.getValue();
            assertThat(state.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(state.topic()).isEqualTo("Topic");
            assertThat(state.hostId()).isEqualTo(HOST_ID);
            assertThat(state.maxParticipants()).isEqualTo((short) 4);
            assertThat(state.isPrivate()).isFalse();
            assertThat(state.startedAtMs()).isEqualTo(5000L);
            assertThat(state.serverNowMs()).isBetween(before, System.currentTimeMillis());
            assertThat(state.peers()).containsExactly(
                    new SpeakingPeerResponse(HOST_ID, "User 1", "avatar-1", ParticipantRole.HOST));
            assertThat(state.mutedUserIds()).isEmpty();
        }

        @Test
        @DisplayName("Go online tells the peers already in the room about the newcomer but not the newcomer")
        void goOnline_Success_NotifiesOthers() {
            hostOnline();
            clearInvocations(publisher);

            guestOnline();

            verify(publisher).sendToUser(HOST_ID, SpeakingEvents.PEER_JOINED,
                    new SpeakingPeerResponse(GUEST_ID, "User 2", "avatar-2", ParticipantRole.GUEST));
            verify(publisher, never()).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.PEER_JOINED), any());
        }

        @Test
        @DisplayName("Go online returns a state listing every online peer and who is muted")
        void goOnline_Success_StateWithPeersAndMuted() {
            hostOnline();
            registry.get(ROOM_CODE).getPeer(HOST_ID).setMicMuted(true);
            clearInvocations(publisher);

            guestOnline();

            ArgumentCaptor<RoomStateResponse> captor = ArgumentCaptor.forClass(RoomStateResponse.class);
            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.ROOM_STATE), captor.capture());
            assertThat(captor.getValue().peers()).extracting(SpeakingPeerResponse::userId)
                    .containsExactly(HOST_ID, GUEST_ID);
            assertThat(captor.getValue().mutedUserIds()).containsExactly(HOST_ID);
        }

        @Test
        @DisplayName("Go online leaves offline peers out of the room state")
        void goOnline_Success_StateSkipsOfflinePeers() {
            hostOnline();
            registry.get(ROOM_CODE).putPeer(new LivePeer(OTHER_ID, "User 3", null, ParticipantRole.GUEST));
            registry.get(ROOM_CODE).getPeer(OTHER_ID).setOnline(false);
            clearInvocations(publisher);

            guestOnline();

            ArgumentCaptor<RoomStateResponse> captor = ArgumentCaptor.forClass(RoomStateResponse.class);
            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.ROOM_STATE), captor.capture());
            assertThat(captor.getValue().peers()).extracting(SpeakingPeerResponse::userId)
                    .containsExactly(HOST_ID, GUEST_ID);
            verify(publisher, never()).sendToUser(eq(OTHER_ID), any(String.class), any());
        }

        @Test
        @DisplayName("Go online again for a user already online resends the state without announcing a join")
        void goOnline_Success_AlreadyOnline() {
            roomWithHostAndGuest();

            guestOnline();

            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.ROOM_STATE), any());
            verify(publisher, never()).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_JOINED), any());
            verify(publisher, never()).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_RECONNECTED), any());
            assertThat(registry.get(ROOM_CODE).getPeers()).hasSize(2);
        }

        @Test
        @DisplayName("Go online during the reconnect grace cancels the timer and announces a reconnection")
        void goOnline_Success_ReconnectInGrace() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            ScheduledFuture<?> grace = mock(ScheduledFuture.class);
            room.getPeer(GUEST_ID).setOnline(false);
            room.putGrace(GUEST_ID, grace);

            guestOnline();

            verify(grace).cancel(false);
            assertThat(room.getGraceFutures()).doesNotContainKey(GUEST_ID);
            assertThat(room.getPeer(GUEST_ID).isOnline()).isTrue();
            verify(publisher).sendToUser(HOST_ID, SpeakingEvents.PEER_RECONNECTED,
                    new SpeakingPeerResponse(GUEST_ID, "User 2", "avatar-2", ParticipantRole.GUEST));
            verify(publisher, never()).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_JOINED), any());
        }

        @Test
        @DisplayName("Go online for an offline peer without a grace timer announces a plain join")
        void goOnline_Success_OfflineWithoutGrace() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(GUEST_ID).setOnline(false);

            guestOnline();

            assertThat(room.getPeer(GUEST_ID).isOnline()).isTrue();
            verify(publisher).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_JOINED), any());
            verify(publisher, never()).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_RECONNECTED), any());
        }

        @Test
        @DisplayName("Go online refreshes the room settings and the peer role from the latest data")
        void goOnline_Success_RefreshesRoomAndRole() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            SpeakingPeerResponse promoted = new SpeakingPeerResponse(GUEST_ID, "User 2", "avatar-2", ParticipantRole.HOST);
            GoOnlineContext refreshed = new GoOnlineContext(100L, ROOM_CODE, "Topic", GUEST_ID, (short) 4, true, 9000L,
                    promoted, List.of(promoted));

            signalingService.goOnline(GUEST_ID, refreshed);

            assertThat(room.getHostId()).isEqualTo(GUEST_ID);
            assertThat(room.isPrivate()).isTrue();
            assertThat(room.getStartedAtMs()).isEqualTo(9000L);
            assertThat(room.getPeer(GUEST_ID).getRole()).isEqualTo(ParticipantRole.HOST);
        }

        @Test
        @DisplayName("Go online drops the user silently from a previous room and removes it when empty")
        void goOnline_Success_LeavesStaleRoom() {
            LiveSpeakingRoom oldRoom = registry.getOrCreate("OLDCDE", () ->
                    new LiveSpeakingRoom("OLDCDE", 7L, GUEST_ID, "Old", (short) 4, false, 1L));
            oldRoom.putPeer(new LivePeer(GUEST_ID, "User 2", null, ParticipantRole.HOST));
            registry.bindUser(GUEST_ID, "OLDCDE");

            guestOnline();

            assertThat(registry.get("OLDCDE")).isNull();
            assertThat(oldRoom.getPeer(GUEST_ID)).isNull();
            assertThat(registry.getByUser(GUEST_ID).getRoomCode()).isEqualTo(ROOM_CODE);
        }

        @Test
        @DisplayName("Go online keeps a previous room alive and quiet when other peers remain in it")
        void goOnline_Success_StaleRoomKeepsOthers() {
            LiveSpeakingRoom oldRoom = registry.getOrCreate("OLDCDE", () ->
                    new LiveSpeakingRoom("OLDCDE", 7L, OTHER_ID, "Old", (short) 4, false, 1L));
            oldRoom.putPeer(new LivePeer(OTHER_ID, "User 3", null, ParticipantRole.HOST));
            oldRoom.putPeer(new LivePeer(GUEST_ID, "User 2", null, ParticipantRole.GUEST));
            registry.bindUser(GUEST_ID, "OLDCDE");

            guestOnline();

            assertThat(registry.get("OLDCDE")).isSameAs(oldRoom);
            assertThat(oldRoom.getPeer(GUEST_ID)).isNull();
            assertThat(oldRoom.getPeer(OTHER_ID)).isNotNull();
            verify(publisher, never()).sendToUser(eq(OTHER_ID), any(String.class), any());
        }
    }

    // ========================================================================
    // relaySignal
    // ========================================================================
    @Nested
    @DisplayName("relaySignal")
    class RelaySignalTests {

        private SpeakingSignalRequest request(String code, Long to, JsonNode payload) {
            return new SpeakingSignalRequest(code, to, SpeakingSignalType.OFFER, payload);
        }

        @Test
        @DisplayName("Relay signal forwards the payload to the target with the sender taken from the principal")
        void relaySignal_Success() {
            roomWithHostAndGuest();
            JsonNode payload = payload("{\"sdp\":\"v=0\"}");

            signalingService.relaySignal(HOST_ID, request(ROOM_CODE, GUEST_ID, payload));

            verify(publisher).sendToUser(GUEST_ID, SpeakingEvents.SIGNAL,
                    new SpeakingSignalResponse(HOST_ID, SpeakingSignalType.OFFER, payload));
            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal normalizes the room code")
        void relaySignal_Success_NormalizesCode() {
            roomWithHostAndGuest();

            signalingService.relaySignal(HOST_ID, request(" abc234 ", GUEST_ID, payload("{\"a\":1}")));

            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.SIGNAL), any());
        }

        @Test
        @DisplayName("Relay signal accepts a payload exactly at the size limit")
        void relaySignal_Success_PayloadAtLimit() {
            roomWithHostAndGuest();
            JsonNode payload = payload("{\"sdp\":\"abcdef\"}");
            int limit = payload.toString().length();
            SpeakingProperties tight = new SpeakingProperties(4, 4, limit, List.of(), 1L, 1L, 1L, 1L);
            SpeakingSignalingServiceImpl service = new SpeakingSignalingServiceImpl(registry, publisher, tight);

            service.relaySignal(HOST_ID, request(ROOM_CODE, GUEST_ID, payload));

            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.SIGNAL), any());
        }

        @Test
        @DisplayName("Relay signal failed: Payload is one byte over the limit")
        void relaySignal_Fail_PayloadOverLimit() {
            roomWithHostAndGuest();
            JsonNode payload = payload("{\"sdp\":\"abcdef\"}");
            SpeakingProperties tight = new SpeakingProperties(
                    4, 4, payload.toString().length() - 1, List.of(), 1L, 1L, 1L, 1L);
            SpeakingSignalingServiceImpl service = new SpeakingSignalingServiceImpl(registry, publisher, tight);

            assertAppError(() -> service.relaySignal(HOST_ID, request(ROOM_CODE, GUEST_ID, payload)),
                    AppError.SPEAKING_SIGNAL_INVALID);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Payload is a JSON null")
        void relaySignal_Fail_NullPayload() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, GUEST_ID, NullNode.getInstance())), AppError.SPEAKING_SIGNAL_INVALID);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Room is not loaded")
        void relaySignal_Fail_RoomNotFound() {
            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request("ZZZZZZ", GUEST_ID, payload("{}"))), AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Sender is not in the room")
        void relaySignal_Fail_SenderNotInRoom() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.relaySignal(OTHER_ID,
                    request(ROOM_CODE, GUEST_ID, payload("{}"))), AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Sender is offline")
        void relaySignal_Fail_SenderOffline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(HOST_ID).setOnline(false);

            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, GUEST_ID, payload("{}"))), AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Sender targets themselves")
        void relaySignal_Fail_SelfTarget() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, HOST_ID, payload("{}"))), AppError.SPEAKING_SIGNAL_INVALID);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Target is offline")
        void relaySignal_Fail_TargetOffline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(GUEST_ID).setOnline(false);

            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, GUEST_ID, payload("{}"))), AppError.SPEAKING_SIGNAL_INVALID);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal failed: Target is not in the room")
        void relaySignal_Fail_TargetNotInRoom() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, OTHER_ID, payload("{}"))), AppError.SPEAKING_SIGNAL_INVALID);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Relay signal releases the room lock after a failure")
        void relaySignal_Fail_ReleasesLock() {
            LiveSpeakingRoom room = roomWithHostAndGuest();

            assertThatThrownBy(() -> signalingService.relaySignal(HOST_ID,
                    request(ROOM_CODE, HOST_ID, payload("{}")))).isInstanceOf(AppException.class);

            assertThat(room.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // reportMic
    // ========================================================================
    @Nested
    @DisplayName("reportMic")
    class ReportMicTests {

        @Test
        @DisplayName("Report mic stores the state and tells the other online peers but not the sender")
        void reportMic_Success() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.putPeer(new LivePeer(OTHER_ID, "User 3", null, ParticipantRole.GUEST));
            room.getPeer(OTHER_ID).setOnline(false);

            signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(ROOM_CODE, true));

            assertThat(room.getPeer(GUEST_ID).isMicMuted()).isTrue();
            verify(publisher).sendToUser(HOST_ID, SpeakingEvents.PEER_MIC, new PeerMicResponse(GUEST_ID, true));
            verify(publisher, never()).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.PEER_MIC), any());
            verify(publisher, never()).sendToUser(eq(OTHER_ID), eq(SpeakingEvents.PEER_MIC), any());
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Report mic broadcasts again when the user unmutes")
        void reportMic_Success_Unmute() {
            roomWithHostAndGuest();
            signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(ROOM_CODE, true));

            signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(ROOM_CODE, false));

            verify(publisher).sendToUser(HOST_ID, SpeakingEvents.PEER_MIC, new PeerMicResponse(GUEST_ID, false));
            assertThat(registry.get(ROOM_CODE).getPeer(GUEST_ID).isMicMuted()).isFalse();
        }

        @Test
        @DisplayName("Report mic does not broadcast when the state did not change")
        void reportMic_Success_Unchanged() {
            roomWithHostAndGuest();

            signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(ROOM_CODE, false));

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Report mic normalizes the room code")
        void reportMic_Success_NormalizesCode() {
            roomWithHostAndGuest();

            signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(" abc234 ", true));

            verify(publisher).sendToUser(eq(HOST_ID), eq(SpeakingEvents.PEER_MIC), any());
        }

        @Test
        @DisplayName("Report mic failed: Room is not loaded")
        void reportMic_Fail_RoomNotFound() {
            assertAppError(() -> signalingService.reportMic(GUEST_ID, new SpeakingMicRequest("ZZZZZZ", true)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Report mic failed: User is not in the room")
        void reportMic_Fail_NotInRoom() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.reportMic(OTHER_ID, new SpeakingMicRequest(ROOM_CODE, true)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Report mic failed: User is offline")
        void reportMic_Fail_Offline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(GUEST_ID).setOnline(false);

            assertAppError(() -> signalingService.reportMic(GUEST_ID, new SpeakingMicRequest(ROOM_CODE, true)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);

            assertThat(room.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // requestMute
    // ========================================================================
    @Nested
    @DisplayName("requestMute")
    class RequestMuteTests {

        @Test
        @DisplayName("Request mute asks the target to mute and names the host")
        void requestMute_Success() {
            roomWithHostAndGuest();

            signalingService.requestMute(HOST_ID, new SpeakingMuteRequest(ROOM_CODE, GUEST_ID));

            verify(publisher).sendToUser(GUEST_ID, SpeakingEvents.MUTE_REQUESTED, new MuteRequestedResponse(HOST_ID));
            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Request mute failed: Room is not loaded")
        void requestMute_Fail_RoomNotFound() {
            assertAppError(() -> signalingService.requestMute(HOST_ID, new SpeakingMuteRequest("ZZZZZZ", GUEST_ID)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Request mute failed: Caller is not in the room")
        void requestMute_Fail_CallerNotInRoom() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.requestMute(OTHER_ID, new SpeakingMuteRequest(ROOM_CODE, GUEST_ID)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Request mute failed: Caller is offline")
        void requestMute_Fail_CallerOffline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(HOST_ID).setOnline(false);

            assertAppError(() -> signalingService.requestMute(HOST_ID, new SpeakingMuteRequest(ROOM_CODE, GUEST_ID)),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Request mute failed: Caller is not the host")
        void requestMute_Fail_NotHost() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.requestMute(GUEST_ID, new SpeakingMuteRequest(ROOM_CODE, HOST_ID)),
                    AppError.SPEAKING_NOT_HOST);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Request mute failed: Host targets themselves")
        void requestMute_Fail_SelfTarget() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.requestMute(HOST_ID, new SpeakingMuteRequest(ROOM_CODE, HOST_ID)),
                    AppError.INVALID_REQUEST);
        }

        @Test
        @DisplayName("Request mute failed: Target is offline")
        void requestMute_Fail_TargetOffline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(GUEST_ID).setOnline(false);

            assertAppError(() -> signalingService.requestMute(HOST_ID, new SpeakingMuteRequest(ROOM_CODE, GUEST_ID)),
                    AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Request mute failed: Target is not in the room")
        void requestMute_Fail_TargetNotInRoom() {
            LiveSpeakingRoom room = roomWithHostAndGuest();

            assertAppError(() -> signalingService.requestMute(HOST_ID, new SpeakingMuteRequest(ROOM_CODE, OTHER_ID)),
                    AppError.SPEAKING_PARTICIPANT_NOT_FOUND);

            assertThat(room.getLock().isLocked()).isFalse();
        }
    }

    // ========================================================================
    // chat
    // ========================================================================
    @Nested
    @DisplayName("chat")
    class ChatTests {

        @Test
        @DisplayName("Chat delivers the trimmed message to every online peer including the sender")
        void chat_Success() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.putPeer(new LivePeer(OTHER_ID, "User 3", null, ParticipantRole.GUEST));
            room.getPeer(OTHER_ID).setOnline(false);
            long before = System.currentTimeMillis();

            signalingService.chat(GUEST_ID, new SpeakingChatRequest(ROOM_CODE, "  hello everyone  "));

            ArgumentCaptor<ChatMessageResponse> toHost = ArgumentCaptor.forClass(ChatMessageResponse.class);
            ArgumentCaptor<ChatMessageResponse> toGuest = ArgumentCaptor.forClass(ChatMessageResponse.class);
            verify(publisher).sendToUser(eq(HOST_ID), eq(SpeakingEvents.CHAT_MESSAGE), toHost.capture());
            verify(publisher).sendToUser(eq(GUEST_ID), eq(SpeakingEvents.CHAT_MESSAGE), toGuest.capture());
            verify(publisher, never()).sendToUser(eq(OTHER_ID), eq(SpeakingEvents.CHAT_MESSAGE), any());
            ChatMessageResponse message = toHost.getValue();
            assertThat(message.fromUserId()).isEqualTo(GUEST_ID);
            assertThat(message.displayName()).isEqualTo("User 2");
            assertThat(message.text()).isEqualTo("hello everyone");
            assertThat(message.timestamp()).isBetween(before, System.currentTimeMillis());
            assertThat(toGuest.getValue()).isSameAs(message);
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Chat normalizes the room code")
        void chat_Success_NormalizesCode() {
            roomWithHostAndGuest();

            signalingService.chat(GUEST_ID, new SpeakingChatRequest(" abc234 ", "hi"));

            verify(publisher, times(2)).sendToUser(any(Long.class), eq(SpeakingEvents.CHAT_MESSAGE), any());
        }

        @ParameterizedTest(name = "[{index}] text \"{0}\"")
        @ValueSource(strings = {"", "   ", "\t\n"})
        @DisplayName("Chat failed: Message is blank")
        void chat_Fail_BlankText(String text) {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.chat(GUEST_ID, new SpeakingChatRequest(ROOM_CODE, text)),
                    AppError.INVALID_REQUEST);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Chat failed: Room is not loaded")
        void chat_Fail_RoomNotFound() {
            assertAppError(() -> signalingService.chat(GUEST_ID, new SpeakingChatRequest("ZZZZZZ", "hi")),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Chat failed: Sender is not in the room")
        void chat_Fail_SenderNotInRoom() {
            roomWithHostAndGuest();

            assertAppError(() -> signalingService.chat(OTHER_ID, new SpeakingChatRequest(ROOM_CODE, "hi")),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);

            verifyNoMoreInteractions(publisher);
        }

        @Test
        @DisplayName("Chat failed: Sender is offline")
        void chat_Fail_SenderOffline() {
            LiveSpeakingRoom room = roomWithHostAndGuest();
            room.getPeer(GUEST_ID).setOnline(false);

            assertAppError(() -> signalingService.chat(GUEST_ID, new SpeakingChatRequest(ROOM_CODE, "hi")),
                    AppError.SPEAKING_ROOM_ACCESS_DENIED);

            assertThat(room.getLock().isLocked()).isFalse();
        }
    }
}
