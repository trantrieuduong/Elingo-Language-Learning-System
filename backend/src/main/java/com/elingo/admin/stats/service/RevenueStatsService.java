package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;

public interface RevenueStatsService {
    RevenueStatsResponses.RevenueTimeSeriesResponse getRevenueTimeSeries(StatsDateRangeRequest request);
    RevenueStatsResponses.RevenueBreakdownResponse getRevenueBreakdown(StatsDateRangeRequest request);
    RevenueStatsResponses.SubscriptionStatsResponse getSubscriptionStats();
}
