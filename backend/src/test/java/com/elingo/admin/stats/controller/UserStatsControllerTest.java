package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.admin.stats.service.UserStatsService;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.exception.HttpExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private UserStatsController userStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userStatsController)
                .setControllerAdvice(new GlobalExceptionHandler(), new HttpExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getUserGrowth")
    class GetUserGrowthTests {

        @Test
        @DisplayName("Should return 200 and user growth data when request is valid")
        void getUserGrowth_Success() throws Exception {
            UserStatsResponses.UserGrowthResponse response = new UserStatsResponses.UserGrowthResponse(
                    100L,
                    List.of(new UserStatsResponses.StatPoint<>("2026-11-01", 10L)),
                    List.of(new UserStatsResponses.StatPoint<>("2026-11-01", 110L)),
                    LocalDateTime.now()
            );

            when(userStatsService.getUserGrowth(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/users/growth")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03")
                            .param("granularity", "DAY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalBeforeRange", is(100)))
                    .andExpect(jsonPath("$.data.newUsers", hasSize(1)))
                    .andExpect(jsonPath("$.data.cumulativeUsers", hasSize(1)));

            verify(userStatsService).getUserGrowth(any());
        }

        @Test
        @DisplayName("Should return 400 when from date is after to date")
        void getUserGrowth_InvalidDateRange_Returns400() throws Exception {
            mockMvc.perform(get("/admin/stats/users/growth")
                            .param("from", "2026-11-20")
                            .param("to", "2026-11-01")
                            .param("granularity", "DAY"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    @DisplayName("getUserActivity")
    class GetUserActivityTests {

        @Test
        @DisplayName("Should return 200 and user activity data")
        void getUserActivity_Success() throws Exception {
            UserStatsResponses.UserActivityResponse response = new UserStatsResponses.UserActivityResponse(
                    15L, 50L, 100L, 15.0, List.of(), LocalDateTime.now()
            );

            when(userStatsService.getUserActivity(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/users/activity")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03")
                            .param("granularity", "DAY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.currentDau", is(15)))
                    .andExpect(jsonPath("$.data.currentWau", is(50)))
                    .andExpect(jsonPath("$.data.currentMau", is(100)))
                    .andExpect(jsonPath("$.data.stickinessPercentage", is(15.0)));
        }
    }

    @Nested
    @DisplayName("getUserBreakdown")
    class GetUserBreakdownTests {

        @Test
        @DisplayName("Should return 200 and user breakdown data")
        void getUserBreakdown_Success() throws Exception {
            UserStatsResponses.UserBreakdownResponse response = new UserStatsResponses.UserBreakdownResponse(
                    500L, 400L, 80.0, 480L, 96.0, 50L, 10.0, List.of(), LocalDateTime.now()
            );

            when(userStatsService.getUserBreakdown()).thenReturn(response);

            mockMvc.perform(get("/admin/stats/users/breakdown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalUsers", is(500)))
                    .andExpect(jsonPath("$.data.verifiedPercentage", is(80.0)))
                    .andExpect(jsonPath("$.data.activePercentage", is(96.0)))
                    .andExpect(jsonPath("$.data.premiumPercentage", is(10.0)));
        }
    }
}
