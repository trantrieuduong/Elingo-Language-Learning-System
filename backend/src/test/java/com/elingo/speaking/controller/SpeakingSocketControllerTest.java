package com.elingo.speaking.controller;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.EndSpeakingRoomRequest;
import com.elingo.speaking.dto.request.JoinSpeakingRoomRequest;
import com.elingo.speaking.dto.request.KickMemberRequest;
import com.elingo.speaking.dto.request.SpeakingChatRequest;
import com.elingo.speaking.dto.request.SpeakingMicRequest;
import com.elingo.speaking.dto.request.SpeakingMuteRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.dto.request.SpeakingSignalType;
import com.elingo.speaking.dto.request.TransferHostRequest;
import com.elingo.speaking.dto.request.UnbanMemberRequest;
import com.elingo.speaking.service.SpeakingRoomService;
import com.elingo.speaking.service.SpeakingSignalingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingSocketControllerTest {

    @Mock
    private SpeakingRoomService speakingRoomService;

    @Mock
    private SpeakingSignalingService signalingService;

    @InjectMocks
    private SpeakingSocketController controller;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_ID = 2L;
    private static final String ROOM_CODE = "ABC234";

    private Principal principal;

    @BeforeEach
    void setUp() {
        // Principal.getName() của WebSocket session chính là userId (dạng chuỗi)
        principal = () -> String.valueOf(USER_ID);
    }

    // ========================================================================
    // delegation to the services
    // ========================================================================
    @Nested
    @DisplayName("room commands")
    class RoomCommandTests {

        @Test
        @DisplayName("Create delegates to the room service with the principal user id")
        void create_Success() {
            CreateSpeakingRoomRequest request = new CreateSpeakingRoomRequest("Travel", 3, true);

            controller.create(request, principal);

            verify(speakingRoomService).createRoom(USER_ID, request);
        }

        @Test
        @DisplayName("Create propagates a business error")
        void create_Fail_ServiceError() {
            CreateSpeakingRoomRequest request = new CreateSpeakingRoomRequest("Travel", 3, true);
            doThrow(new AppException(AppError.SPEAKING_ALREADY_IN_ROOM)).when(speakingRoomService).createRoom(USER_ID, request);

            assertThatThrownBy(() -> controller.create(request, principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SPEAKING_ALREADY_IN_ROOM));
        }

        @Test
        @DisplayName("Join delegates to the room service with the room code from the request")
        void join_Success() {
            controller.join(new JoinSpeakingRoomRequest(ROOM_CODE), principal);

            verify(speakingRoomService).joinRoom(USER_ID, ROOM_CODE);
        }

        @Test
        @DisplayName("Join propagates a business error")
        void join_Fail_ServiceError() {
            doThrow(new AppException(AppError.SPEAKING_ROOM_FULL)).when(speakingRoomService).joinRoom(USER_ID, ROOM_CODE);

            assertThatThrownBy(() -> controller.join(new JoinSpeakingRoomRequest(ROOM_CODE), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SPEAKING_ROOM_FULL));
        }

        @Test
        @DisplayName("Leave delegates to the room service")
        void leave_Success() {
            controller.leave(principal);

            verify(speakingRoomService).leaveRoom(USER_ID);
        }

        @Test
        @DisplayName("Transfer host delegates with the room code and the new host id")
        void transferHost_Success() {
            controller.transferHost(new TransferHostRequest(ROOM_CODE, TARGET_ID), principal);

            verify(speakingRoomService).transferHost(USER_ID, ROOM_CODE, TARGET_ID);
        }

        @Test
        @DisplayName("End delegates to the room service")
        void end_Success() {
            controller.end(new EndSpeakingRoomRequest(ROOM_CODE), principal);

            verify(speakingRoomService).endRoom(USER_ID, ROOM_CODE);
        }

        @Test
        @DisplayName("Kick delegates with the room code and the target user id")
        void kick_Success() {
            controller.kick(new KickMemberRequest(ROOM_CODE, TARGET_ID), principal);

            verify(speakingRoomService).kickMember(USER_ID, ROOM_CODE, TARGET_ID);
        }

        @Test
        @DisplayName("Kick propagates a business error")
        void kick_Fail_ServiceError() {
            doThrow(new AppException(AppError.SPEAKING_NOT_HOST)).when(speakingRoomService).kickMember(USER_ID, ROOM_CODE, TARGET_ID);

            assertThatThrownBy(() -> controller.kick(new KickMemberRequest(ROOM_CODE, TARGET_ID), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SPEAKING_NOT_HOST));
        }

        @Test
        @DisplayName("Unban delegates with the room code and the target user id")
        void unban_Success() {
            controller.unban(new UnbanMemberRequest(ROOM_CODE, TARGET_ID), principal);

            verify(speakingRoomService).unbanMember(USER_ID, ROOM_CODE, TARGET_ID);
        }

        @Test
        @DisplayName("Room commands fail when the principal name is not a user id")
        void roomCommands_Fail_InvalidPrincipal() {
            Principal broken = () -> "not-a-number";

            assertThatThrownBy(() -> controller.leave(broken)).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> controller.join(new JoinSpeakingRoomRequest(ROOM_CODE), broken))
                    .isInstanceOf(NumberFormatException.class);

            verifyNoInteractions(speakingRoomService);
        }
    }

    @Nested
    @DisplayName("signaling commands")
    class SignalingCommandTests {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        @DisplayName("Signal delegates to the signaling service")
        void signal_Success() {
            JsonNode payload = objectMapper.readTree("{\"sdp\":\"v=0\"}");
            SpeakingSignalRequest request = new SpeakingSignalRequest(ROOM_CODE, TARGET_ID, SpeakingSignalType.OFFER, payload);

            controller.signal(request, principal);

            verify(signalingService).relaySignal(USER_ID, request);
        }

        @Test
        @DisplayName("Signal propagates a business error")
        void signal_Fail_ServiceError() {
            JsonNode payload = objectMapper.readTree("{}");
            SpeakingSignalRequest request = new SpeakingSignalRequest(ROOM_CODE, TARGET_ID, SpeakingSignalType.ICE, payload);
            doThrow(new AppException(AppError.SPEAKING_SIGNAL_INVALID)).when(signalingService).relaySignal(USER_ID, request);

            assertThatThrownBy(() -> controller.signal(request, principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SPEAKING_SIGNAL_INVALID));
        }

        @Test
        @DisplayName("Mic delegates to the signaling service")
        void mic_Success() {
            SpeakingMicRequest request = new SpeakingMicRequest(ROOM_CODE, true);

            controller.mic(request, principal);

            verify(signalingService).reportMic(USER_ID, request);
        }

        @Test
        @DisplayName("Mute delegates to the signaling service")
        void mute_Success() {
            SpeakingMuteRequest request = new SpeakingMuteRequest(ROOM_CODE, TARGET_ID);

            controller.mute(request, principal);

            verify(signalingService).requestMute(USER_ID, request);
        }

        @Test
        @DisplayName("Chat delegates to the signaling service")
        void chat_Success() {
            SpeakingChatRequest request = new SpeakingChatRequest(ROOM_CODE, "hello");

            controller.chat(request, principal);

            verify(signalingService).chat(USER_ID, request);
        }

        @Test
        @DisplayName("Chat propagates a business error")
        void chat_Fail_ServiceError() {
            SpeakingChatRequest request = new SpeakingChatRequest(ROOM_CODE, "hello");
            doThrow(new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED)).when(signalingService).chat(USER_ID, request);

            assertThatThrownBy(() -> controller.chat(request, principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SPEAKING_ROOM_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // activeRoom
    // ========================================================================
    @Nested
    @DisplayName("activeRoom")
    class ActiveRoomTests {

        @Test
        @DisplayName("Active room asks the service to send the active room to the user")
        void activeRoom_Success() {
            controller.activeRoom(principal);

            verify(speakingRoomService).sendActiveRoom(USER_ID);
        }

        @Test
        @DisplayName("Active room swallows a service failure instead of sending an error")
        void activeRoom_Success_FailureIsSwallowed() {
            doThrow(new IllegalStateException("db down")).when(speakingRoomService).sendActiveRoom(USER_ID);

            assertThatCode(() -> controller.activeRoom(principal)).doesNotThrowAnyException();

            verify(speakingRoomService).sendActiveRoom(USER_ID);
        }

        @Test
        @DisplayName("Active room swallows an invalid principal name")
        void activeRoom_Success_InvalidPrincipalIsSwallowed() {
            Principal broken = () -> "not-a-number";

            assertThatCode(() -> controller.activeRoom(broken)).doesNotThrowAnyException();

            verifyNoInteractions(speakingRoomService);
        }
    }

    // ========================================================================
    // Request validation (@Valid trên payload của @MessageMapping)
    // ========================================================================
    @Nested
    @DisplayName("request validation")
    class RequestValidationTests {

        private Validator validator;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void initValidator() {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        private <T> List<String> violationMessages(T target) {
            return validator.validate(target).stream()
                    .map(ConstraintViolation::getMessage)
                    .toList();
        }

        // ---- CreateSpeakingRoomRequest ----
        @Test
        @DisplayName("CreateSpeakingRoomRequest is valid with every optional field missing")
        void createRequest_Valid_AllOptional() {
            assertThat(violationMessages(new CreateSpeakingRoomRequest(null, null, null))).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] maxParticipants {0}")
        @ValueSource(ints = {2, 3, 4})
        @DisplayName("CreateSpeakingRoomRequest accepts a capacity from 2 to 4")
        void createRequest_Valid_Capacity(int capacity) {
            assertThat(violationMessages(new CreateSpeakingRoomRequest("t", capacity, false))).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] maxParticipants {0}")
        @ValueSource(ints = {-1, 0, 1, 5, 100})
        @DisplayName("CreateSpeakingRoomRequest failed: Capacity outside 2..4")
        void createRequest_Fail_Capacity(int capacity) {
            assertThat(violationMessages(new CreateSpeakingRoomRequest("t", capacity, false)))
                    .containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("CreateSpeakingRoomRequest accepts a topic of exactly 255 characters")
        void createRequest_Valid_TopicAtLimit() {
            assertThat(violationMessages(new CreateSpeakingRoomRequest("a".repeat(255), 2, false))).isEmpty();
        }

        @Test
        @DisplayName("CreateSpeakingRoomRequest failed: Topic longer than 255 characters")
        void createRequest_Fail_TopicTooLong() {
            assertThat(violationMessages(new CreateSpeakingRoomRequest("a".repeat(256), 2, false)))
                    .containsExactly("INVALID_REQUEST");
        }

        // ---- JoinSpeakingRoomRequest ----
        @Test
        @DisplayName("JoinSpeakingRoomRequest is valid when the room code is provided")
        void joinRequest_Valid() {
            assertThat(violationMessages(new JoinSpeakingRoomRequest(ROOM_CODE))).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] roomCode \"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("JoinSpeakingRoomRequest failed: Blank room code")
        void joinRequest_Fail_BlankRoomCode(String roomCode) {
            assertThat(violationMessages(new JoinSpeakingRoomRequest(roomCode)))
                    .containsExactly("SPEAKING_ROOM_NOT_FOUND");
        }

        @Test
        @DisplayName("JoinSpeakingRoomRequest failed: Missing room code")
        void joinRequest_Fail_NullRoomCode() {
            assertThat(violationMessages(new JoinSpeakingRoomRequest(null))).containsExactly("SPEAKING_ROOM_NOT_FOUND");
        }

        // ---- EndSpeakingRoomRequest ----
        @Test
        @DisplayName("EndSpeakingRoomRequest is valid when the room code is provided")
        void endRequest_Valid() {
            assertThat(violationMessages(new EndSpeakingRoomRequest(ROOM_CODE))).isEmpty();
        }

        @Test
        @DisplayName("EndSpeakingRoomRequest failed: Blank room code")
        void endRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new EndSpeakingRoomRequest(" "))).containsExactly("INVALID_REQUEST");
        }

        // ---- TransferHostRequest ----
        @Test
        @DisplayName("TransferHostRequest is valid when room code and new host are provided")
        void transferRequest_Valid() {
            assertThat(violationMessages(new TransferHostRequest(ROOM_CODE, TARGET_ID))).isEmpty();
        }

        @Test
        @DisplayName("TransferHostRequest failed: Missing room code and new host")
        void transferRequest_Fail_Missing() {
            assertThat(violationMessages(new TransferHostRequest(null, null)))
                    .containsExactlyInAnyOrder("INVALID_REQUEST", "INVALID_REQUEST");
        }

        // ---- KickMemberRequest ----
        @Test
        @DisplayName("KickMemberRequest is valid when room code and user are provided")
        void kickRequest_Valid() {
            assertThat(violationMessages(new KickMemberRequest(ROOM_CODE, TARGET_ID))).isEmpty();
        }

        @Test
        @DisplayName("KickMemberRequest failed: Blank room code")
        void kickRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new KickMemberRequest("", TARGET_ID))).containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("KickMemberRequest failed: Missing user id")
        void kickRequest_Fail_MissingUser() {
            assertThat(violationMessages(new KickMemberRequest(ROOM_CODE, null))).containsExactly("INVALID_REQUEST");
        }

        // ---- UnbanMemberRequest ----
        @Test
        @DisplayName("UnbanMemberRequest is valid when room code and user are provided")
        void unbanRequest_Valid() {
            assertThat(violationMessages(new UnbanMemberRequest(ROOM_CODE, TARGET_ID))).isEmpty();
        }

        @Test
        @DisplayName("UnbanMemberRequest failed: Missing room code and user id")
        void unbanRequest_Fail_Missing() {
            assertThat(violationMessages(new UnbanMemberRequest(null, null)))
                    .containsExactlyInAnyOrder("INVALID_REQUEST", "INVALID_REQUEST");
        }

        // ---- SpeakingMicRequest ----
        @ParameterizedTest(name = "[{index}] muted {0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("SpeakingMicRequest is valid with a muted flag")
        void micRequest_Valid(boolean muted) {
            assertThat(violationMessages(new SpeakingMicRequest(ROOM_CODE, muted))).isEmpty();
        }

        @Test
        @DisplayName("SpeakingMicRequest failed: Missing muted flag")
        void micRequest_Fail_MissingMuted() {
            assertThat(violationMessages(new SpeakingMicRequest(ROOM_CODE, null))).containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("SpeakingMicRequest failed: Blank room code")
        void micRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new SpeakingMicRequest("  ", true))).containsExactly("INVALID_REQUEST");
        }

        // ---- SpeakingMuteRequest ----
        @Test
        @DisplayName("SpeakingMuteRequest is valid when room code and target are provided")
        void muteRequest_Valid() {
            assertThat(violationMessages(new SpeakingMuteRequest(ROOM_CODE, TARGET_ID))).isEmpty();
        }

        @Test
        @DisplayName("SpeakingMuteRequest failed: Missing target user id")
        void muteRequest_Fail_MissingTarget() {
            assertThat(violationMessages(new SpeakingMuteRequest(ROOM_CODE, null))).containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("SpeakingMuteRequest failed: Blank room code")
        void muteRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new SpeakingMuteRequest("", TARGET_ID))).containsExactly("INVALID_REQUEST");
        }

        // ---- SpeakingChatRequest ----
        @Test
        @DisplayName("SpeakingChatRequest is valid with a short message")
        void chatRequest_Valid() {
            assertThat(violationMessages(new SpeakingChatRequest(ROOM_CODE, "hello"))).isEmpty();
        }

        @Test
        @DisplayName("SpeakingChatRequest accepts a message of exactly 500 characters")
        void chatRequest_Valid_TextAtLimit() {
            assertThat(violationMessages(new SpeakingChatRequest(ROOM_CODE, "a".repeat(500)))).isEmpty();
        }

        @Test
        @DisplayName("SpeakingChatRequest failed: Message longer than 500 characters")
        void chatRequest_Fail_TextTooLong() {
            assertThat(violationMessages(new SpeakingChatRequest(ROOM_CODE, "a".repeat(501))))
                    .containsExactly("INVALID_REQUEST");
        }

        @ParameterizedTest(name = "[{index}] text \"{0}\"")
        @ValueSource(strings = {"", "   ", "\t"})
        @DisplayName("SpeakingChatRequest failed: Blank message")
        void chatRequest_Fail_BlankText(String text) {
            assertThat(violationMessages(new SpeakingChatRequest(ROOM_CODE, text))).containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("SpeakingChatRequest failed: Missing message")
        void chatRequest_Fail_NullText() {
            assertThat(violationMessages(new SpeakingChatRequest(ROOM_CODE, null))).containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("SpeakingChatRequest failed: Blank room code")
        void chatRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new SpeakingChatRequest("", "hello"))).containsExactly("INVALID_REQUEST");
        }

        // ---- SpeakingSignalRequest ----
        @Test
        @DisplayName("SpeakingSignalRequest is valid when every field is provided")
        void signalRequest_Valid() {
            JsonNode payload = objectMapper.readTree("{\"sdp\":\"v=0\"}");

            assertThat(violationMessages(new SpeakingSignalRequest(ROOM_CODE, TARGET_ID, SpeakingSignalType.OFFER, payload)))
                    .isEmpty();
        }

        @Test
        @DisplayName("SpeakingSignalRequest failed: Blank room code")
        void signalRequest_Fail_BlankRoomCode() {
            JsonNode payload = objectMapper.readTree("{}");

            assertThat(violationMessages(new SpeakingSignalRequest(" ", TARGET_ID, SpeakingSignalType.ICE, payload)))
                    .containsExactly("SPEAKING_ROOM_NOT_FOUND");
        }

        @Test
        @DisplayName("SpeakingSignalRequest failed: Missing target, type and payload")
        void signalRequest_Fail_MissingFields() {
            assertThat(violationMessages(new SpeakingSignalRequest(ROOM_CODE, null, null, null)))
                    .containsExactlyInAnyOrder("SPEAKING_SIGNAL_INVALID", "SPEAKING_SIGNAL_INVALID", "SPEAKING_SIGNAL_INVALID");
        }
    }
}
