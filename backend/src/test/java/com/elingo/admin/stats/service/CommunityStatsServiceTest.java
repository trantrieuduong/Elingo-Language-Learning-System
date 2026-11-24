package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.service.impl.CommunityStatsServiceImpl;
import com.elingo.common.enums.StatsGranularity;
import com.elingo.common.service.RedisService;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.dto.projection.DateCountProjection;
import com.elingo.community.dto.projection.PostBacklogProjection;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.projection.PostStatusCountProjection;
import com.elingo.community.moderation.dto.projection.AiLabelCount;
import com.elingo.community.moderation.dto.projection.ModerationActionCounts;
import com.elingo.community.service.CommunityStatsQueryService;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.dto.projection.StatusCountProjection;
import com.elingo.report.dto.projection.TargetTypeCountProjection;
import com.elingo.report.service.ReportStatsQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CommunityStatsServiceTest {

    @Mock
    private CommunityStatsQueryService communityStatsQueryService;

    @Mock
    private ReportStatsQueryService reportStatsQueryService;

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CommunityStatsServiceImpl communityStatsService;

    private final LocalDate fromDate = LocalDate.of(2026, 11, 1);
    private final LocalDate toDate = LocalDate.of(2026, 11, 3);
    private final StatsDateRangeRequest defaultRequest = StatsDateRangeRequest.of(fromDate, toDate, StatsGranularity.DAY);

    @Nested
    @DisplayName("getSocialStats")
    class GetSocialStatsTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getSocialStats_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:community:social:from=2026-11-01&to=2026-11-03&granularity=DAY";
            String jsonContent = "{\"totalPosts\":100}";
            CommunityStatsResponses.SocialStatsResponse expected = new CommunityStatsResponses.SocialStatsResponse(
                    100L, 500L, 200L, List.of(), List.of(), List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, CommunityStatsResponses.SocialStatsResponse.class)).thenReturn(expected);

            CommunityStatsResponses.SocialStatsResponse actual = communityStatsService.getSocialStats(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(communityStatsQueryService);
        }

        @Test
        @DisplayName("Should fetch distributions and engagement series, then cache on cache miss")
        void getSocialStats_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:community:social:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn(null);

            PostStatusCountProjection statusProj = mock(PostStatusCountProjection.class);
            when(statusProj.getStatus()).thenReturn(PostStatus.PUBLISHED);
            when(statusProj.getCountVal()).thenReturn(100L);
            when(communityStatsQueryService.countPostsByStatus()).thenReturn(List.of(statusProj));

            PostGroupCountProjection groupProj = mock(PostGroupCountProjection.class);
            when(groupProj.getGroupId()).thenReturn(1L);
            when(groupProj.getGroupName()).thenReturn("Grammar");
            when(groupProj.getPostCount()).thenReturn(100L);
            when(communityStatsQueryService.countPostsByGroup()).thenReturn(List.of(groupProj));

            DateCountProjection likeProj = mock(DateCountProjection.class);
            when(likeProj.getDateKey()).thenReturn("2026-11-01");
            when(likeProj.getCountVal()).thenReturn(15L);
            when(communityStatsQueryService.countLikesByDay(any(), any())).thenReturn(List.of(likeProj));

            DateCountProjection commentProj = mock(DateCountProjection.class);
            when(commentProj.getDateKey()).thenReturn("2026-11-01");
            when(commentProj.getCountVal()).thenReturn(5L);
            when(communityStatsQueryService.countCommentsByDay(any(), any())).thenReturn(List.of(commentProj));

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            CommunityStatsResponses.SocialStatsResponse actual = communityStatsService.getSocialStats(defaultRequest);

            assertThat(actual.totalPosts()).isEqualTo(100L);
            assertThat(actual.totalLikes()).isEqualTo(15L);
            assertThat(actual.totalComments()).isEqualTo(5L);
            assertThat(actual.byStatus()).hasSize(1);
            assertThat(actual.byStatus().get(0).percentage()).isEqualTo(100.0);
            assertThat(actual.byGroup()).hasSize(1);
            assertThat(actual.engagementSeries()).hasSize(3);

            // Day 1 has 15 likes + 5 comments = 20 interactions
            assertThat(actual.engagementSeries().get(0).totalInteractions()).isEqualTo(20L);

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(3L));
        }

        @Test
        @DisplayName("Should fall back to database query when Redis cache read throws JacksonException")
        void getSocialStats_CacheReadError_FallsBackToDb() throws Exception {
            String cacheKey = "admin:stats:community:social:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn("corrupted-json");
            when(objectMapper.readValue(eq("corrupted-json"), eq(CommunityStatsResponses.SocialStatsResponse.class)))
                    .thenThrow(mock(JacksonException.class));

            when(communityStatsQueryService.countPostsByStatus()).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countPostsByGroup()).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countLikesByDay(any(), any())).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countCommentsByDay(any(), any())).thenReturn(Collections.emptyList());

            CommunityStatsResponses.SocialStatsResponse actual = communityStatsService.getSocialStats(defaultRequest);

            assertThat(actual.totalPosts()).isEqualTo(0L);
            verify(communityStatsQueryService).countPostsByStatus();
        }

        @Test
        @DisplayName("Should handle zero total posts without division by zero")
        void getSocialStats_ZeroTotalPosts_HandlesZeroPercentage() {
            when(redisService.get(anyString())).thenReturn(null);
            when(communityStatsQueryService.countPostsByStatus()).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countPostsByGroup()).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countLikesByDay(any(), any())).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.countCommentsByDay(any(), any())).thenReturn(Collections.emptyList());

            CommunityStatsResponses.SocialStatsResponse actual = communityStatsService.getSocialStats(null);

            assertThat(actual.totalPosts()).isEqualTo(0L);
            assertThat(actual.byStatus()).isEmpty();
            assertThat(actual.byGroup()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getModerationStats")
    class GetModerationStatsTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getModerationStats_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:community:moderation:from=2026-11-01&to=2026-11-03";
            String jsonContent = "{\"actions\":{\"totalActions\":50}}";
            CommunityStatsResponses.ModerationStatsResponse expected = new CommunityStatsResponses.ModerationStatsResponse(
                    new CommunityStatsResponses.ModerationActionBreakdown(30L, 10L, 5L, 3L, 2L, 50L, 80.0),
                    List.of(),
                    120.0,
                    new CommunityStatsResponses.ModerationBacklog(2L, 3L, 5L),
                    LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, CommunityStatsResponses.ModerationStatsResponse.class)).thenReturn(expected);

            CommunityStatsResponses.ModerationStatsResponse actual = communityStatsService.getModerationStats(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(communityStatsQueryService);
        }

        @Test
        @DisplayName("Should compute action breakdown, AI labels, and backlog on cache miss")
        void getModerationStats_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:community:moderation:from=2026-11-01&to=2026-11-03";
            when(redisService.get(cacheKey)).thenReturn(null);

            ModerationActionCounts actionCounts =
                    new ModerationActionCounts(80L, 10L, 10L, 5L, 5L, 110L);
            when(communityStatsQueryService.countModerationActions(any(), any())).thenReturn(actionCounts);

            AiLabelCount labelProj = new AiLabelCount("SPAM", 10L);
            when(communityStatsQueryService.countAiLabels(any(), any())).thenReturn(List.of(labelProj));

            when(communityStatsQueryService.calculateAvgManualReviewDurationSeconds(any(), any())).thenReturn(150.0);

            PostBacklogProjection backlogProj = mock(PostBacklogProjection.class);
            when(backlogProj.getPendingCount()).thenReturn(4L);
            when(backlogProj.getNeedsReviewCount()).thenReturn(6L);
            when(communityStatsQueryService.countPostBacklog()).thenReturn(backlogProj);

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            CommunityStatsResponses.ModerationStatsResponse actual = communityStatsService.getModerationStats(defaultRequest);

            assertThat(actual.actions().totalActions()).isEqualTo(110L);
            assertThat(actual.actions().autoApproveCount()).isEqualTo(80L);
            // automationRate = (80 + 10) / 110 * 100 = 81.82%
            assertThat(actual.actions().automationRate()).isEqualTo(81.82);

            assertThat(actual.aiLabelDistribution()).hasSize(1);
            assertThat(actual.aiLabelDistribution().get(0).category()).isEqualTo("SPAM");
            assertThat(actual.aiLabelDistribution().get(0).percentage()).isEqualTo(100.0);

            assertThat(actual.avgManualReviewSeconds()).isEqualTo(150.0);
            assertThat(actual.backlog().pendingCount()).isEqualTo(4L);
            assertThat(actual.backlog().needsReviewCount()).isEqualTo(6L);
            assertThat(actual.backlog().totalBacklog()).isEqualTo(10L);

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(3L));
        }

        @Test
        @DisplayName("Should handle zero total actions and null avg duration gracefully")
        void getModerationStats_ZeroActionsAndNullAvg_HandlesDefaults() {
            when(redisService.get(anyString())).thenReturn(null);

            ModerationActionCounts zeroCounts =
                    new ModerationActionCounts(0L, 0L, 0L, 0L, 0L, 0L);
            when(communityStatsQueryService.countModerationActions(any(), any())).thenReturn(zeroCounts);
            when(communityStatsQueryService.countAiLabels(any(), any())).thenReturn(Collections.emptyList());
            when(communityStatsQueryService.calculateAvgManualReviewDurationSeconds(any(), any())).thenReturn(null);
            when(communityStatsQueryService.countPostBacklog()).thenReturn(null);

            CommunityStatsResponses.ModerationStatsResponse actual = communityStatsService.getModerationStats(defaultRequest);

            assertThat(actual.actions().totalActions()).isEqualTo(0L);
            assertThat(actual.actions().automationRate()).isEqualTo(0.0);
            assertThat(actual.avgManualReviewSeconds()).isEqualTo(0.0);
            assertThat(actual.backlog().totalBacklog()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getReportStats")
    class GetReportStatsTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getReportStats_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:community:reports:from=2026-11-01&to=2026-11-03";
            String jsonContent = "{\"totalReports\":40}";
            CommunityStatsResponses.ReportStatsResponse expected = new CommunityStatsResponses.ReportStatsResponse(
                    40L, 5L, 87.5, List.of(), List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, CommunityStatsResponses.ReportStatsResponse.class)).thenReturn(expected);

            CommunityStatsResponses.ReportStatsResponse actual = communityStatsService.getReportStats(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(reportStatsQueryService);
        }

        @Test
        @DisplayName("Should aggregate reports by status and target type on cache miss")
        void getReportStats_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:community:reports:from=2026-11-01&to=2026-11-03";
            when(redisService.get(cacheKey)).thenReturn(null);

            StatusCountProjection resolvedProj = mock(StatusCountProjection.class);
            when(resolvedProj.getStatus()).thenReturn(ReportStatus.RESOLVED);
            when(resolvedProj.getCountVal()).thenReturn(8L);

            StatusCountProjection pendingProj = mock(StatusCountProjection.class);
            when(pendingProj.getStatus()).thenReturn(ReportStatus.PENDING);
            when(pendingProj.getCountVal()).thenReturn(2L);

            when(reportStatsQueryService.countReportsByStatus(any(), any()))
                    .thenReturn(List.of(resolvedProj, pendingProj));

            TargetTypeCountProjection targetProj = mock(TargetTypeCountProjection.class);
            when(targetProj.getTargetType()).thenReturn(ReportTargetType.POST);
            when(targetProj.getCountVal()).thenReturn(10L);
            when(reportStatsQueryService.countReportsByTargetType(any(), any()))
                    .thenReturn(List.of(targetProj));

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            CommunityStatsResponses.ReportStatsResponse actual = communityStatsService.getReportStats(defaultRequest);

            assertThat(actual.totalReports()).isEqualTo(10L);
            assertThat(actual.pendingReports()).isEqualTo(2L);
            // 8 resolved / 10 total = 80.0%
            assertThat(actual.resolutionRate()).isEqualTo(80.0);
            assertThat(actual.byStatus()).hasSize(2);
            assertThat(actual.byTargetType()).hasSize(1);
            assertThat(actual.byTargetType().get(0).category()).isEqualTo("POST");
            assertThat(actual.byTargetType().get(0).percentage()).isEqualTo(100.0);

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(5L));
        }

        @Test
        @DisplayName("Should handle zero total reports without division by zero")
        void getReportStats_ZeroTotalReports_HandlesZeroPercentage() {
            when(redisService.get(anyString())).thenReturn(null);
            when(reportStatsQueryService.countReportsByStatus(any(), any())).thenReturn(Collections.emptyList());
            when(reportStatsQueryService.countReportsByTargetType(any(), any())).thenReturn(Collections.emptyList());

            CommunityStatsResponses.ReportStatsResponse actual = communityStatsService.getReportStats(defaultRequest);

            assertThat(actual.totalReports()).isEqualTo(0L);
            assertThat(actual.resolutionRate()).isEqualTo(0.0);
            assertThat(actual.pendingReports()).isEqualTo(0L);
            assertThat(actual.byStatus()).isEmpty();
            assertThat(actual.byTargetType()).isEmpty();
        }
    }
}
