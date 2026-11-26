package com.elingo.admin.stats.service.impl;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CategoryShare;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.service.CommunityStatsService;
import com.elingo.admin.stats.util.StatsZeroFiller;
import com.elingo.common.service.RedisService;
import com.elingo.community.dto.projection.DateCountProjection;
import com.elingo.community.dto.projection.PostBacklogProjection;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.projection.PostStatusCountProjection;
import com.elingo.community.moderation.dto.projection.AiLabelCount;
import com.elingo.community.moderation.dto.projection.ModerationActionCounts;
import com.elingo.community.service.CommunityStatsQueryService;
import com.elingo.report.dto.projection.StatusCountProjection;
import com.elingo.report.dto.projection.TargetTypeCountProjection;
import com.elingo.report.service.ReportStatsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "COMMUNITY-STATS-SERVICE")
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S1192") // Giữ nguyên message log Redis chuẩn thống nhất giữa các hàm stats
public class CommunityStatsServiceImpl implements CommunityStatsService {

    private final CommunityStatsQueryService communityStatsQueryService;
    private final ReportStatsQueryService reportStatsQueryService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_SOCIAL_MINUTES = 3L;
    private static final long CACHE_TTL_MODERATION_MINUTES = 3L;
    private static final long CACHE_TTL_REPORTS_MINUTES = 5L;

    @Override
    @SuppressWarnings("java:S3776")
    public CommunityStatsResponses.SocialStatsResponse getSocialStats(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:community:social:from=%s&to=%s&granularity=%s",
                safeRequest.from(), safeRequest.to(), safeRequest.granularity());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CommunityStatsResponses.SocialStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<PostStatusCountProjection> statusRaw = communityStatsQueryService.countPostsByStatus();
        long totalPosts = statusRaw.stream()
                .mapToLong(p -> p.getCountVal() != null ? p.getCountVal() : 0L)
                .sum();

