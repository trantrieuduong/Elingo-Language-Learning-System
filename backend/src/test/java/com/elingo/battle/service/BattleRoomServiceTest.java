package com.elingo.battle.service;

import com.elingo.battle.dto.response.RoomCreatedResponse;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleRoomServiceImpl;
import com.elingo.battle.state.BattleRoomRegistry;
import com.elingo.battle.state.LiveRoom;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.battle.websocket.BattleEvents;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleRoomServiceTest {

    @Mock
    private BattleRoomRegistry registry;

    @Mock
    private BattleMatchService matchService;

    @Mock
    private BattleMatchPersistenceService persistenceService;

    @Mock
    private BattleQueueService queueService;

    @Mock
    private BattleEventPublisher eventPublisher;

    @InjectMocks
    private BattleRoomServiceImpl roomService;

    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final String ROOM_CODE = "ABC234";
    private static final BattleMode MODE = BattleMode.MULTIPLE_CHOICE;
    private static final String ROOM_CODE_PATTERN = "[A-HJ-NP-Z2-9]{6}";

    private LiveRoom buildRoom() {
        return new LiveRoom(ROOM_CODE, HOST_ID, MODE);
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
            roomService.createRoom(HOST_ID, MODE);

            ArgumentCaptor<LiveRoom> roomCaptor = ArgumentCaptor.forClass(LiveRoom.class);
            verify(registry).register(roomCaptor.capture());
            LiveRoom room = roomCaptor.getValue();
            assertThat(room.hostId()).isEqualTo(HOST_ID);
            assertThat(room.mode()).isEqualTo(MODE);
            assertThat(room.roomCode()).matches(ROOM_CODE_PATTERN);

            verify(registry).removeByHost(HOST_ID);
            verify(eventPublisher).sendToUser(HOST_ID, BattleEvents.ROOM_CREATED,
                    new RoomCreatedResponse(room.roomCode(), MODE));
        }

        @Test
        @DisplayName("Create room failed: User is already waiting in queue")
        void createRoom_Fail_UserInQueue() {
            when(queueService.isUserInQueue(HOST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.createRoom(HOST_ID, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            verify(registry, never()).register(any(LiveRoom.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Create room failed: User is already in an active match")
        void createRoom_Fail_UserInActiveMatch() {
            when(matchService.isUserInActiveMatch(HOST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.createRoom(HOST_ID, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            verify(registry, never()).register(any(LiveRoom.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Create room retries when generated code already exists in registry")
        void createRoom_Success_RetryOnRegistryCollision() {
            when(registry.codeExists(anyString())).thenReturn(true, false);

            roomService.createRoom(HOST_ID, MODE);

            verify(registry, times(2)).codeExists(anyString());
            verify(registry).register(any(LiveRoom.class));
            verify(eventPublisher).sendToUser(any(), any(), any());
        }

        @Test
        @DisplayName("Create room retries when generated code already exists in database")
        void createRoom_Success_RetryOnDatabaseCollision() {
            when(persistenceService.roomCodeExists(anyString())).thenReturn(true, false);

            roomService.createRoom(HOST_ID, MODE);

            verify(persistenceService, times(2)).roomCodeExists(anyString());
            verify(registry).register(any(LiveRoom.class));
        }

        @Test
        @DisplayName("Create room failed: Cannot generate a unique room code")
        void createRoom_Fail_CannotGenerateUniqueCode() {
            when(registry.codeExists(anyString())).thenReturn(true);

            assertThatThrownBy(() -> roomService.createRoom(HOST_ID, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.UNCATEGORIZED_EXCEPTION));

            verify(registry, times(10)).codeExists(anyString());
            verify(registry, never()).register(any(LiveRoom.class));
            verifyNoInteractions(eventPublisher);
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
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));

            roomService.joinRoom(GUEST_ID, ROOM_CODE);

            verify(registry).removeByCode(ROOM_CODE);
            verify(matchService).createRoomMatch(HOST_ID, GUEST_ID, MODE, ROOM_CODE);
            verify(eventPublisher, never()).sendError(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Join room successfully with lowercase and surrounding spaces in room code")
        void joinRoom_Success_NormalizesRoomCode() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));

            roomService.joinRoom(GUEST_ID, "  abc234 ");

            verify(matchService).createRoomMatch(HOST_ID, GUEST_ID, MODE, ROOM_CODE);
        }

        @Test
        @DisplayName("Join room failed: Room not found")
        void joinRoom_Fail_RoomNotFound() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ROOM_NOT_FOUND));

            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Host cannot join their own room")
        void joinRoom_Fail_HostJoinsOwnRoom() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));

            assertThatThrownBy(() -> roomService.joinRoom(HOST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ROOM_NOT_FOUND));

            verify(registry, never()).removeByCode(anyString());
            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Guest is hosting another room")
        void joinRoom_Fail_GuestIsHosting() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            when(registry.isHosting(GUEST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Guest is already waiting in queue")
        void joinRoom_Fail_GuestInQueue() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            when(queueService.isUserInQueue(GUEST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            verify(registry, never()).removeByCode(anyString());
            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Guest is already in an active match")
        void joinRoom_Fail_GuestInActiveMatch() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            when(matchService.isUserInActiveMatch(GUEST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ALREADY_PARTICIPATED));

            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Host has joined the queue meanwhile and room is removed")
        void joinRoom_Fail_HostInQueue() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            when(queueService.isUserInQueue(GUEST_ID)).thenReturn(false);
            when(queueService.isUserInQueue(HOST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ROOM_NOT_FOUND));

            verify(registry).removeByCode(ROOM_CODE);
            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room failed: Host is in an active match meanwhile and room is removed")
        void joinRoom_Fail_HostInActiveMatch() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            when(matchService.isUserInActiveMatch(GUEST_ID)).thenReturn(false);
            when(matchService.isUserInActiveMatch(HOST_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_ROOM_NOT_FOUND));

            verify(registry).removeByCode(ROOM_CODE);
            verify(matchService, never()).createRoomMatch(anyLong(), anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("Join room sends error to both users when match creation fails with business error")
        void joinRoom_Fail_MatchCreationBusinessError() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            doThrow(new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS))
                    .when(matchService).createRoomMatch(HOST_ID, GUEST_ID, MODE, ROOM_CODE);

            assertThatCode(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE)).doesNotThrowAnyException();

            verify(registry).removeByCode(ROOM_CODE);
            verify(eventPublisher).sendError(HOST_ID, AppError.BATTLE_NOT_ENOUGH_CARDS.getCode(),
                    AppError.BATTLE_NOT_ENOUGH_CARDS.getMessage());
            verify(eventPublisher).sendError(GUEST_ID, AppError.BATTLE_NOT_ENOUGH_CARDS.getCode(),
                    AppError.BATTLE_NOT_ENOUGH_CARDS.getMessage());
        }

        @Test
        @DisplayName("Join room sends internal error to both users when match creation fails unexpectedly")
        void joinRoom_Fail_MatchCreationUnexpectedError() {
            when(registry.getByCode(ROOM_CODE)).thenReturn(Optional.of(buildRoom()));
            doThrow(new IllegalStateException("boom"))
                    .when(matchService).createRoomMatch(HOST_ID, GUEST_ID, MODE, ROOM_CODE);

            assertThatCode(() -> roomService.joinRoom(GUEST_ID, ROOM_CODE)).doesNotThrowAnyException();

            verify(eventPublisher).sendError(HOST_ID, AppError.INTERNAL_SERVER_ERROR.getCode(),
                    AppError.INTERNAL_SERVER_ERROR.getMessage());
            verify(eventPublisher).sendError(GUEST_ID, AppError.INTERNAL_SERVER_ERROR.getCode(),
                    AppError.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    // ========================================================================
    // leaveRoom
    // ========================================================================
    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoomTests {

        @Test
        @DisplayName("Leave room successfully when user is hosting")
        void leaveRoom_Success() {
            when(registry.removeByHost(HOST_ID)).thenReturn(Optional.of(buildRoom()));

            roomService.leaveRoom(HOST_ID);

            verify(registry).removeByHost(HOST_ID);
            verify(eventPublisher).sendToUser(HOST_ID, BattleEvents.ROOM_LEFT, Collections.emptyMap());
        }

        @Test
        @DisplayName("Leave room still notifies user when they are not hosting any room")
        void leaveRoom_Success_NotHosting() {
            roomService.leaveRoom(HOST_ID);

            verify(registry).removeByHost(HOST_ID);
            verify(eventPublisher).sendToUser(HOST_ID, BattleEvents.ROOM_LEFT, Collections.emptyMap());
        }
    }

    // ========================================================================
    // onDisconnect
    // ========================================================================
    @Nested
    @DisplayName("onDisconnect")
    class OnDisconnectTests {

        @Test
        @DisplayName("Remove hosted room on disconnect without notifying user")
        void onDisconnect_Success() {
            when(registry.removeByHost(HOST_ID)).thenReturn(Optional.of(buildRoom()));

            roomService.onDisconnect(HOST_ID);

            verify(registry).removeByHost(HOST_ID);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("On disconnect does nothing when user is not hosting")
        void onDisconnect_Success_NotHosting() {
            assertThatCode(() -> roomService.onDisconnect(HOST_ID)).doesNotThrowAnyException();

            verify(registry).removeByHost(HOST_ID);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("On disconnect swallows registry errors")
        void onDisconnect_Success_RegistryError() {
            when(registry.removeByHost(HOST_ID)).thenThrow(new IllegalStateException("boom"));

            assertThatCode(() -> roomService.onDisconnect(HOST_ID)).doesNotThrowAnyException();

            verifyNoInteractions(eventPublisher);
        }
    }
}
