package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.speaking.entity.SpeakingRoom;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.mapper.SpeakingMapper;
import com.elingo.speaking.repository.SpeakingParticipantRepository;
import com.elingo.speaking.repository.SpeakingParticipantRepository.RoomCountProjection;
import com.elingo.speaking.repository.SpeakingRoomRepository;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.EndResult;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.LeaveResult;
import com.elingo.speaking.service.impl.SpeakingRoomPersistenceServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingRoomPersistenceServiceTest {

    @Mock
    private SpeakingRoomRepository roomRepository;

    @Mock
    private SpeakingParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpeakingMapper mapper;

    @InjectMocks
    private SpeakingRoomPersistenceServiceImpl persistence;

    private static final Long ROOM_ID = 100L;
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final String ROOM_CODE = "ABC234";

    // Mặc định 3, tối đa 4 để phân biệt được nhánh dùng mặc định và nhánh bị chặn trần
    private static final SpeakingProperties PROPS = new SpeakingProperties(
            3, 4, 16384, List.of("stun:example.org:3478"), 15000L, 30000L, 600000L, 60000L);

    private User host;
    private User guest;

    @BeforeEach
    void setUp() {
        // SpeakingProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(persistence, "props", PROPS);
        host = user(HOST_ID, "Host User");
        guest = user(GUEST_ID, "Guest User");
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private User user(Long id, String fullName) {
        return User.builder().id(id).fullName(fullName).avatarUrl("avatar-" + id).build();
    }

    private SpeakingRoom room(SpeakingRoomStatus status) {
        return SpeakingRoom.builder()
                .id(ROOM_ID)
                .roomCode(ROOM_CODE)
                .host(host)
                .topic("Travel")
                .maxParticipants((short) 4)
                .status(status)
                .isPrivate(false)
                .build();
    }

    private SpeakingParticipant participant(Long id, SpeakingRoom room, User user, ParticipantRole role) {
        return SpeakingParticipant.builder()
                .id(id)
                .room(room)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.now().minusSeconds(100))
                .build();
    }

    private static void assertAppError(ThrowingCallable call, AppError expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("appError", expected);
    }

    private void stubLockedRoom(SpeakingRoom room) {
        when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.of(room));
    }

    private void stubNoActiveParticipants() {
        when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of());
    }

    // ========================================================================
    // createRoom
    // ========================================================================
    @Nested
    @DisplayName("createRoom")
    class CreateRoomTests {

        private ArgumentCaptor<SpeakingRoom> roomCaptor;

        private void stubHappyPath() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(false);
            when(userRepository.findById(HOST_ID)).thenReturn(Optional.of(host));
            when(roomRepository.existsByRoomCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(SpeakingRoom.class))).thenAnswer(invocation -> {
                SpeakingRoom saved = invocation.getArgument(0);
                saved.setId(ROOM_ID);
                return saved;
            });
            stubNoActiveParticipants();
        }

        private SpeakingRoom createAndCaptureRoom(String topic, Integer max, Boolean isPrivate) {
            persistence.createRoom(HOST_ID, topic, max, isPrivate);
            roomCaptor = ArgumentCaptor.forClass(SpeakingRoom.class);
            verify(roomRepository).save(roomCaptor.capture());
            return roomCaptor.getValue();
        }

        @Test
        @DisplayName("Create room saves a waiting room and its host participant")
        void createRoom_Success() {
            stubHappyPath();

            SpeakingRoomDetailResponse detail = persistence.createRoom(HOST_ID, "Travel", 2, true);

            ArgumentCaptor<SpeakingRoom> rooms = ArgumentCaptor.forClass(SpeakingRoom.class);
            verify(roomRepository).save(rooms.capture());
            SpeakingRoom saved = rooms.getValue();
            assertThat(saved.getRoomCode()).matches("[A-HJ-NP-Z2-9]{6}");
            assertThat(saved.getHost()).isSameAs(host);
            assertThat(saved.getTopic()).isEqualTo("Travel");
            assertThat(saved.getMaxParticipants()).isEqualTo((short) 2);
            assertThat(saved.getIsPrivate()).isTrue();
            assertThat(saved.getStatus()).isEqualTo(SpeakingRoomStatus.WAITING);

            ArgumentCaptor<SpeakingParticipant> participants = ArgumentCaptor.forClass(SpeakingParticipant.class);
            verify(participantRepository).save(participants.capture());
            assertThat(participants.getValue().getRole()).isEqualTo(ParticipantRole.HOST);
            assertThat(participants.getValue().getUser()).isSameAs(host);
            assertThat(participants.getValue().getRoom()).isSameAs(saved);

            assertThat(detail.roomCode()).isEqualTo(saved.getRoomCode());
            assertThat(detail.hostId()).isEqualTo(HOST_ID);
            assertThat(detail.status()).isEqualTo(SpeakingRoomStatus.WAITING);
            assertThat(detail.isPrivate()).isTrue();
        }

        @Test
        @DisplayName("Create room returns the active participants of the new room")
        void createRoom_Success_ReturnsParticipants() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(false);
            when(userRepository.findById(HOST_ID)).thenReturn(Optional.of(host));
            when(roomRepository.existsByRoomCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(SpeakingRoom.class))).thenAnswer(invocation -> {
                SpeakingRoom saved = invocation.getArgument(0);
                saved.setId(ROOM_ID);
                return saved;
            });
            SpeakingParticipant hostPart = participant(1L, room(SpeakingRoomStatus.WAITING), host, ParticipantRole.HOST);
            SpeakingPeerResponse hostPeer = new SpeakingPeerResponse(HOST_ID, "Host User", "avatar-1", ParticipantRole.HOST);
            when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of(hostPart));
            when(mapper.toPeer(hostPart)).thenReturn(hostPeer);

            SpeakingRoomDetailResponse detail = persistence.createRoom(HOST_ID, null, null, null);

            assertThat(detail.participants()).containsExactly(hostPeer);
        }

        @ParameterizedTest(name = "[{index}] topic \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Create room stores no topic when it is blank")
        void createRoom_Success_BlankTopic(String topic) {
            stubHappyPath();

            SpeakingRoom saved = createAndCaptureRoom(topic, 2, false);

            assertThat(saved.getTopic()).isNull();
        }

        @Test
        @DisplayName("Create room trims the topic")
        void createRoom_Success_TrimmedTopic() {
            stubHappyPath();

            SpeakingRoom saved = createAndCaptureRoom("  IELTS speaking  ", 2, false);

            assertThat(saved.getTopic()).isEqualTo("IELTS speaking");
        }

        @ParameterizedTest(name = "[{index}] requested {0} -> stored {1}")
        @CsvSource({"2, 2", "3, 3", "4, 4", "10, 4"})
        @DisplayName("Create room caps the capacity at the configured limit")
        void createRoom_Success_CapacityLimit(int requested, int expected) {
            stubHappyPath();

            SpeakingRoom saved = createAndCaptureRoom("t", requested, false);

            assertThat(saved.getMaxParticipants()).isEqualTo((short) expected);
        }

        @Test
        @DisplayName("Create room uses the default capacity when none is requested")
        void createRoom_Success_DefaultCapacity() {
            stubHappyPath();

            SpeakingRoom saved = createAndCaptureRoom("t", null, false);

            assertThat(saved.getMaxParticipants()).isEqualTo((short) PROPS.defaultMaxParticipants());
        }

        @ParameterizedTest(name = "[{index}] isPrivate {0} -> {1}")
        @CsvSource(value = {"true, true", "false, false", "null, false"}, nullValues = "null")
        @DisplayName("Create room treats a missing private flag as public")
        void createRoom_Success_PrivateFlag(Boolean requested, boolean expected) {
            stubHappyPath();

            SpeakingRoom saved = createAndCaptureRoom("t", 2, requested);

            assertThat(saved.getIsPrivate()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Create room retries when the generated code is already taken")
        void createRoom_Success_CodeCollisionRetried() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(false);
            when(userRepository.findById(HOST_ID)).thenReturn(Optional.of(host));
            when(roomRepository.existsByRoomCode(anyString())).thenReturn(true, true, false);
            when(roomRepository.save(any(SpeakingRoom.class))).thenAnswer(invocation -> {
                SpeakingRoom saved = invocation.getArgument(0);
                saved.setId(ROOM_ID);
                return saved;
            });
            stubNoActiveParticipants();

            persistence.createRoom(HOST_ID, "t", 2, false);

            verify(roomRepository, times(3)).existsByRoomCode(anyString());
            verify(roomRepository).save(any(SpeakingRoom.class));
        }

        @Test
        @DisplayName("Create room failed: User is already in another room")
        void createRoom_Fail_AlreadyInRoom() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(true);

            assertAppError(() -> persistence.createRoom(HOST_ID, "t", 2, false), AppError.SPEAKING_ALREADY_IN_ROOM);

            verifyNoInteractions(userRepository);
            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create room failed: User not found")
        void createRoom_Fail_UserNotFound() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(false);
            when(userRepository.findById(HOST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.createRoom(HOST_ID, "t", 2, false), AppError.USER_NOT_FOUND);

            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create room failed: No unique code after 10 attempts")
        void createRoom_Fail_NoUniqueCode() {
            when(participantRepository.existsByUserIdAndLeftAtIsNull(HOST_ID)).thenReturn(false);
            when(userRepository.findById(HOST_ID)).thenReturn(Optional.of(host));
            when(roomRepository.existsByRoomCode(anyString())).thenReturn(true);

            assertAppError(() -> persistence.createRoom(HOST_ID, "t", 2, false), AppError.UNCATEGORIZED_EXCEPTION);

            verify(roomRepository, times(10)).existsByRoomCode(anyString());
            verify(roomRepository, never()).save(any());
            verify(participantRepository, never()).save(any());
        }
    }

    // ========================================================================
    // joinRoom
    // ========================================================================
    @Nested
    @DisplayName("joinRoom")
    class JoinRoomTests {

        private SpeakingPeerResponse peerOf(Long id, ParticipantRole role) {
            return new SpeakingPeerResponse(id, "User " + id, null, role);
        }

        private void stubFreshJoin(SpeakingRoom room) {
            stubLockedRoom(room);
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());
            when(participantRepository.existsByUserIdAndLeftAtIsNull(GUEST_ID)).thenReturn(false);
            when(participantRepository.countByRoomIdAndLeftAtIsNull(ROOM_ID)).thenReturn(1L);
            when(userRepository.findById(GUEST_ID)).thenReturn(Optional.of(guest));
        }

        private void stubMembers(SpeakingRoom room) {
            SpeakingParticipant hostPart = participant(1L, room, host, ParticipantRole.HOST);
            SpeakingParticipant guestPart = participant(2L, room, guest, ParticipantRole.GUEST);
            when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of(hostPart, guestPart));
            when(mapper.toPeer(hostPart)).thenReturn(peerOf(HOST_ID, ParticipantRole.HOST));
            when(mapper.toPeer(guestPart)).thenReturn(peerOf(GUEST_ID, ParticipantRole.GUEST));
        }

        @Test
        @DisplayName("Join room saves a guest participant and returns the room context")
        void joinRoom_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            room.setStartedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0));
            stubFreshJoin(room);
            stubMembers(room);

            GoOnlineContext ctx = persistence.joinRoom(GUEST_ID, ROOM_CODE);

            ArgumentCaptor<SpeakingParticipant> captor = ArgumentCaptor.forClass(SpeakingParticipant.class);
            verify(participantRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(ParticipantRole.GUEST);
            assertThat(captor.getValue().getUser()).isSameAs(guest);
            assertThat(captor.getValue().getRoom()).isSameAs(room);

            assertThat(ctx.roomId()).isEqualTo(ROOM_ID);
            assertThat(ctx.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(ctx.topic()).isEqualTo("Travel");
            assertThat(ctx.hostId()).isEqualTo(HOST_ID);
            assertThat(ctx.maxParticipants()).isEqualTo((short) 4);
            assertThat(ctx.isPrivate()).isFalse();
            assertThat(ctx.me().userId()).isEqualTo(GUEST_ID);
            assertThat(ctx.members()).extracting(SpeakingPeerResponse::userId).containsExactly(HOST_ID, GUEST_ID);
            long expectedMs = LocalDateTime.of(2026, 1, 1, 10, 0, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            assertThat(ctx.startedAtMs()).isEqualTo(expectedMs);
        }

        @Test
        @DisplayName("Join room normalizes the room code before looking it up")
        void joinRoom_Success_NormalizesCode() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubFreshJoin(room);
            stubMembers(room);

            GoOnlineContext ctx = persistence.joinRoom(GUEST_ID, "  abc234 ");

            assertThat(ctx.roomCode()).isEqualTo(ROOM_CODE);
            verify(roomRepository).findByRoomCodeForUpdate(ROOM_CODE);
        }

        @Test
        @DisplayName("Join room activates a waiting room and stamps its start time")
        void joinRoom_Success_ActivatesWaitingRoom() {
            SpeakingRoom room = room(SpeakingRoomStatus.WAITING);
            stubFreshJoin(room);
            stubMembers(room);
            LocalDateTime before = LocalDateTime.now();

            GoOnlineContext ctx = persistence.joinRoom(GUEST_ID, ROOM_CODE);

            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(room.getStartedAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
            assertThat(ctx.startedAtMs()).isNotNull();
        }

        @Test
        @DisplayName("Join room keeps the original start time of an active room")
        void joinRoom_Success_KeepsStartTimeOfActiveRoom() {
            LocalDateTime startedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            room.setStartedAt(startedAt);
            stubFreshJoin(room);
            stubMembers(room);

            persistence.joinRoom(GUEST_ID, ROOM_CODE);

            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(room.getStartedAt()).isEqualTo(startedAt);
        }

        @Test
        @DisplayName("Join room allows the last free seat")
        void joinRoom_Success_LastSeat() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());
            when(participantRepository.existsByUserIdAndLeftAtIsNull(GUEST_ID)).thenReturn(false);
            when(participantRepository.countByRoomIdAndLeftAtIsNull(ROOM_ID)).thenReturn(3L);
            when(userRepository.findById(GUEST_ID)).thenReturn(Optional.of(guest));
            stubMembers(room);

            persistence.joinRoom(GUEST_ID, ROOM_CODE);

            verify(participantRepository).save(any(SpeakingParticipant.class));
        }

        @Test
        @DisplayName("Join room is idempotent for a user who is already in this room")
        void joinRoom_Success_AlreadyMember() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            SpeakingParticipant existing = participant(2L, room, guest, ParticipantRole.GUEST);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.of(existing));
            stubMembers(room);

            GoOnlineContext ctx = persistence.joinRoom(GUEST_ID, ROOM_CODE);

            assertThat(ctx.me().userId()).isEqualTo(GUEST_ID);
            verify(participantRepository, never()).save(any());
            verify(participantRepository, never()).existsByUserIdAndLeftAtIsNull(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Join room returns a null start time for a room that has not started")
        void joinRoom_Success_NotStartedYet() {
            SpeakingRoom room = room(SpeakingRoomStatus.WAITING);
            stubLockedRoom(room);
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, HOST_ID)).thenReturn(false);
            SpeakingParticipant existing = participant(1L, room, host, ParticipantRole.HOST);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.of(existing));
            when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of(existing));
            when(mapper.toPeer(existing)).thenReturn(peerOf(HOST_ID, ParticipantRole.HOST));

            GoOnlineContext ctx = persistence.joinRoom(HOST_ID, ROOM_CODE);

            assertThat(ctx.startedAtMs()).isNull();
            assertThat(ctx.me().role()).isEqualTo(ParticipantRole.HOST);
        }

        @Test
        @DisplayName("Join room failed: Room not found")
        void joinRoom_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Join room failed: Room is closed")
        void joinRoom_Fail_RoomClosed() {
            stubLockedRoom(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_CLOSED);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Join room failed: User was kicked from the room")
        void joinRoom_Fail_Banned() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(true);

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_BANNED);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Join room failed: User is already in another room")
        void joinRoom_Fail_AlreadyInAnotherRoom() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());
            when(participantRepository.existsByUserIdAndLeftAtIsNull(GUEST_ID)).thenReturn(true);

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ALREADY_IN_ROOM);

            verify(participantRepository, never()).save(any());
        }

        @ParameterizedTest(name = "[{index}] {0} participants in a room of 4")
        @ValueSource(longs = {4, 5})
        @DisplayName("Join room failed: Room is full")
        void joinRoom_Fail_RoomFull(long current) {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());
            when(participantRepository.existsByUserIdAndLeftAtIsNull(GUEST_ID)).thenReturn(false);
            when(participantRepository.countByRoomIdAndLeftAtIsNull(ROOM_ID)).thenReturn(current);

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_FULL);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Join room failed: User not found")
        void joinRoom_Fail_UserNotFound() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());
            when(participantRepository.existsByUserIdAndLeftAtIsNull(GUEST_ID)).thenReturn(false);
            when(participantRepository.countByRoomIdAndLeftAtIsNull(ROOM_ID)).thenReturn(1L);
            when(userRepository.findById(GUEST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.USER_NOT_FOUND);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Join room failed: Member is missing from the active list")
        void joinRoom_Fail_MemberMissingFromActiveList() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            when(participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID)).thenReturn(false);
            SpeakingParticipant existing = participant(2L, room, guest, ParticipantRole.GUEST);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.of(existing));
            when(participantRepository.findActiveByRoomId(ROOM_ID))
                    .thenReturn(List.of(participant(1L, room, host, ParticipantRole.HOST)));

            assertAppError(() -> persistence.joinRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }
    }

    // ========================================================================
    // leaveRoom
    // ========================================================================
    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoomTests {

        private SpeakingRoom room;
        private SpeakingParticipant hostPart;
        private SpeakingParticipant guestPart;

        @BeforeEach
        void setUpRoom() {
            room = room(SpeakingRoomStatus.ACTIVE);
            hostPart = participant(1L, room, host, ParticipantRole.HOST);
            guestPart = participant(2L, room, guest, ParticipantRole.GUEST);
        }

        private void stubLeaver(Long userId, SpeakingParticipant me, List<SpeakingParticipant> activeBeforeLeave) {
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, userId)).thenReturn(Optional.of(me));
            when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(activeBeforeLeave);
        }

        @Test
        @DisplayName("Guest leaving marks the participant as left and keeps the room open")
        void leaveRoom_Success_Guest() {
            stubLeaver(GUEST_ID, guestPart, List.of(hostPart, guestPart));
            LocalDateTime before = LocalDateTime.now();

            LeaveResult result = persistence.leaveRoom(GUEST_ID, ROOM_CODE);

            assertThat(guestPart.getLeftAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
            assertThat(result.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(result.topic()).isEqualTo("Travel");
            assertThat(result.durationSeconds()).isBetween(99L, 110L);
            assertThat(result.roomClosed()).isFalse();
            assertThat(result.newHostId()).isNull();
            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(room.getHost()).isSameAs(host);
        }

        @Test
        @DisplayName("Host leaving hands the host role to the longest-waiting remaining member")
        void leaveRoom_Success_HostTransfersRole() {
            SpeakingParticipant thirdPart = participant(3L, room, user(OTHER_ID, "Third"), ParticipantRole.GUEST);
            stubLeaver(HOST_ID, hostPart, List.of(hostPart, guestPart, thirdPart));

            LeaveResult result = persistence.leaveRoom(HOST_ID, ROOM_CODE);

            assertThat(result.newHostId()).isEqualTo(GUEST_ID);
            assertThat(result.roomClosed()).isFalse();
            assertThat(guestPart.getRole()).isEqualTo(ParticipantRole.HOST);
            assertThat(room.getHost()).isSameAs(guest);
            assertThat(thirdPart.getRole()).isEqualTo(ParticipantRole.GUEST);
        }

        @Test
        @DisplayName("Last member leaving closes the room")
        void leaveRoom_Success_LastMemberClosesRoom() {
            stubLeaver(HOST_ID, hostPart, List.of(hostPart));
            LocalDateTime before = LocalDateTime.now();

            LeaveResult result = persistence.leaveRoom(HOST_ID, ROOM_CODE);

            assertThat(result.roomClosed()).isTrue();
            assertThat(result.newHostId()).isNull();
            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.CLOSED);
            assertThat(room.getEndedAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
        }

        @Test
        @DisplayName("Leave room normalizes the room code and reports the normalized code")
        void leaveRoom_Success_NormalizesCode() {
            stubLeaver(GUEST_ID, guestPart, List.of(hostPart, guestPart));

            LeaveResult result = persistence.leaveRoom(GUEST_ID, " abc234 ");

            assertThat(result.roomCode()).isEqualTo(ROOM_CODE);
        }

        @Test
        @DisplayName("Leave room never reports a negative duration")
        void leaveRoom_Success_NegativeDurationClamped() {
            guestPart.setJoinedAt(LocalDateTime.now().plusHours(1));
            stubLeaver(GUEST_ID, guestPart, List.of(hostPart, guestPart));

            LeaveResult result = persistence.leaveRoom(GUEST_ID, ROOM_CODE);

            assertThat(result.durationSeconds()).isZero();
        }

        @Test
        @DisplayName("Leave room failed: Room not found")
        void leaveRoom_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.leaveRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Leave room failed: User is not an active participant")
        void leaveRoom_Fail_ParticipantNotFound() {
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.leaveRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);

            verify(participantRepository, never()).findActiveByRoomId(any());
        }
    }

    // ========================================================================
    // transferHost
    // ========================================================================
    @Nested
    @DisplayName("transferHost")
    class TransferHostTests {

        @Test
        @DisplayName("Transfer host swaps the roles and updates the room host")
        void transferHost_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant hostPart = participant(1L, room, host, ParticipantRole.HOST);
            SpeakingParticipant guestPart = participant(2L, room, guest, ParticipantRole.GUEST);
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.of(guestPart));
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.of(hostPart));
            stubNoActiveParticipants();

            SpeakingRoomDetailResponse detail = persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(hostPart.getRole()).isEqualTo(ParticipantRole.GUEST);
            assertThat(guestPart.getRole()).isEqualTo(ParticipantRole.HOST);
            assertThat(room.getHost()).isSameAs(guest);
            assertThat(detail.hostId()).isEqualTo(GUEST_ID);
        }

        @Test
        @DisplayName("Transfer host failed: Room not found")
        void transferHost_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Transfer host failed: Room is closed")
        void transferHost_Fail_RoomClosed() {
            stubLockedRoom(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_CLOSED);
        }

        @Test
        @DisplayName("Transfer host failed: Caller is not the host")
        void transferHost_Fail_NotHost() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.transferHost(GUEST_ID, ROOM_CODE, OTHER_ID), AppError.SPEAKING_NOT_HOST);

            verify(participantRepository, never()).findByRoomIdAndUserIdAndLeftAtIsNull(any(), any());
        }

        @Test
        @DisplayName("Transfer host failed: Host cannot transfer to themselves")
        void transferHost_Fail_SelfTransfer() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.transferHost(HOST_ID, ROOM_CODE, HOST_ID), AppError.INVALID_REQUEST);
        }

        @Test
        @DisplayName("Transfer host failed: Target is not an active participant")
        void transferHost_Fail_TargetNotFound() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Transfer host failed: Host is no longer an active participant")
        void transferHost_Fail_HostParticipantNotFound() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID))
                    .thenReturn(Optional.of(participant(2L, room, guest, ParticipantRole.GUEST)));
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.transferHost(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
            assertThat(room.getHost()).isSameAs(host);
        }
    }

    // ========================================================================
    // endRoom
    // ========================================================================
    @Nested
    @DisplayName("endRoom")
    class EndRoomTests {

        @Test
        @DisplayName("End room closes the room and every open participant")
        void endRoom_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant hostPart = participant(1L, room, host, ParticipantRole.HOST);
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.of(hostPart));
            LocalDateTime before = LocalDateTime.now();

            EndResult result = persistence.endRoom(HOST_ID, ROOM_CODE);

            verify(participantRepository).closeOpenParticipants(eq(ROOM_ID), any(LocalDateTime.class));
            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.CLOSED);
            assertThat(room.getEndedAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
            assertThat(result.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(result.topic()).isEqualTo("Travel");
            assertThat(result.durationSeconds()).isBetween(99L, 110L);
        }

        @Test
        @DisplayName("End room never reports a negative duration")
        void endRoom_Success_NegativeDurationClamped() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant hostPart = participant(1L, room, host, ParticipantRole.HOST);
            hostPart.setJoinedAt(LocalDateTime.now().plusHours(1));
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.of(hostPart));

            assertThat(persistence.endRoom(HOST_ID, ROOM_CODE).durationSeconds()).isZero();
        }

        @Test
        @DisplayName("End room failed: Room not found")
        void endRoom_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.endRoom(HOST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("End room failed: Room is already closed")
        void endRoom_Fail_RoomClosed() {
            stubLockedRoom(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.endRoom(HOST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_CLOSED);
        }

        @Test
        @DisplayName("End room failed: Caller is not the host")
        void endRoom_Fail_NotHost() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);

            assertAppError(() -> persistence.endRoom(GUEST_ID, ROOM_CODE), AppError.SPEAKING_NOT_HOST);

            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            verify(participantRepository, never()).closeOpenParticipants(any(), any());
        }

        @Test
        @DisplayName("End room failed: Host is no longer an active participant")
        void endRoom_Fail_ParticipantNotFound() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, HOST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.endRoom(HOST_ID, ROOM_CODE), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);

            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
        }
    }

    // ========================================================================
    // kickMember
    // ========================================================================
    @Nested
    @DisplayName("kickMember")
    class KickMemberTests {

        @Test
        @DisplayName("Kick member marks the participant as left and kicked at the same time")
        void kickMember_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant guestPart = participant(2L, room, guest, ParticipantRole.GUEST);
            stubLockedRoom(room);
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.of(guestPart));
            stubNoActiveParticipants();
            LocalDateTime before = LocalDateTime.now();

            SpeakingRoomDetailResponse detail = persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(guestPart.getLeftAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
            assertThat(guestPart.getKickedAt()).isEqualTo(guestPart.getLeftAt());
            assertThat(detail.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(detail.hostId()).isEqualTo(HOST_ID);
        }

        @Test
        @DisplayName("Kick member failed: Room not found")
        void kickMember_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Kick member failed: Room is closed")
        void kickMember_Fail_RoomClosed() {
            stubLockedRoom(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_CLOSED);
        }

        @Test
        @DisplayName("Kick member failed: Caller is not the host")
        void kickMember_Fail_NotHost() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.kickMember(GUEST_ID, ROOM_CODE, OTHER_ID), AppError.SPEAKING_NOT_HOST);
        }

        @Test
        @DisplayName("Kick member failed: Host cannot kick themselves")
        void kickMember_Fail_SelfKick() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.kickMember(HOST_ID, ROOM_CODE, HOST_ID), AppError.INVALID_REQUEST);
        }

        @Test
        @DisplayName("Kick member failed: Target is not an active participant")
        void kickMember_Fail_TargetNotFound() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(ROOM_ID, GUEST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.kickMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
        }
    }

    // ========================================================================
    // listBans
    // ========================================================================
    @Nested
    @DisplayName("listBans")
    class ListBansTests {

        private void stubRoomWithHost(SpeakingRoom room) {
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room));
        }

        @Test
        @DisplayName("List bans maps the kicked participants")
        void listBans_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            LocalDateTime kickedAt = LocalDateTime.of(2026, 1, 1, 10, 30, 0);
            SpeakingParticipant kicked = participant(2L, room, guest, ParticipantRole.GUEST);
            kicked.setKickedAt(kickedAt);
            stubRoomWithHost(room);
            when(participantRepository.findKickedByRoomId(ROOM_ID)).thenReturn(List.of(kicked));

            List<SpeakingBannedUserResponse> bans = persistence.listBans(HOST_ID, " abc234 ");

            assertThat(bans).containsExactly(
                    new SpeakingBannedUserResponse(GUEST_ID, "Guest User", "avatar-2", kickedAt));
        }

        @Test
        @DisplayName("List bans returns an empty list when nobody was kicked")
        void listBans_Success_Empty() {
            stubRoomWithHost(room(SpeakingRoomStatus.WAITING));
            when(participantRepository.findKickedByRoomId(ROOM_ID)).thenReturn(List.of());

            assertThat(persistence.listBans(HOST_ID, ROOM_CODE)).isEmpty();
        }

        @Test
        @DisplayName("List bans failed: Room not found")
        void listBans_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.listBans(HOST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("List bans failed: Room is closed")
        void listBans_Fail_RoomClosed() {
            stubRoomWithHost(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.listBans(HOST_ID, ROOM_CODE), AppError.SPEAKING_ROOM_CLOSED);
        }

        @Test
        @DisplayName("List bans failed: Caller is not the host")
        void listBans_Fail_NotHost() {
            stubRoomWithHost(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.listBans(GUEST_ID, ROOM_CODE), AppError.SPEAKING_NOT_HOST);

            verify(participantRepository, never()).findKickedByRoomId(any());
        }
    }

    // ========================================================================
    // unbanMember
    // ========================================================================
    @Nested
    @DisplayName("unbanMember")
    class UnbanMemberTests {

        @Test
        @DisplayName("Unban member clears the kicked mark and returns the remaining bans")
        void unbanMember_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant kicked = participant(2L, room, guest, ParticipantRole.GUEST);
            kicked.setKickedAt(LocalDateTime.now());
            stubLockedRoom(room);
            when(participantRepository.findFirstByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID))
                    .thenReturn(Optional.of(kicked));
            when(participantRepository.findKickedByRoomId(ROOM_ID)).thenReturn(List.of());

            List<SpeakingBannedUserResponse> remaining = persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID);

            assertThat(kicked.getKickedAt()).isNull();
            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("Unban member failed: Room not found")
        void unbanMember_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Unban member failed: Room is closed")
        void unbanMember_Fail_RoomClosed() {
            stubLockedRoom(room(SpeakingRoomStatus.CLOSED));

            assertAppError(() -> persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_ROOM_CLOSED);
        }

        @Test
        @DisplayName("Unban member failed: Caller is not the host")
        void unbanMember_Fail_NotHost() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));

            assertAppError(() -> persistence.unbanMember(GUEST_ID, ROOM_CODE, OTHER_ID), AppError.SPEAKING_NOT_HOST);
        }

        @Test
        @DisplayName("Unban member failed: Target was never kicked")
        void unbanMember_Fail_NotBanned() {
            stubLockedRoom(room(SpeakingRoomStatus.ACTIVE));
            when(participantRepository.findFirstByRoomIdAndUserIdAndKickedAtIsNotNull(ROOM_ID, GUEST_ID))
                    .thenReturn(Optional.empty());

            assertAppError(() -> persistence.unbanMember(HOST_ID, ROOM_CODE, GUEST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
        }
    }

    // ========================================================================
    // stale / orphan room cleanup
    // ========================================================================
    @Nested
    @DisplayName("room cleanup")
    class CleanupTests {

        @Test
        @DisplayName("Find stale room codes delegates to the repository")
        void findStaleRoomCodes_Success() {
            LocalDateTime before = LocalDateTime.now().minusMinutes(10);
            when(roomRepository.findOpenRoomCodesCreatedBefore(before)).thenReturn(List.of("AAAAAA", "BBBBBB"));

            assertThat(persistence.findStaleRoomCodes(before)).containsExactly("AAAAAA", "BBBBBB");
        }

        @Test
        @DisplayName("Force close room closes the room and its open participants")
        void forceCloseRoom_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            stubLockedRoom(room);
            LocalDateTime before = LocalDateTime.now();

            boolean closed = persistence.forceCloseRoom(ROOM_CODE);

            assertThat(closed).isTrue();
            verify(participantRepository).closeOpenParticipants(eq(ROOM_ID), any(LocalDateTime.class));
            assertThat(room.getStatus()).isEqualTo(SpeakingRoomStatus.CLOSED);
            assertThat(room.getEndedAt()).isNotNull().isAfterOrEqualTo(before.minusSeconds(1));
        }

        @Test
        @DisplayName("Force close room returns false when the room does not exist")
        void forceCloseRoom_RoomNotFound() {
            when(roomRepository.findByRoomCodeForUpdate(ROOM_CODE)).thenReturn(Optional.empty());

            assertThat(persistence.forceCloseRoom(ROOM_CODE)).isFalse();

            verify(participantRepository, never()).closeOpenParticipants(any(), any());
        }

        @Test
        @DisplayName("Force close room returns false when the room is already closed")
        void forceCloseRoom_AlreadyClosed() {
            SpeakingRoom room = room(SpeakingRoomStatus.CLOSED);
            stubLockedRoom(room);

            assertThat(persistence.forceCloseRoom(ROOM_CODE)).isFalse();

            verify(participantRepository, never()).closeOpenParticipants(any(), any());
            assertThat(room.getEndedAt()).isNull();
        }

        @Test
        @DisplayName("Close all open rooms only counts the rooms that were really closed")
        void closeAllOpenRooms_Success() {
            SpeakingRoom open = room(SpeakingRoomStatus.WAITING);
            SpeakingRoom alreadyClosed = room(SpeakingRoomStatus.CLOSED);
            when(roomRepository.findAllOpenRoomCodes()).thenReturn(List.of("AAAAAA", "BBBBBB", "CCCCCC"));
            when(roomRepository.findByRoomCodeForUpdate("AAAAAA")).thenReturn(Optional.of(open));
            when(roomRepository.findByRoomCodeForUpdate("BBBBBB")).thenReturn(Optional.of(alreadyClosed));
            when(roomRepository.findByRoomCodeForUpdate("CCCCCC")).thenReturn(Optional.empty());

            int closed = persistence.closeAllOpenRooms();

            assertThat(closed).isEqualTo(1);
            assertThat(open.getStatus()).isEqualTo(SpeakingRoomStatus.CLOSED);
            verify(participantRepository, times(1)).closeOpenParticipants(eq(ROOM_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Close all open rooms returns zero when there are no open rooms")
        void closeAllOpenRooms_Success_NoOpenRooms() {
            when(roomRepository.findAllOpenRoomCodes()).thenReturn(List.of());

            assertThat(persistence.closeAllOpenRooms()).isZero();

            verify(roomRepository, never()).findByRoomCodeForUpdate(anyString());
        }
    }

    // ========================================================================
    // findActiveRoomCode / findActiveRoomCodeOrNull
    // ========================================================================
    @Nested
    @DisplayName("active room lookup")
    class ActiveRoomTests {

        @Test
        @DisplayName("Find active room code returns the code of the room the user is in")
        void findActiveRoomCode_Success() {
            SpeakingParticipant active = participant(1L, room(SpeakingRoomStatus.ACTIVE), host, ParticipantRole.HOST);
            when(participantRepository.findActiveByUserId(HOST_ID)).thenReturn(Optional.of(active));

            assertThat(persistence.findActiveRoomCode(HOST_ID)).isEqualTo(ROOM_CODE);
        }

        @Test
        @DisplayName("Find active room code failed: User is not in any room")
        void findActiveRoomCode_Fail_NotInRoom() {
            when(participantRepository.findActiveByUserId(HOST_ID)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.findActiveRoomCode(HOST_ID), AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
        }

        @Test
        @DisplayName("Find active room code or null returns the code of the room the user is in")
        void findActiveRoomCodeOrNull_Success() {
            SpeakingParticipant active = participant(1L, room(SpeakingRoomStatus.ACTIVE), host, ParticipantRole.HOST);
            when(participantRepository.findActiveByUserId(HOST_ID)).thenReturn(Optional.of(active));

            assertThat(persistence.findActiveRoomCodeOrNull(HOST_ID)).isEqualTo(ROOM_CODE);
        }

        @Test
        @DisplayName("Find active room code or null returns null when the user is not in any room")
        void findActiveRoomCodeOrNull_NotInRoom() {
            when(participantRepository.findActiveByUserId(HOST_ID)).thenReturn(Optional.empty());

            assertThat(persistence.findActiveRoomCodeOrNull(HOST_ID)).isNull();
        }
    }

    // ========================================================================
    // getRoomDetail
    // ========================================================================
    @Nested
    @DisplayName("getRoomDetail")
    class GetRoomDetailTests {

        @Test
        @DisplayName("Get room detail returns the room with its active participants")
        void getRoomDetail_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant hostPart = participant(1L, room, host, ParticipantRole.HOST);
            SpeakingPeerResponse hostPeer = new SpeakingPeerResponse(HOST_ID, "Host User", "avatar-1", ParticipantRole.HOST);
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room));
            when(participantRepository.findActiveByRoomId(ROOM_ID)).thenReturn(List.of(hostPart));
            when(mapper.toPeer(hostPart)).thenReturn(hostPeer);

            SpeakingRoomDetailResponse detail = persistence.getRoomDetail(" abc234 ");

            assertThat(detail.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(detail.topic()).isEqualTo("Travel");
            assertThat(detail.maxParticipants()).isEqualTo((short) 4);
            assertThat(detail.status()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(detail.isPrivate()).isFalse();
            assertThat(detail.hostId()).isEqualTo(HOST_ID);
            assertThat(detail.participants()).containsExactly(hostPeer);
        }

        @Test
        @DisplayName("Get room detail reports a missing private flag as public")
        void getRoomDetail_Success_NullPrivateFlag() {
            SpeakingRoom room = room(SpeakingRoomStatus.WAITING);
            room.setIsPrivate(null);
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room));
            stubNoActiveParticipants();

            assertThat(persistence.getRoomDetail(ROOM_CODE).isPrivate()).isFalse();
        }

        @Test
        @DisplayName("Get room detail failed: Room not found")
        void getRoomDetail_Fail_RoomNotFound() {
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.empty());

            assertAppError(() -> persistence.getRoomDetail(ROOM_CODE), AppError.SPEAKING_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Get room detail failed: Room is closed")
        void getRoomDetail_Fail_RoomClosed() {
            when(roomRepository.findByRoomCodeWithHost(ROOM_CODE)).thenReturn(Optional.of(room(SpeakingRoomStatus.CLOSED)));

            assertAppError(() -> persistence.getRoomDetail(ROOM_CODE), AppError.SPEAKING_ROOM_CLOSED);
        }
    }

    // ========================================================================
    // getOpenRooms
    // ========================================================================
    @Nested
    @DisplayName("getOpenRooms")
    class GetOpenRoomsTests {

        private RoomCountProjection count(Long roomId, long total) {
            return new RoomCountProjection() {
                @Override
                public Long getRoomId() {
                    return roomId;
                }

                @Override
                public long getTotal() {
                    return total;
                }
            };
        }

        private void stubPage(List<SpeakingRoom> rooms, long total) {
            when(roomRepository.findAll(ArgumentMatchers.<Specification<SpeakingRoom>>any(), any(Pageable.class)))
                    .thenAnswer(invocation -> new PageImpl<>(rooms, invocation.<Pageable>getArgument(1), total));
        }

        @Test
        @DisplayName("Get open rooms maps rooms with their participant counts and host")
        void getOpenRooms_Success() {
            SpeakingRoom room = room(SpeakingRoomStatus.ACTIVE);
            room.setStartedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0));
            room.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0, 0));
            SpeakingPeerResponse hostPeer = new SpeakingPeerResponse(HOST_ID, "Host User", "avatar-1", ParticipantRole.HOST);
            stubPage(List.of(room), 1);
            when(participantRepository.countActiveByRoomIds(List.of(ROOM_ID))).thenReturn(List.of(count(ROOM_ID, 3)));
            when(mapper.toHostPeer(host)).thenReturn(hostPeer);

            PageResponse<SpeakingRoomResponse> response = persistence.getOpenRooms(
                    new SpeakingRoomSearchRequest("travel", 4), PageRequest.of(0, 10));

            assertThat(response.content()).hasSize(1);
            SpeakingRoomResponse item = response.content().get(0);
            assertThat(item.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(item.topic()).isEqualTo("Travel");
            assertThat(item.maxParticipants()).isEqualTo((short) 4);
            assertThat(item.status()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(item.isPrivate()).isFalse();
            assertThat(item.participantCount()).isEqualTo(3L);
            assertThat(item.host()).isEqualTo(hostPeer);
            assertThat(item.startedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0, 0));
            assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0, 0));
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Get open rooms counts zero participants for a room missing from the count query")
        void getOpenRooms_Success_MissingCount() {
            stubPage(List.of(room(SpeakingRoomStatus.WAITING)), 1);
            when(participantRepository.countActiveByRoomIds(List.of(ROOM_ID))).thenReturn(List.of());
            when(mapper.toHostPeer(host)).thenReturn(new SpeakingPeerResponse(HOST_ID, "Host User", null, ParticipantRole.HOST));

            PageResponse<SpeakingRoomResponse> response = persistence.getOpenRooms(null, PageRequest.of(0, 10));

            assertThat(response.content().get(0).participantCount()).isZero();
        }

        @Test
        @DisplayName("Get open rooms reports a missing private flag as public")
        void getOpenRooms_Success_NullPrivateFlag() {
            SpeakingRoom room = room(SpeakingRoomStatus.WAITING);
            room.setIsPrivate(null);
            stubPage(List.of(room), 1);
            when(participantRepository.countActiveByRoomIds(List.of(ROOM_ID))).thenReturn(List.of());
            when(mapper.toHostPeer(host)).thenReturn(new SpeakingPeerResponse(HOST_ID, "Host User", null, ParticipantRole.HOST));

            assertThat(persistence.getOpenRooms(null, PageRequest.of(0, 10)).content().get(0).isPrivate()).isFalse();
        }

        @Test
        @DisplayName("Get open rooms skips the count query when the page is empty")
        void getOpenRooms_Success_EmptyPage() {
            stubPage(List.of(), 0);

            PageResponse<SpeakingRoomResponse> response = persistence.getOpenRooms(null, PageRequest.of(0, 10));

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            verify(participantRepository, never()).countActiveByRoomIds(anyList());
        }

        @Test
        @DisplayName("Get open rooms keeps the requested page and size and sorts newest first")
        void getOpenRooms_Success_SortsNewestFirst() {
            stubPage(List.of(), 0);

            persistence.getOpenRooms(new SpeakingRoomSearchRequest(null, null), PageRequest.of(2, 5, Sort.by("topic")));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(roomRepository).findAll(ArgumentMatchers.<Specification<SpeakingRoom>>any(), captor.capture());
            Pageable used = captor.getValue();
            assertThat(used.getPageNumber()).isEqualTo(2);
            assertThat(used.getPageSize()).isEqualTo(5);
            assertThat(used.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }

        @Test
        @DisplayName("Get open rooms accepts a request with a blank keyword")
        void getOpenRooms_Success_BlankKeyword() {
            stubPage(List.of(), 0);

            PageResponse<SpeakingRoomResponse> response = persistence.getOpenRooms(
                    new SpeakingRoomSearchRequest("   ", null), PageRequest.of(0, 10));

            assertThat(response.content()).isEmpty();
        }
    }
}
