package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.response.CategoryShare;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.service.CommunityStatsService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CommunityStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityStatsService communityStatsService;

    @InjectMocks
    private CommunityStatsController communityStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(communityStatsController)
                .setControllerAdvice(new GlobalExceptionHandler(), new HttpExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getSocialStats")
    class GetSocialStatsTests {

        @Test
        @DisplayName("Should return 200 and social statistics when request is valid")
        void getSocialStats_Success() throws Exception {
            CommunityStatsResponses.SocialStatsResponse response = new CommunityStatsResponses.SocialStatsResponse(
                    200L,
                    800L,
                    300L,
                    List.of(new CategoryShare<>("PUBLISHED", 180L, 90.0)),
                    List.of(new CommunityStatsResponses.GroupPostShare(1L, "Grammar", 100L, 50.0)),
                    List.of(new CommunityStatsResponses.SocialEngagementPoint(
                            LocalDate.of(2026, 11, 1), 50L, 20L, 70L
                    )),
                    LocalDateTime.now()
            );

            when(communityStatsService.getSocialStats(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/community/social")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03")
                            .param("granularity", "DAY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalPosts", is(200)))
                    .andExpect(jsonPath("$.data.totalLikes", is(800)))
                    .andExpect(jsonPath("$.data.engagementSeries", hasSize(1)));

            verify(communityStatsService).getSocialStats(any());
        }

        @Test
        @DisplayName("Should return 400 when from date is after to date")
        void getSocialStats_InvalidDateRange_Returns400() throws Exception {
            mockMvc.perform(get("/admin/stats/community/social")
                            .param("from", "2026-11-15")
                            .param("to", "2026-11-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    @DisplayName("getModerationStats")
    class GetModerationStatsTests {

        @Test
        @DisplayName("Should return 200 and moderation analytics")
        void getModerationStats_Success() throws Exception {
            CommunityStatsResponses.ModerationStatsResponse response = new CommunityStatsResponses.ModerationStatsResponse(
                    new CommunityStatsResponses.ModerationActionBreakdown(80L, 10L, 5L, 3L, 2L, 100L, 90.0),
                    List.of(new CategoryShare<>("CLEAN", 80L, 80.0)),
                    120.0,
                    new CommunityStatsResponses.ModerationBacklog(3L, 5L, 8L),
                    LocalDateTime.now()
            );

            when(communityStatsService.getModerationStats(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/community/moderation")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.actions.totalActions", is(100)))
                    .andExpect(jsonPath("$.data.actions.automationRate", is(90.0)))
                    .andExpect(jsonPath("$.data.backlog.totalBacklog", is(8)));

            verify(communityStatsService).getModerationStats(any());
        }
    }

    @Nested
    @DisplayName("getReportStats")
    class GetReportStatsTests {

        @Test
        @DisplayName("Should return 200 and violation reports analytics")
        void getReportStats_Success() throws Exception {
            CommunityStatsResponses.ReportStatsResponse response = new CommunityStatsResponses.ReportStatsResponse(
                    50L,
                    5L,
                    90.0,
                    List.of(new CategoryShare<>("RESOLVED", 40L, 80.0)),
                    List.of(new CategoryShare<>("POST", 30L, 60.0)),
                    LocalDateTime.now()
            );

            when(communityStatsService.getReportStats(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/community/reports")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalReports", is(50)))
                    .andExpect(jsonPath("$.data.resolutionRate", is(90.0)));

            verify(communityStatsService).getReportStats(any());
        }
    }
}
