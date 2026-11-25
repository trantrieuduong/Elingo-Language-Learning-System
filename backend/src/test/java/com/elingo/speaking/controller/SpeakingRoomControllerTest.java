package com.elingo.speaking.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.IceConfigResponse;
import com.elingo.speaking.dto.response.IceServerResponse;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingHistoryItemResponse;
import com.elingo.speaking.dto.response.SpeakingHistorySummaryResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.service.SpeakingHistoryService;
import com.elingo.speaking.service.SpeakingIceService;
import com.elingo.speaking.service.SpeakingRoomService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingRoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SpeakingRoomService speakingRoomService;

    @Mock
    private SpeakingIceService iceService;

    @Mock
    private SpeakingHistoryService historyService;

    @InjectMocks
    private SpeakingRoomController speakingRoomController;

    private static final Long USER_ID = 1L;
    private static final String ROOM_CODE = "ABC234";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(speakingRoomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(CurrentUserId.class);
                            }

                            @Override
                            public Object resolveArgument(@NonNull MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          @NonNull NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return USER_ID;
                            }
                        }
                )
                .build();
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private SpeakingPeerResponse peer(Long userId, ParticipantRole role) {
        return new SpeakingPeerResponse(userId, "User " + userId, null, role);
    }

    private SpeakingRoomResponse roomItem() {
        return new SpeakingRoomResponse(ROOM_CODE, "Travel", (short) 4, SpeakingRoomStatus.ACTIVE, false, 2L,
                peer(USER_ID, ParticipantRole.HOST), LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    private SpeakingHistoryItemResponse historyItem() {
        return new SpeakingHistoryItemResponse(ROOM_CODE, "Travel", (short) 4, "HOST", 2,
                LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 10, 5), 300L,
                SpeakingRoomStatus.ACTIVE, true);
    }

    private SpeakingRoomDetailResponse detail() {
        return new SpeakingRoomDetailResponse(ROOM_CODE, "Travel", (short) 4, SpeakingRoomStatus.ACTIVE, false, USER_ID,
                LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                List.of(peer(USER_ID, ParticipantRole.HOST), peer(2L, ParticipantRole.GUEST)));
    }

    private <T> PageResponse<T> page(List<T> content, Pageable pageable, long total) {
        return PageResponse.of(new PageImpl<>(content, pageable, total));
    }

    // ========================================================================
    // GET /speaking-rooms
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms")
    class ListTests {

        @Test
        @DisplayName("List open rooms returns the page with default paging")
        void list_Success() throws Exception {
            when(speakingRoomService.getOpenRooms(any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(roomItem()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/speaking-rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].roomCode").value(ROOM_CODE))
                    .andExpect(jsonPath("$.data.content[0].topic").value("Travel"))
                    .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.content[0].participantCount").value(2))
                    .andExpect(jsonPath("$.data.content[0].host.userId").value(USER_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            ArgumentCaptor<SpeakingRoomSearchRequest> requestCaptor = ArgumentCaptor.forClass(SpeakingRoomSearchRequest.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(speakingRoomService).getOpenRooms(requestCaptor.capture(), pageableCaptor.capture());
            assertThat(requestCaptor.getValue().keyword()).isNull();
            assertThat(requestCaptor.getValue().maxParticipants()).isNull();
            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("List open rooms passes the keyword, capacity and paging to the service")
        void list_Success_WithFilters() throws Exception {
            when(speakingRoomService.getOpenRooms(any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(roomItem()), PageRequest.of(1, 5), 6));

            mockMvc.perform(get("/speaking-rooms")
                            .param("keyword", "travel")
                            .param("maxParticipants", "3")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(6));

            ArgumentCaptor<SpeakingRoomSearchRequest> requestCaptor = ArgumentCaptor.forClass(SpeakingRoomSearchRequest.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(speakingRoomService).getOpenRooms(requestCaptor.capture(), pageableCaptor.capture());
            assertThat(requestCaptor.getValue().keyword()).isEqualTo("travel");
            assertThat(requestCaptor.getValue().maxParticipants()).isEqualTo(3);
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("List open rooms returns an empty page when there are no rooms")
        void list_Success_Empty() throws Exception {
            when(speakingRoomService.getOpenRooms(any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/speaking-rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @ParameterizedTest(name = "[{index}] maxParticipants={0}")
        @ValueSource(strings = {"1", "5", "0", "-2"})
        @DisplayName("List open rooms failed: Capacity filter is out of range")
        void list_Fail_InvalidMaxParticipants(String value) throws Exception {
            mockMvc.perform(get("/speaking-rooms").param("maxParticipants", value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verifyNoInteractions(speakingRoomService);
        }

        @Test
        @DisplayName("List open rooms failed: Keyword is longer than 100 characters")
        void list_Fail_KeywordTooLong() throws Exception {
            mockMvc.perform(get("/speaking-rooms").param("keyword", "a".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verifyNoInteractions(speakingRoomService);
        }

        @Test
        @DisplayName("List open rooms accepts a keyword of exactly 100 characters")
        void list_Success_KeywordAtLimit() throws Exception {
            when(speakingRoomService.getOpenRooms(any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/speaking-rooms").param("keyword", "a".repeat(100)))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // GET /speaking-rooms/history
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms/history")
    class HistoryTests {

        @Test
        @DisplayName("History returns the sessions of the current user")
        void history_Success() throws Exception {
            when(historyService.getHistory(any(Long.class), any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(historyItem()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/speaking-rooms/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].roomCode").value(ROOM_CODE))
                    .andExpect(jsonPath("$.data.content[0].role").value("HOST"))
                    .andExpect(jsonPath("$.data.content[0].visitCount").value(2))
                    .andExpect(jsonPath("$.data.content[0].durationSeconds").value(300))
                    .andExpect(jsonPath("$.data.content[0].canRejoin").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(historyService).getHistory(any(Long.class), any(SpeakingRoomSearchRequest.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
            verify(speakingRoomService, never()).getRoom(anyString());
        }

        @Test
        @DisplayName("History passes the user id, filters and paging to the service")
        void history_Success_WithFilters() throws Exception {
            when(historyService.getHistory(any(Long.class), any(SpeakingRoomSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page(List.of(), PageRequest.of(2, 20), 0));

            mockMvc.perform(get("/speaking-rooms/history")
                            .param("keyword", "ielts")
                            .param("maxParticipants", "2")
                            .param("page", "2")
                            .param("size", "20"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<SpeakingRoomSearchRequest> requestCaptor = ArgumentCaptor.forClass(SpeakingRoomSearchRequest.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(historyService).getHistory(userCaptor.capture(), requestCaptor.capture(), pageableCaptor.capture());
            assertThat(userCaptor.getValue()).isEqualTo(USER_ID);
            assertThat(requestCaptor.getValue().keyword()).isEqualTo("ielts");
            assertThat(requestCaptor.getValue().maxParticipants()).isEqualTo(2);
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("History failed: Capacity filter is out of range")
        void history_Fail_InvalidMaxParticipants() throws Exception {
            mockMvc.perform(get("/speaking-rooms/history").param("maxParticipants", "9"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verifyNoInteractions(historyService);
        }

        @Test
        @DisplayName("History failed: Keyword is longer than 100 characters")
        void history_Fail_KeywordTooLong() throws Exception {
            mockMvc.perform(get("/speaking-rooms/history").param("keyword", "a".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verifyNoInteractions(historyService);
        }
    }

    // ========================================================================
    // GET /speaking-rooms/history/summary
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms/history/summary")
    class HistorySummaryTests {

        @Test
        @DisplayName("History summary returns the totals of the current user")
        void historySummary_Success() throws Exception {
            when(historyService.getSummary(USER_ID)).thenReturn(new SpeakingHistorySummaryResponse(3600L, 4L, 6L));

            mockMvc.perform(get("/speaking-rooms/history/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalSeconds").value(3600))
                    .andExpect(jsonPath("$.data.roomCount").value(4))
                    .andExpect(jsonPath("$.data.sessionCount").value(6));
        }

        @Test
        @DisplayName("History summary returns zeros for a user without sessions")
        void historySummary_Success_Zeros() throws Exception {
            when(historyService.getSummary(USER_ID)).thenReturn(new SpeakingHistorySummaryResponse(0L, 0L, 0L));

            mockMvc.perform(get("/speaking-rooms/history/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalSeconds").value(0))
                    .andExpect(jsonPath("$.data.roomCount").value(0))
                    .andExpect(jsonPath("$.data.sessionCount").value(0));
        }
    }

    // ========================================================================
    // GET /speaking-rooms/ice-config
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms/ice-config")
    class IceConfigTests {

        @Test
        @DisplayName("ICE config returns the WebRTC servers for the current user")
        void iceConfig_Success() throws Exception {
            when(iceService.getIceConfig(USER_ID)).thenReturn(new IceConfigResponse(
                    List.of(new IceServerResponse(List.of("stun:stun.example.org:3478"), null, null)), 0L));

            mockMvc.perform(get("/speaking-rooms/ice-config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.iceServers.length()").value(1))
                    .andExpect(jsonPath("$.data.iceServers[0].urls[0]").value("stun:stun.example.org:3478"))
                    .andExpect(jsonPath("$.data.ttlSeconds").value(0));

            verify(speakingRoomService, never()).getRoom(anyString());
        }
    }

    // ========================================================================
    // GET /speaking-rooms/{roomCode}/bans
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms/{roomCode}/bans")
    class BansTests {

        @Test
        @DisplayName("Bans returns the kicked members of the room")
        void bans_Success() throws Exception {
            when(speakingRoomService.listBans(USER_ID, ROOM_CODE)).thenReturn(List.of(
                    new SpeakingBannedUserResponse(7L, "Eve", null, LocalDateTime.of(2026, 1, 1, 10, 30))));

            mockMvc.perform(get("/speaking-rooms/{roomCode}/bans", ROOM_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].userId").value(7))
                    .andExpect(jsonPath("$.data[0].displayName").value("Eve"));
        }

        @Test
        @DisplayName("Bans returns an empty list when nobody was kicked")
        void bans_Success_Empty() throws Exception {
            when(speakingRoomService.listBans(USER_ID, ROOM_CODE)).thenReturn(List.of());

            mockMvc.perform(get("/speaking-rooms/{roomCode}/bans", ROOM_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Bans failed: Caller is not the host")
        void bans_Fail_NotHost() throws Exception {
            when(speakingRoomService.listBans(USER_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_NOT_HOST));

            mockMvc.perform(get("/speaking-rooms/{roomCode}/bans", ROOM_CODE))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("SPEAKING_NOT_HOST"));
        }

        @Test
        @DisplayName("Bans failed: Room not found")
        void bans_Fail_RoomNotFound() throws Exception {
            when(speakingRoomService.listBans(USER_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));

            mockMvc.perform(get("/speaking-rooms/{roomCode}/bans", ROOM_CODE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("SPEAKING_ROOM_NOT_FOUND"));
        }

        @Test
        @DisplayName("Bans failed: Room is closed")
        void bans_Fail_RoomClosed() throws Exception {
            when(speakingRoomService.listBans(USER_ID, ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_CLOSED));

            mockMvc.perform(get("/speaking-rooms/{roomCode}/bans", ROOM_CODE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("SPEAKING_ROOM_CLOSED"));
        }
    }

    // ========================================================================
    // GET /speaking-rooms/{roomCode}
    // ========================================================================
    @Nested
    @DisplayName("GET /speaking-rooms/{roomCode}")
    class DetailTests {

        @Test
        @DisplayName("Detail returns the room with its participants")
        void detail_Success() throws Exception {
            when(speakingRoomService.getRoom(ROOM_CODE)).thenReturn(detail());

            mockMvc.perform(get("/speaking-rooms/{roomCode}", ROOM_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.roomCode").value(ROOM_CODE))
                    .andExpect(jsonPath("$.data.topic").value("Travel"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.hostId").value(USER_ID))
                    .andExpect(jsonPath("$.data.participants.length()").value(2))
                    .andExpect(jsonPath("$.data.participants[0].role").value("HOST"))
                    .andExpect(jsonPath("$.data.participants[1].role").value("GUEST"));
        }

        @Test
        @DisplayName("Detail passes the room code to the service unchanged")
        void detail_Success_RoomCodeNotModified() throws Exception {
            when(speakingRoomService.getRoom("abc234")).thenReturn(detail());

            mockMvc.perform(get("/speaking-rooms/{roomCode}", "abc234"))
                    .andExpect(status().isOk());

            verify(speakingRoomService).getRoom("abc234");
        }

        @Test
        @DisplayName("Detail failed: Room not found")
        void detail_Fail_RoomNotFound() throws Exception {
            when(speakingRoomService.getRoom(ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));

            mockMvc.perform(get("/speaking-rooms/{roomCode}", ROOM_CODE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("SPEAKING_ROOM_NOT_FOUND"));
        }

        @Test
        @DisplayName("Detail failed: Room is closed")
        void detail_Fail_RoomClosed() throws Exception {
            when(speakingRoomService.getRoom(ROOM_CODE)).thenThrow(new AppException(AppError.SPEAKING_ROOM_CLOSED));

            mockMvc.perform(get("/speaking-rooms/{roomCode}", ROOM_CODE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("SPEAKING_ROOM_CLOSED"));
        }

        @Test
        @DisplayName("Detail failed: Unexpected error becomes a server error")
        void detail_Fail_UnexpectedError() throws Exception {
            when(speakingRoomService.getRoom(ROOM_CODE)).thenThrow(new IllegalStateException("boom"));

            mockMvc.perform(get("/speaking-rooms/{roomCode}", ROOM_CODE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("UNCATEGORIZED_EXCEPTION"));
        }
    }
}
