package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingHistoryItemResponse;
import com.elingo.speaking.dto.response.SpeakingHistorySummaryResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.speaking.entity.SpeakingRoom;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.repository.SpeakingParticipantRepository;
import com.elingo.speaking.repository.SpeakingParticipantRepository.HistoryRoomRow;
import com.elingo.speaking.repository.SpeakingParticipantRepository.HistorySummaryRow;
import com.elingo.speaking.service.impl.SpeakingHistoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingHistoryServiceTest {

    @Mock
    private SpeakingParticipantRepository participantRepository;

    @InjectMocks
    private SpeakingHistoryServiceImpl historyService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final Long OTHER_ROOM_ID = 11L;
    private static final String ROOM_CODE = "ABC234";
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

    private static HistoryRoomRow row(Long roomId) {
        return new HistoryRoomRow() {
            @Override
            public Long getRoomId() {
                return roomId;
            }

            @Override
            public LocalDateTime getLastLeftAt() {
                return null;
            }
        };
    }

    private SpeakingRoom room(Long id, String code, SpeakingRoomStatus status) {
        return SpeakingRoom.builder()
                .id(id)
                .roomCode(code)
                .topic("Travel")
                .maxParticipants((short) 4)
                .status(status)
                .build();
    }

    private SpeakingParticipant visit(Long id, SpeakingRoom room, ParticipantRole role,
                                      LocalDateTime joinedAt, LocalDateTime leftAt, LocalDateTime kickedAt) {
        return SpeakingParticipant.builder()
                .id(id)
                .room(room)
                .role(role)
                .joinedAt(joinedAt)
                .leftAt(leftAt)
                .kickedAt(kickedAt)
                .build();
    }

    private void stubRows(List<HistoryRoomRow> rows) {
        when(participantRepository.findHistoryRooms(eq(USER_ID), anyString(), anyInt(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), rows.size()));
    }

    // ========================================================================
    // getHistory - query building
    // ========================================================================
    @Nested
    @DisplayName("getHistory - query building")
    class GetHistoryQueryTests {

        private ArgumentCaptor<String> patternCaptor;
        private ArgumentCaptor<Integer> maxCaptor;
        private ArgumentCaptor<Pageable> pageableCaptor;

        private void callAndCapture(SpeakingRoomSearchRequest request, Pageable pageable) {
            stubRows(List.of());

            historyService.getHistory(USER_ID, request, pageable);

            patternCaptor = ArgumentCaptor.forClass(String.class);
            maxCaptor = ArgumentCaptor.forClass(Integer.class);
            pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(participantRepository).findHistoryRooms(
                    eq(USER_ID), patternCaptor.capture(), maxCaptor.capture(), pageableCaptor.capture());
        }

        @Test
        @DisplayName("Get history without a request matches every topic and every capacity")
        void getHistory_NullRequest() {
            callAndCapture(null, PageRequest.of(0, 10));

            assertThat(patternCaptor.getValue()).isEqualTo("%");
            assertThat(maxCaptor.getValue()).isZero();
        }

        @ParameterizedTest(name = "[{index}] keyword \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Get history with a blank keyword matches every topic")
        void getHistory_BlankKeyword(String keyword) {
            callAndCapture(new SpeakingRoomSearchRequest(keyword, null), PageRequest.of(0, 10));

            assertThat(patternCaptor.getValue()).isEqualTo("%");
        }

        @Test
        @DisplayName("Get history lowercases and trims the keyword into a LIKE pattern")
        void getHistory_KeywordPattern() {
            callAndCapture(new SpeakingRoomSearchRequest("  Travel ", null), PageRequest.of(0, 10));

            assertThat(patternCaptor.getValue()).isEqualTo("%travel%");
        }

        @Test
        @DisplayName("Get history escapes LIKE wildcards in the keyword")
        void getHistory_KeywordEscapesWildcards() {
            callAndCapture(new SpeakingRoomSearchRequest("100%_!", null), PageRequest.of(0, 10));

            // "!" phải được escape trước, sau đó mới tới "%" và "_"
            assertThat(patternCaptor.getValue()).isEqualTo("%100!%!_!!%");
        }

        @Test
        @DisplayName("Get history passes the capacity filter when provided")
        void getHistory_MaxParticipants() {
            callAndCapture(new SpeakingRoomSearchRequest(null, 3), PageRequest.of(0, 10));

            assertThat(maxCaptor.getValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("Get history uses zero to disable the capacity filter")
        void getHistory_NoMaxParticipants() {
            callAndCapture(new SpeakingRoomSearchRequest("travel", null), PageRequest.of(0, 10));

            assertThat(maxCaptor.getValue()).isZero();
        }

        @Test
        @DisplayName("Get history keeps the page number and size but drops the requested sort")
        void getHistory_PageableWithoutSort() {
            callAndCapture(null, PageRequest.of(2, 5, Sort.by("createdAt")));

            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
            assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
        }
    }

    // ========================================================================
    // getHistory - result
    // ========================================================================
    @Nested
    @DisplayName("getHistory - result")
    class GetHistoryResultTests {

        @Test
        @DisplayName("Get history returns an empty page without loading visits when there are no rooms")
        void getHistory_Success_Empty() {
            when(participantRepository.findHistoryRooms(eq(USER_ID), anyString(), anyInt(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.<HistoryRoomRow>of(), PageRequest.of(0, 10), 0));

            PageResponse<SpeakingHistoryItemResponse> response = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10));

            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.totalElements()).isZero();
            verify(participantRepository, never()).findFinishedByUserAndRoomIds(any(), anyList());
        }

        @Test
        @DisplayName("Get history maps a room with a single visit")
        void getHistory_Success_SingleVisit() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant visit = visit(1L, room, ParticipantRole.HOST, BASE, BASE.plusMinutes(5), null);
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(visit));

            PageResponse<SpeakingHistoryItemResponse> response = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10));

            assertThat(response.content()).hasSize(1);
            SpeakingHistoryItemResponse item = response.content().get(0);
            assertThat(item.roomCode()).isEqualTo(ROOM_CODE);
            assertThat(item.topic()).isEqualTo("Travel");
            assertThat(item.maxParticipants()).isEqualTo((short) 4);
            assertThat(item.role()).isEqualTo("HOST");
            assertThat(item.visitCount()).isEqualTo(1);
            assertThat(item.joinedAt()).isEqualTo(BASE);
            assertThat(item.leftAt()).isEqualTo(BASE.plusMinutes(5));
            assertThat(item.durationSeconds()).isEqualTo(300L);
            assertThat(item.roomStatus()).isEqualTo(SpeakingRoomStatus.ACTIVE);
            assertThat(item.canRejoin()).isTrue();
        }

        @Test
        @DisplayName("Get history merges several visits of one room into a single row")
        void getHistory_Success_MultipleVisits() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant first = visit(1L, room, ParticipantRole.GUEST, BASE, BASE.plusMinutes(5), null);
            SpeakingParticipant second = visit(2L, room, ParticipantRole.HOST, BASE.plusMinutes(30), BASE.plusMinutes(40), null);
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(first, second));

            SpeakingHistoryItemResponse item = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10))
                    .content().get(0);

            assertThat(item.visitCount()).isEqualTo(2);
            assertThat(item.joinedAt()).isEqualTo(BASE);
            assertThat(item.leftAt()).isEqualTo(BASE.plusMinutes(40));
            assertThat(item.durationSeconds()).isEqualTo(5 * 60 + 10 * 60);
            // Vai trò lấy theo lượt gần nhất
            assertThat(item.role()).isEqualTo("HOST");
        }

        @Test
        @DisplayName("Get history never reports a negative duration for a visit")
        void getHistory_Success_NegativeDurationClamped() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant broken = visit(1L, room, ParticipantRole.GUEST, BASE, BASE.minusMinutes(5), null);
            SpeakingParticipant normal = visit(2L, room, ParticipantRole.GUEST, BASE.plusHours(1), BASE.plusHours(1).plusMinutes(1), null);
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(broken, normal));

            SpeakingHistoryItemResponse item = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10))
                    .content().get(0);

            assertThat(item.durationSeconds()).isEqualTo(60L);
        }

        @Test
        @DisplayName("Get history forbids rejoining a closed room")
        void getHistory_Success_ClosedRoomCannotRejoin() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.CLOSED);
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(visit(1L, room, ParticipantRole.GUEST, BASE, BASE.plusMinutes(5), null)));

            SpeakingHistoryItemResponse item = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10))
                    .content().get(0);

            assertThat(item.canRejoin()).isFalse();
            assertThat(item.roomStatus()).isEqualTo(SpeakingRoomStatus.CLOSED);
        }

        @Test
        @DisplayName("Get history forbids rejoining a room the user was kicked from")
        void getHistory_Success_KickedCannotRejoin() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.ACTIVE);
            SpeakingParticipant ordinary = visit(1L, room, ParticipantRole.GUEST, BASE, BASE.plusMinutes(5), null);
            SpeakingParticipant kicked = visit(2L, room, ParticipantRole.GUEST, BASE.plusMinutes(10), BASE.plusMinutes(12), BASE.plusMinutes(12));
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(ordinary, kicked));

            SpeakingHistoryItemResponse item = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10))
                    .content().get(0);

            assertThat(item.canRejoin()).isFalse();
        }

        @Test
        @DisplayName("Get history allows rejoining a waiting room")
        void getHistory_Success_WaitingRoomCanRejoin() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.WAITING);
            stubRows(List.of(row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(visit(1L, room, ParticipantRole.HOST, BASE, BASE.plusMinutes(1), null)));

            SpeakingHistoryItemResponse item = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10))
                    .content().get(0);

            assertThat(item.canRejoin()).isTrue();
        }

        @Test
        @DisplayName("Get history keeps the row order and groups visits by room")
        void getHistory_Success_MultipleRooms() {
            SpeakingRoom roomA = room(ROOM_ID, "AAAAAA", SpeakingRoomStatus.ACTIVE);
            SpeakingRoom roomB = room(OTHER_ROOM_ID, "BBBBBB", SpeakingRoomStatus.CLOSED);
            stubRows(List.of(row(OTHER_ROOM_ID), row(ROOM_ID)));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(OTHER_ROOM_ID, ROOM_ID)))
                    .thenReturn(List.of(
                            visit(1L, roomA, ParticipantRole.GUEST, BASE, BASE.plusMinutes(2), null),
                            visit(2L, roomB, ParticipantRole.HOST, BASE, BASE.plusMinutes(7), null),
                            visit(3L, roomA, ParticipantRole.GUEST, BASE.plusHours(1), BASE.plusHours(1).plusMinutes(3), null)));

            List<SpeakingHistoryItemResponse> items = historyService.getHistory(USER_ID, null, PageRequest.of(0, 10)).content();

            assertThat(items).extracting(SpeakingHistoryItemResponse::roomCode).containsExactly("BBBBBB", "AAAAAA");
            assertThat(items.get(0).visitCount()).isEqualTo(1);
            assertThat(items.get(0).durationSeconds()).isEqualTo(7 * 60L);
            assertThat(items.get(1).visitCount()).isEqualTo(2);
            assertThat(items.get(1).durationSeconds()).isEqualTo(5 * 60L);
        }

        @Test
        @DisplayName("Get history reports the page metadata")
        void getHistory_Success_PageMetadata() {
            SpeakingRoom room = room(ROOM_ID, ROOM_CODE, SpeakingRoomStatus.ACTIVE);
            when(participantRepository.findHistoryRooms(eq(USER_ID), anyString(), anyInt(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(row(ROOM_ID)), PageRequest.of(1, 1), 3));
            when(participantRepository.findFinishedByUserAndRoomIds(USER_ID, List.of(ROOM_ID)))
                    .thenReturn(List.of(visit(1L, room, ParticipantRole.GUEST, BASE, BASE.plusMinutes(1), null)));

            PageResponse<SpeakingHistoryItemResponse> response = historyService.getHistory(USER_ID, null, PageRequest.of(1, 1));

            assertThat(response.page()).isEqualTo(2);
            assertThat(response.size()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(3);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.isLast()).isFalse();
        }
    }

    // ========================================================================
    // getSummary
    // ========================================================================
    @Nested
    @DisplayName("getSummary")
    class GetSummaryTests {

        @Test
        @DisplayName("Get summary maps the totals of the user")
        void getSummary_Success() {
            HistorySummaryRow summary = mock(HistorySummaryRow.class);
            when(summary.getTotalSeconds()).thenReturn(3600L);
            when(summary.getRoomCount()).thenReturn(4L);
            when(summary.getSessionCount()).thenReturn(6L);
            when(participantRepository.summarize(USER_ID)).thenReturn(summary);

            SpeakingHistorySummaryResponse response = historyService.getSummary(USER_ID);

            assertThat(response.totalSeconds()).isEqualTo(3600L);
            assertThat(response.roomCount()).isEqualTo(4L);
            assertThat(response.sessionCount()).isEqualTo(6L);
        }

        @Test
        @DisplayName("Get summary returns zeros when the query returns nothing")
        void getSummary_Success_NullRow() {
            when(participantRepository.summarize(USER_ID)).thenReturn(null);

            SpeakingHistorySummaryResponse response = historyService.getSummary(USER_ID);

            assertThat(response).isEqualTo(new SpeakingHistorySummaryResponse(0L, 0L, 0L));
        }

        @Test
        @DisplayName("Get summary returns zeros for null columns")
        void getSummary_Success_NullColumns() {
            HistorySummaryRow summary = mock(HistorySummaryRow.class);
            when(summary.getTotalSeconds()).thenReturn(null);
            when(summary.getRoomCount()).thenReturn(null);
            when(summary.getSessionCount()).thenReturn(null);
            when(participantRepository.summarize(USER_ID)).thenReturn(summary);

            SpeakingHistorySummaryResponse response = historyService.getSummary(USER_ID);

            assertThat(response).isEqualTo(new SpeakingHistorySummaryResponse(0L, 0L, 0L));
        }
    }
}
