package com.elingo.battle.controller;

import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.QueueJoinRequest;
import com.elingo.battle.dto.request.RejoinRequest;
import com.elingo.battle.dto.request.RoomCreateRequest;
import com.elingo.battle.dto.request.RoomJoinRequest;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleSocketControllerTest {

    @Mock
    private BattleQueueService battleQueueService;

    @Mock
    private BattleEngineService battleEngineService;

    @Mock
    private BattleRoomService battleRoomService;

    @Mock
    private BattleConnectionService battleConnectionService;

    @InjectMocks
    private BattleSocketController battleSocketController;

    private static final Long USER_ID = 1L;
    private static final Long MATCH_ID = 100L;
    private static final Long QUESTION_ID = 1000L;
    private static final String ROOM_CODE = "ABC234";

    private Principal principal;

    @BeforeEach
    void setUp() {
        // Principal.getName() của WebSocket session chính là userId (dạng chuỗi)
        principal = () -> String.valueOf(USER_ID);
    }

    // ========================================================================
    // joinQueue
    // ========================================================================
    @Nested
    @DisplayName("joinQueue")
    class JoinQueueTests {

        @Test
        @DisplayName("Join queue successfully with multiple choice mode")
        void joinQueue_Success_MultipleChoice() {
            battleSocketController.joinQueue(new QueueJoinRequest(BattleMode.MULTIPLE_CHOICE), principal);

            verify(battleQueueService).join(USER_ID, BattleMode.MULTIPLE_CHOICE);
        }

        @Test
        @DisplayName("Join queue successfully with typing mode")
        void joinQueue_Success_Typing() {
            battleSocketController.joinQueue(new QueueJoinRequest(BattleMode.TYPING), principal);

            verify(battleQueueService).join(USER_ID, BattleMode.TYPING);
        }

        @Test
        @DisplayName("Join queue failed: User is already in an active match")
        void joinQueue_Fail_AlreadyParticipated() {
            doThrow(new AppException(AppError.BATTLE_ALREADY_PARTICIPATED))
                    .when(battleQueueService).join(USER_ID, BattleMode.TYPING);

            assertThatThrownBy(() -> battleSocketController.joinQueue(new QueueJoinRequest(BattleMode.TYPING), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));
        }

        @Test
        @DisplayName("Join queue failed: Principal name is not a valid user id")
        void joinQueue_Fail_InvalidPrincipal() {
            Principal invalidPrincipal = () -> "not-a-number";

            assertThatThrownBy(() -> battleSocketController.joinQueue(new QueueJoinRequest(BattleMode.TYPING), invalidPrincipal))
                    .isInstanceOf(NumberFormatException.class);

            verify(battleQueueService, never()).join(anyLong(), any());
        }
    }

    // ========================================================================
    // leaveQueue
    // ========================================================================
    @Nested
    @DisplayName("leaveQueue")
    class LeaveQueueTests {

        @Test
        @DisplayName("Leave queue successfully")
        void leaveQueue_Success() {
            battleSocketController.leaveQueue(principal);

            verify(battleQueueService).leave(USER_ID);
        }
    }

    // ========================================================================
    // answer
    // ========================================================================
    @Nested
    @DisplayName("answer")
    class AnswerTests {

        @Test
        @DisplayName("Submit multiple choice answer successfully")
        void answer_Success_MultipleChoice() {
            BattleAnswerRequest request = new BattleAnswerRequest(MATCH_ID, QUESTION_ID, 5L, null);

            battleSocketController.answer(request, principal);

            verify(battleEngineService).submitAnswer(USER_ID, request);
        }

        @Test
        @DisplayName("Submit typing answer successfully")
        void answer_Success_Typing() {
            BattleAnswerRequest request = new BattleAnswerRequest(MATCH_ID, QUESTION_ID, null, "ice cream");

            battleSocketController.answer(request, principal);

            verify(battleEngineService).submitAnswer(USER_ID, request);
        }

        @Test
        @DisplayName("Submit answer failed: Question timeout")
        void answer_Fail_Timeout() {
            BattleAnswerRequest request = new BattleAnswerRequest(MATCH_ID, QUESTION_ID, 5L, null);
            doThrow(new AppException(AppError.BATTLE_QUESTION_TIMEOUT))
                    .when(battleEngineService).submitAnswer(USER_ID, request);

            assertThatThrownBy(() -> battleSocketController.answer(request, principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_QUESTION_TIMEOUT));
        }

        @Test
        @DisplayName("Submit answer failed: Question already answered")
        void answer_Fail_AlreadyAnswered() {
            BattleAnswerRequest request = new BattleAnswerRequest(MATCH_ID, QUESTION_ID, 5L, null);
            doThrow(new AppException(AppError.BATTLE_QUESTION_ALREADY_ANSWERED))
                    .when(battleEngineService).submitAnswer(USER_ID, request);

            assertThatThrownBy(() -> battleSocketController.answer(request, principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_QUESTION_ALREADY_ANSWERED));
        }
    }

    // ========================================================================
    // createRoom
    // ========================================================================
    @Nested
    @DisplayName("createRoom")
    class CreateRoomTests {

        @Test
        @DisplayName("Create room successfully")
        void createRoom_Success() {
            battleSocketController.createRoom(new RoomCreateRequest(BattleMode.MULTIPLE_CHOICE), principal);

            verify(battleRoomService).createRoom(USER_ID, BattleMode.MULTIPLE_CHOICE);
        }

        @Test
        @DisplayName("Create room failed: User is already in queue or active match")
        void createRoom_Fail_AlreadyParticipated() {
            doThrow(new AppException(AppError.BATTLE_ALREADY_PARTICIPATED))
                    .when(battleRoomService).createRoom(USER_ID, BattleMode.TYPING);

            assertThatThrownBy(() -> battleSocketController.createRoom(new RoomCreateRequest(BattleMode.TYPING), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));
        }
    }

    // ========================================================================
    // joinRoom
    // ========================================================================
    @Nested
    @DisplayName("joinRoom")
    class JoinRoomTests {

        @Test
        @DisplayName("Join room successfully")
        void joinRoom_Success() {
            battleSocketController.joinRoom(new RoomJoinRequest(ROOM_CODE), principal);

            verify(battleRoomService).joinRoom(USER_ID, ROOM_CODE);
        }

        @Test
        @DisplayName("Join room failed: Room not found")
        void joinRoom_Fail_RoomNotFound() {
            doThrow(new AppException(AppError.BATTLE_ROOM_NOT_FOUND))
                    .when(battleRoomService).joinRoom(USER_ID, ROOM_CODE);

            assertThatThrownBy(() -> battleSocketController.joinRoom(new RoomJoinRequest(ROOM_CODE), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ROOM_NOT_FOUND));
        }
    }

    // ========================================================================
    // leaveRoom
    // ========================================================================
    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoomTests {

        @Test
        @DisplayName("Leave room successfully")
        void leaveRoom_Success() {
            battleSocketController.leaveRoom(principal);

            verify(battleRoomService).leaveRoom(USER_ID);
        }
    }

    // ========================================================================
    // rejoin
    // ========================================================================
    @Nested
    @DisplayName("rejoin")
    class RejoinTests {

        @Test
        @DisplayName("Rejoin match successfully")
        void rejoin_Success() {
            battleSocketController.rejoin(new RejoinRequest(MATCH_ID), principal);

            verify(battleConnectionService).rejoin(USER_ID, MATCH_ID);
        }

        @Test
        @DisplayName("Rejoin match failed: Match not found")
        void rejoin_Fail_MatchNotFound() {
            doThrow(new AppException(AppError.BATTLE_MATCH_NOT_FOUND))
                    .when(battleConnectionService).rejoin(USER_ID, MATCH_ID);

            assertThatThrownBy(() -> battleSocketController.rejoin(new RejoinRequest(MATCH_ID), principal))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));
        }
    }

    // ========================================================================
    // activeMatch
    // ========================================================================
    @Nested
    @DisplayName("activeMatch")
    class ActiveMatchTests {

        @Test
        @DisplayName("Request active match successfully")
        void activeMatch_Success() {
            battleSocketController.activeMatch(principal);

            verify(battleConnectionService).sendActiveMatch(USER_ID);
        }
    }

    // ========================================================================
    // Request validation (@Valid trên payload của @MessageMapping)
    // ========================================================================
    @Nested
    @DisplayName("request validation")
    class RequestValidationTests {

        private Validator validator;

        @BeforeEach
        void initValidator() {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        private <T> List<String> violationMessages(T target) {
            return validator.validate(target).stream()
                    .map(ConstraintViolation::getMessage)
                    .toList();
        }

        @Test
        @DisplayName("QueueJoinRequest is valid when mode is provided")
        void queueJoinRequest_Valid() {
            assertThat(violationMessages(new QueueJoinRequest(BattleMode.TYPING))).isEmpty();
        }

        @Test
        @DisplayName("QueueJoinRequest failed: Missing mode")
        void queueJoinRequest_Fail_MissingMode() {
            assertThat(violationMessages(new QueueJoinRequest(null))).containsExactly("BATTLE_MODE_INVALID");
        }

        @Test
        @DisplayName("RoomCreateRequest failed: Missing mode")
        void roomCreateRequest_Fail_MissingMode() {
            assertThat(violationMessages(new RoomCreateRequest(null))).containsExactly("BATTLE_MODE_INVALID");
        }

        @Test
        @DisplayName("RoomJoinRequest is valid when room code is provided")
        void roomJoinRequest_Valid() {
            assertThat(violationMessages(new RoomJoinRequest(ROOM_CODE))).isEmpty();
        }

        @Test
        @DisplayName("RoomJoinRequest failed: Blank room code")
        void roomJoinRequest_Fail_BlankRoomCode() {
            assertThat(violationMessages(new RoomJoinRequest("   "))).containsExactly("BATTLE_ROOM_NOT_FOUND");
        }

        @Test
        @DisplayName("RoomJoinRequest failed: Missing room code")
        void roomJoinRequest_Fail_MissingRoomCode() {
            assertThat(violationMessages(new RoomJoinRequest(null))).containsExactly("BATTLE_ROOM_NOT_FOUND");
        }

        @Test
        @DisplayName("RejoinRequest failed: Missing match id")
        void rejoinRequest_Fail_MissingMatchId() {
            assertThat(violationMessages(new RejoinRequest(null))).containsExactly("BATTLE_MATCH_NOT_FOUND");
        }

        @Test
        @DisplayName("BattleAnswerRequest is valid when match id and question id are provided")
        void battleAnswerRequest_Valid() {
            assertThat(violationMessages(new BattleAnswerRequest(MATCH_ID, QUESTION_ID, null, "ice cream"))).isEmpty();
        }

        @Test
        @DisplayName("BattleAnswerRequest failed: Missing match id and question id")
        void battleAnswerRequest_Fail_MissingIds() {
            assertThat(violationMessages(new BattleAnswerRequest(null, null, 5L, null)))
                    .containsExactlyInAnyOrder("BATTLE_MATCH_NOT_FOUND", "BATTLE_QUESTION_NOT_FOUND");
        }
    }
}
