package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.LeaderboardService;
import com.elingo.gamification.service.LevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("GamificationController Tests")
class GamificationControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    private MockMvc mockMvc;

    @Mock
    private LevelService levelService;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private GamificationController gamificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gamificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(CurrentUserId.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                    ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest,
                                    WebDataBinderFactory binderFactory) {
                                return CURRENT_USER_ID;
                            }
                        }
                )
                .build();
    }

    @Nested
    @DisplayName("getAllLevels")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should return all configured levels roadmap successfully")
        void getAllLevels_Success() throws Exception {
            List<LevelResponse> mockLevels = List.of(
                    new LevelResponse((short) 1, 0, "Novice"),
                    new LevelResponse((short) 2, 100, "Explorer")
            );

            when(levelService.getAllLevels()).thenReturn(mockLevels);

            mockMvc.perform(get("/gamification/levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].levelNo").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("Novice"))
                    .andExpect(jsonPath("$.data[1].levelNo").value(2))
                    .andExpect(jsonPath("$.data[1].requiredXp").value(100));

            verify(levelService).getAllLevels();
        }
    }

    @Nested
    @DisplayName("getRealtimeLeaderboard")
    class GetRealtimeLeaderboardTests {

        @Test
        @DisplayName("Should return realtime leaderboard with default parameters")
        void getRealtimeLeaderboard_Success_DefaultParams() throws Exception {
            LeaderboardItemResponse item = new LeaderboardItemResponse(1, 10L, "alex_master", null, 1250);
            PageResponse<LeaderboardItemResponse> pageResponse = new PageResponse<>(
                    List.of(item),
                    1,
                    20,
                    1L,
                    1,
                    true
            );

            MyLeaderboardRankResponse rankResponse = new MyLeaderboardRankResponse(
                    1, 1250, 0, LeaderboardPeriodType.WEEKLY, "2026-W46"
            );

            LeaderboardResponse response = new LeaderboardResponse(
                    LeaderboardPeriodType.WEEKLY, "2026-W46", pageResponse, rankResponse
            );

            when(leaderboardService.getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.WEEKLY), any(Pageable.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/gamification/leaderboards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.periodType").value("WEEKLY"))
                    .andExpect(jsonPath("$.data.periodKey").value("2026-W46"))
                    .andExpect(jsonPath("$.data.leaderboard.content[0].userId").value(10))
                    .andExpect(jsonPath("$.data.myRank.rank").value(1));

            verify(leaderboardService).getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.WEEKLY), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return monthly realtime leaderboard when requested")
        void getRealtimeLeaderboard_Success_Monthly() throws Exception {
            PageResponse<LeaderboardItemResponse> emptyPage = new PageResponse<>(
                    List.of(),
                    1,
                    20,
                    0L,
                    0,
                    true
            );

            LeaderboardResponse response = new LeaderboardResponse(
                    LeaderboardPeriodType.MONTHLY,
                    "2026-11",
                    emptyPage,
                    null
            );

            when(leaderboardService.getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.MONTHLY), any(Pageable.class)))
                    .thenReturn(response);

            mockMvc.perform(get("/gamification/leaderboards")
                            .param("periodType", "MONTHLY")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.periodType").value("MONTHLY"))
                    .andExpect(jsonPath("$.data.periodKey").value("2026-11"));

            verify(leaderboardService).getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.MONTHLY), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getHistoricalLeaderboard")
    class GetHistoricalLeaderboardTests {

        @Test
        @DisplayName("Should return historical leaderboard snapshot page successfully")
        void getHistoricalLeaderboard_Success() throws Exception {
            LeaderboardItemResponse item = new LeaderboardItemResponse(1, 10L, "alex_master", null, 2500);
            PageResponse<LeaderboardItemResponse> pageResponse = new PageResponse<>(
                    List.of(item),
                    1,
                    20,
                    1L,
                    1,
                    true
            );

            when(leaderboardService.getHistoricalLeaderboard(eq(LeaderboardPeriodType.WEEKLY), eq("2026-W45"), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/gamification/leaderboards/history")
                            .param("periodType", "WEEKLY")
                            .param("periodKey", "2026-W45"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].userId").value(10))
                    .andExpect(jsonPath("$.data.content[0].totalXp").value(2500));

            verify(leaderboardService).getHistoricalLeaderboard(eq(LeaderboardPeriodType.WEEKLY), eq("2026-W45"), any(Pageable.class));
        }
    }
}
