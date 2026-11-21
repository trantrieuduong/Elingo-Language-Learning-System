package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.UserStatsResponses;

public interface UserStatsService {
    UserStatsResponses.UserGrowthResponse getUserGrowth(StatsDateRangeRequest request);
    UserStatsResponses.UserActivityResponse getUserActivity(StatsDateRangeRequest request);
    UserStatsResponses.UserBreakdownResponse getUserBreakdown();
}
