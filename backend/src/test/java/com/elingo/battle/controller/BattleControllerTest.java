package com.elingo.battle.controller;

import com.elingo.battle.dto.response.BattleHistoryItemResponse;
import com.elingo.battle.dto.response.BattleMatchDetailResponse;
import com.elingo.battle.dto.response.BattleRoundAnswerResponse;
import com.elingo.battle.dto.response.BattleRoundDetailResponse;
import com.elingo.battle.dto.response.BattleStatsResponse;
import com.elingo.battle.dto.response.PlayerFinalResultResponse;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleResult;
import com.elingo.battle.service.BattleHistoryService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import org.jspecify.annotations.NonNull;
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
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BattleHistoryService battleHistoryService;

    @InjectMocks
    private BattleController battleController;

    private static final Long USER_ID = 1L;
    private static final Long OPPONENT_ID = 2L;
    private static final Long MATCH_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(battleController)
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

    private BattleHistoryItemResponse buildHistoryItem(Long matchId, BattleResult result, int myScore, int opponentScore) {
        return new BattleHistoryItemResponse(
                matchId,
                BattleMode.MULTIPLE_CHOICE,
                BattleMatchType.RANDOM,
                new PlayerResponse(OPPONENT_ID, "Opponent", null),
                myScore,
                opponentScore,
                result,
                LocalDateTime.now()
        );
    }

    private BattleMatchDetailResponse buildMatchDetail() {
        List<PlayerResponse> players = List.of(
                new PlayerResponse(USER_ID, "User One", null),
                new PlayerResponse(OPPONENT_ID, "Opponent", null)
        );
        List<PlayerFinalResultResponse> results = List.of(
                new PlayerFinalResultResponse(USER_ID, 300, 2, true),
                new PlayerFinalResultResponse(OPPONENT_ID, 150, 1, false)
        );
        List<BattleRoundDetailResponse> rounds = List.of(
                new BattleRoundDetailResponse(1, "apple", "tao", List.of(
                        new BattleRoundAnswerResponse(USER_ID, true, "tao", true, 2000),
                        new BattleRoundAnswerResponse(OPPONENT_ID, false, null, false, null)
                ))
        );
        return new BattleMatchDetailResponse(
                MATCH_ID,
                BattleMode.MULTIPLE_CHOICE,
                BattleMatchType.RANDOM,
                1,
                USER_ID,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now(),
                players,
                results,
                rounds
        );
    }

    // ========================================================================
    // history
    // ========================================================================
    @Nested
    @DisplayName("history")
    class HistoryTests {

        @Test
        @DisplayName("Get battle history successfully with default paging")
        void history_Success_DefaultPaging() throws Exception {
            BattleHistoryItemResponse item = buildHistoryItem(MATCH_ID, BattleResult.WIN, 300, 150);
            Page<BattleHistoryItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

            when(battleHistoryService.getHistory(eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/battles/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].matchId").value(MATCH_ID))
                    .andExpect(jsonPath("$.data.content[0].mode").value("MULTIPLE_CHOICE"))
                    .andExpect(jsonPath("$.data.content[0].matchType").value("RANDOM"))
                    .andExpect(jsonPath("$.data.content[0].result").value("WIN"))
                    .andExpect(jsonPath("$.data.content[0].myScore").value(300))
                    .andExpect(jsonPath("$.data.content[0].opponentScore").value(150))
                    .andExpect(jsonPath("$.data.content[0].opponent.userId").value(OPPONENT_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(battleHistoryService).getHistory(eq(USER_ID), isNull(), isNull(), isNull(), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isZero();
            assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("Get battle history successfully with result, mode, matchType filters and custom paging")
        void history_Success_WithFilters() throws Exception {
            BattleHistoryItemResponse item = buildHistoryItem(MATCH_ID, BattleResult.LOSE, 100, 250);
            Page<BattleHistoryItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(1, 5), 6);

            when(battleHistoryService.getHistory(eq(USER_ID), eq(BattleResult.LOSE), eq(BattleMode.TYPING),
                    eq(BattleMatchType.ROOM), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/battles/history")
                            .param("result", "LOSE")
                            .param("mode", "TYPING")
                            .param("matchType", "ROOM")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].result").value("LOSE"))
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(6));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(battleHistoryService).getHistory(eq(USER_ID), eq(BattleResult.LOSE), eq(BattleMode.TYPING),
                    eq(BattleMatchType.ROOM), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("Get battle history successfully with multiple items")
        void history_Success_MultipleItems() throws Exception {
            List<BattleHistoryItemResponse> items = List.of(
                    buildHistoryItem(MATCH_ID, BattleResult.WIN, 300, 150),
                    buildHistoryItem(MATCH_ID + 1, BattleResult.DRAW, 200, 200),
                    buildHistoryItem(MATCH_ID + 2, BattleResult.LOSE, 0, 100)
            );
            Page<BattleHistoryItemResponse> page = new PageImpl<>(items, PageRequest.of(0, 10), 3);

            when(battleHistoryService.getHistory(eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/battles/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(3))
                    .andExpect(jsonPath("$.data.content[0].result").value("WIN"))
                    .andExpect(jsonPath("$.data.content[1].result").value("DRAW"))
                    .andExpect(jsonPath("$.data.content[2].result").value("LOSE"));
        }

        @Test
        @DisplayName("Get battle history successfully when user has no finished match")
        void history_Success_Empty() throws Exception {
            Page<BattleHistoryItemResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(battleHistoryService.getHistory(eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/battles/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // ========================================================================
    // stats
    // ========================================================================
    @Nested
    @DisplayName("stats")
    class StatsTests {

        @Test
        @DisplayName("Get battle stats successfully")
        void stats_Success() throws Exception {
            when(battleHistoryService.getStats(USER_ID))
                    .thenReturn(new BattleStatsResponse(10, 6, 3, 1, 60.0));

            mockMvc.perform(get("/battles/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalMatches").value(10))
                    .andExpect(jsonPath("$.data.wins").value(6))
                    .andExpect(jsonPath("$.data.losses").value(3))
                    .andExpect(jsonPath("$.data.draws").value(1))
                    .andExpect(jsonPath("$.data.winRate").value(60.0));

            verify(battleHistoryService).getStats(USER_ID);
        }

        @Test
        @DisplayName("Get battle stats successfully when user has never played")
        void stats_Success_NoMatches() throws Exception {
            when(battleHistoryService.getStats(USER_ID))
                    .thenReturn(new BattleStatsResponse(0, 0, 0, 0, 0.0));

            mockMvc.perform(get("/battles/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalMatches").value(0))
                    .andExpect(jsonPath("$.data.winRate").value(0.0));
        }
    }

    // ========================================================================
    // detail
    // ========================================================================
    @Nested
    @DisplayName("detail")
    class DetailTests {

        @Test
        @DisplayName("Get battle match detail successfully")
        void detail_Success() throws Exception {
            when(battleHistoryService.getMatchDetail(USER_ID, MATCH_ID)).thenReturn(buildMatchDetail());

            mockMvc.perform(get("/battles/{matchId}", MATCH_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matchId").value(MATCH_ID))
                    .andExpect(jsonPath("$.data.mode").value("MULTIPLE_CHOICE"))
                    .andExpect(jsonPath("$.data.matchType").value("RANDOM"))
                    .andExpect(jsonPath("$.data.totalRounds").value(1))
                    .andExpect(jsonPath("$.data.winnerId").value(USER_ID))
                    .andExpect(jsonPath("$.data.players.length()").value(2))
                    .andExpect(jsonPath("$.data.players[0].userId").value(USER_ID))
                    .andExpect(jsonPath("$.data.results[0].score").value(300))
                    .andExpect(jsonPath("$.data.results[0].isWinner").value(true))
                    .andExpect(jsonPath("$.data.results[1].isWinner").value(false))
                    .andExpect(jsonPath("$.data.rounds.length()").value(1))
                    .andExpect(jsonPath("$.data.rounds[0].roundNo").value(1))
                    .andExpect(jsonPath("$.data.rounds[0].term").value("apple"))
                    .andExpect(jsonPath("$.data.rounds[0].correctAnswer").value("tao"))
                    .andExpect(jsonPath("$.data.rounds[0].answers[0].answered").value(true))
                    .andExpect(jsonPath("$.data.rounds[0].answers[0].answerText").value("tao"))
                    .andExpect(jsonPath("$.data.rounds[0].answers[0].isCorrect").value(true))
                    .andExpect(jsonPath("$.data.rounds[0].answers[1].answered").value(false))
                    .andExpect(jsonPath("$.data.rounds[0].answers[1].isCorrect").value(false));

            verify(battleHistoryService).getMatchDetail(USER_ID, MATCH_ID);
        }

        @Test
        @DisplayName("Get battle match detail failed: Match not found")
        void detail_Fail_MatchNotFound() throws Exception {
            when(battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .thenThrow(new AppException(AppError.BATTLE_MATCH_NOT_FOUND));

            mockMvc.perform(get("/battles/{matchId}", MATCH_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("BATTLE_MATCH_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get battle match detail failed: User is not a participant of the match")
        void detail_Fail_ParticipantNotFound() throws Exception {
            when(battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .thenThrow(new AppException(AppError.BATTLE_PARTICIPANT_NOT_FOUND));

            mockMvc.perform(get("/battles/{matchId}", MATCH_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("BATTLE_PARTICIPANT_NOT_FOUND"));
        }
    }
}