        List<CategoryShare<String>> byStatus = statusRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalPosts > 0 ? roundDouble(((double) count / totalPosts) * 100.0) : 0.0;
            return new CategoryShare<>(p.getStatus().name(), count, pct);
        }).toList();

        List<PostGroupCountProjection> groupRaw = communityStatsQueryService.countPostsByGroup();
        List<CommunityStatsResponses.GroupPostShare> byGroup = groupRaw.stream().map(p -> {
            long count = p.getPostCount() != null ? p.getPostCount() : 0L;
            double pct = totalPosts > 0 ? roundDouble(((double) count / totalPosts) * 100.0) : 0.0;
            return new CommunityStatsResponses.GroupPostShare(
                    p.getGroupId(),
                    p.getGroupName(),
                    count,
                    pct
            );
        }).toList();

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<DateCountProjection> likesRaw = communityStatsQueryService.countLikesByDay(fromTime, toTime);
        Map<String, Long> likesMap = new HashMap<>();
        long totalLikes = 0L;
        for (DateCountProjection p : likesRaw) {
            if (p.getDateKey() != null) {
                long c = p.getCountVal() != null ? p.getCountVal() : 0L;
                likesMap.put(p.getDateKey(), c);
                totalLikes += c;
            }
        }

        List<DateCountProjection> commentsRaw = communityStatsQueryService.countCommentsByDay(fromTime, toTime);
        Map<String, Long> commentsMap = new HashMap<>();
        long totalComments = 0L;
        for (DateCountProjection p : commentsRaw) {
            if (p.getDateKey() != null) {
                long c = p.getCountVal() != null ? p.getCountVal() : 0L;
                commentsMap.put(p.getDateKey(), c);
                totalComments += c;
            }
        }

        List<CommunityStatsResponses.SocialEngagementPoint> engagementSeries =
                StatsZeroFiller.fillSocialEngagementSeries(safeRequest.from(), safeRequest.to(), likesMap, commentsMap);

        CommunityStatsResponses.SocialStatsResponse response = new CommunityStatsResponses.SocialStatsResponse(
                totalPosts,
                totalLikes,
                totalComments,
                byStatus,
                byGroup,
                engagementSeries,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_SOCIAL_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    public CommunityStatsResponses.ModerationStatsResponse getModerationStats(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:community:moderation:from=%s&to=%s",
                safeRequest.from(), safeRequest.to());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CommunityStatsResponses.ModerationStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        ModerationActionCounts actionCounts =
                communityStatsQueryService.countModerationActions(fromTime, toTime);

        long autoHandled = actionCounts.autoApproveCount() + actionCounts.autoRejectCount();
        long totalDecisions = actionCounts.totalActions();
        double automationRate = totalDecisions > 0
                ? roundDouble(((double) autoHandled / totalDecisions) * 100.0)
                : 0.0;

        CommunityStatsResponses.ModerationActionBreakdown actionBreakdown =
                new CommunityStatsResponses.ModerationActionBreakdown(
                        actionCounts.autoApproveCount(),
                        actionCounts.autoRejectCount(),
                        actionCounts.flagForReviewCount(),
                        actionCounts.manualApproveCount(),
                        actionCounts.manualRejectCount(),
                        totalDecisions,
                        automationRate
                );

        List<AiLabelCount> aiLabelRaw = communityStatsQueryService.countAiLabels(fromTime, toTime);
        long totalAiLabels = aiLabelRaw.stream().mapToLong(AiLabelCount::countVal).sum();

        List<CategoryShare<String>> aiLabelDistribution = aiLabelRaw.stream().map(p -> {
            long count = p.countVal();
            double pct = totalAiLabels > 0 ? roundDouble(((double) count / totalAiLabels) * 100.0) : 0.0;
            return new CategoryShare<>(p.label(), count, pct);
        }).toList();

        Double avgManualSec = communityStatsQueryService.calculateAvgManualReviewDurationSeconds(fromTime, toTime);
        double avgManualReviewSeconds = avgManualSec != null ? roundDouble(avgManualSec) : 0.0;

        PostBacklogProjection backlogRaw = communityStatsQueryService.countPostBacklog();
        long pending = backlogRaw != null && backlogRaw.getPendingCount() != null ? backlogRaw.getPendingCount() : 0L;
        long needsReview = backlogRaw != null && backlogRaw.getNeedsReviewCount() != null ? backlogRaw.getNeedsReviewCount() : 0L;
        long totalBacklog = pending + needsReview;

        CommunityStatsResponses.ModerationBacklog backlog =
                new CommunityStatsResponses.ModerationBacklog(pending, needsReview, totalBacklog);

        CommunityStatsResponses.ModerationStatsResponse response = new CommunityStatsResponses.ModerationStatsResponse(
                actionBreakdown,
                aiLabelDistribution,
                avgManualReviewSeconds,
                backlog,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_MODERATION_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public CommunityStatsResponses.ReportStatsResponse getReportStats(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:community:reports:from=%s&to=%s",
                safeRequest.from(), safeRequest.to());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CommunityStatsResponses.ReportStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<StatusCountProjection> statusRaw =
                reportStatsQueryService.countReportsByStatus(fromTime, toTime);
        long totalReports = statusRaw.stream()
                .mapToLong(p -> p.getCountVal() != null ? p.getCountVal() : 0L)
                .sum();

        long pendingReports = 0L;
        long resolvedReports = 0L;
        long rejectedReports = 0L;

        for (StatusCountProjection p : statusRaw) {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            if (p.getStatus() == com.elingo.report.entity.ReportStatus.PENDING) {
                pendingReports += count;
            } else if (p.getStatus() == com.elingo.report.entity.ReportStatus.RESOLVED) {
                resolvedReports += count;
            } else if (p.getStatus() == com.elingo.report.entity.ReportStatus.REJECTED) {
                rejectedReports += count;
            }
        }

        List<CategoryShare<String>> byStatus = statusRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalReports > 0 ? roundDouble(((double) count / totalReports) * 100.0) : 0.0;
            return new CategoryShare<>(p.getStatus().name(), count, pct);
        }).toList();

        List<TargetTypeCountProjection> targetRaw =
                reportStatsQueryService.countReportsByTargetType(fromTime, toTime);
        List<CategoryShare<String>> byTargetType = targetRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalReports > 0 ? roundDouble(((double) count / totalReports) * 100.0) : 0.0;
            return new CategoryShare<>(p.getTargetType().name(), count, pct);
        }).toList();

        long handledReports = resolvedReports + rejectedReports;
        double resolutionRate = totalReports > 0
                ? roundDouble(((double) handledReports / totalReports) * 100.0)
                : 0.0;

        CommunityStatsResponses.ReportStatsResponse response = new CommunityStatsResponses.ReportStatsResponse(
                totalReports,
                pendingReports,
                resolutionRate,
                byStatus,
                byTargetType,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_REPORTS_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    private StatsDateRangeRequest sanitizeRequest(StatsDateRangeRequest request) {
        if (request == null) {
            return new StatsDateRangeRequest(null, null, null);
        }
        return request;
    }

    private double roundDouble(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
