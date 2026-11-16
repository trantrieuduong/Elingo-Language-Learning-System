package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.LeaderboardService;
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
class LeaderboardControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    private MockMvc mockMvc;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardController leaderboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(leaderboardController)
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
    @DisplayName("getRealtimeLeaderboard")
    class GetRealtimeLeaderboardTests {

        @Test
        @DisplayName("Get realtime leaderboard successfully with default parameters")
        void getRealtimeLeaderboard_Success_DefaultParams() throws Exception {
            LeaderboardItemResponse item = new LeaderboardItemResponse(1, 10L, "alex_master", null, 1250);
            MyLeaderboardRankResponse myRank = new MyLeaderboardRankResponse(5, 850, 50, LeaderboardPeriodType.WEEKLY, "2026-W46");
            PageResponse<LeaderboardItemResponse> page = new PageResponse<>(List.of(item), 1, 20, 128L, 7, false);
            LeaderboardResponse mockResponse = new LeaderboardResponse(
                    LeaderboardPeriodType.WEEKLY,
                    "2026-W46",
                    page,
                    myRank
            );

            when(leaderboardService.getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.WEEKLY), any()))
                    .thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/leaderboards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.periodType").value("WEEKLY"))
                    .andExpect(jsonPath("$.data.periodKey").value("2026-W46"))
                    .andExpect(jsonPath("$.data.leaderboard.totalElements").value(128))
                    .andExpect(jsonPath("$.data.leaderboard.content.length()").value(1))
                    .andExpect(jsonPath("$.data.leaderboard.content[0].username").value("alex_master"))
                    .andExpect(jsonPath("$.data.myRank.rank").value(5));

            verify(leaderboardService).getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.WEEKLY), any());
        }

        @Test
        @DisplayName("Get realtime leaderboard successfully with custom periodType, page, and size")
        void getRealtimeLeaderboard_Success_CustomParams() throws Exception {
            PageResponse<LeaderboardItemResponse> page = new PageResponse<>(List.of(), 2, 10, 50L, 5, false);
            LeaderboardResponse mockResponse = new LeaderboardResponse(
                    LeaderboardPeriodType.MONTHLY,
                    "2026-11",
                    page,
                    null
            );

            when(leaderboardService.getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.MONTHLY), any()))
                    .thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/leaderboards")
                            .param("periodType", "MONTHLY")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.periodType").value("MONTHLY"))
                    .andExpect(jsonPath("$.data.periodKey").value("2026-11"));

            verify(leaderboardService).getRealtimeLeaderboard(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.MONTHLY), any());
        }
    }

    @Nested
    @DisplayName("getMyRank")
    class GetMyRankTests {

        @Test
        @DisplayName("Get current user rank successfully")
        void getMyRank_Success() throws Exception {
            MyLeaderboardRankResponse mockRank = new MyLeaderboardRankResponse(
                    3,
                    900,
                    120,
                    LeaderboardPeriodType.WEEKLY,
                    "2026-W46"
            );

            when(leaderboardService.getMyRank(eq(CURRENT_USER_ID), eq(LeaderboardPeriodType.WEEKLY)))
                    .thenReturn(mockRank);

            mockMvc.perform(get("/gamification/leaderboards/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.rank").value(3))
                    .andExpect(jsonPath("$.data.totalXp").value(900))
                    .andExpect(jsonPath("$.data.diffToNextRank").value(120))
                    .andExpect(jsonPath("$.data.periodType").value("WEEKLY"));

            verify(leaderboardService).getMyRank(CURRENT_USER_ID, LeaderboardPeriodType.WEEKLY);
        }
    }

    @Nested
    @DisplayName("getHistoricalLeaderboard")
    class GetHistoricalLeaderboardTests {

        @Test
        @DisplayName("Get historical leaderboard snapshot successfully")
        void getHistoricalLeaderboard_Success() throws Exception {
            LeaderboardItemResponse item = new LeaderboardItemResponse(1, 10L, "alex_master", null, 2500);
            PageResponse<LeaderboardItemResponse> pageResp = new PageResponse<>(List.of(item), 1, 20, 1L, 1, true);

            when(leaderboardService.getHistoricalLeaderboard(eq(LeaderboardPeriodType.WEEKLY), eq("2026-W45"), any()))
                    .thenReturn(pageResp);

            mockMvc.perform(get("/gamification/leaderboards/history")
                            .param("periodType", "WEEKLY")
                            .param("periodKey", "2026-W45")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].username").value("alex_master"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(leaderboardService).getHistoricalLeaderboard(eq(LeaderboardPeriodType.WEEKLY), eq("2026-W45"), any());
        }
    }
}
