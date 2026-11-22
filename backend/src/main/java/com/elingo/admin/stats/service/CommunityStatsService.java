package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;

public interface CommunityStatsService {
    CommunityStatsResponses.SocialStatsResponse getSocialStats(StatsDateRangeRequest request);
    CommunityStatsResponses.ModerationStatsResponse getModerationStats(StatsDateRangeRequest request);
    CommunityStatsResponses.ReportStatsResponse getReportStats(StatsDateRangeRequest request);
}
