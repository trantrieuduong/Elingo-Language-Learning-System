package com.elingo.premium.service;

import com.elingo.common.enums.StatsGranularity;
import com.elingo.premium.dto.projection.DateAmountCountProjection;
import com.elingo.premium.dto.projection.PlanRevenueStat;
import com.elingo.premium.dto.projection.TransactionStatusCount;

import java.time.LocalDateTime;
import java.util.List;

public interface RevenueStatsQueryService {
    List<DateAmountCountProjection> getRevenueTimeSeries(LocalDateTime from, LocalDateTime to, StatsGranularity granularity);
    List<PlanRevenueStat> getRevenueBreakdownByPlan(LocalDateTime from, LocalDateTime to);
    List<TransactionStatusCount> countTransactionsByStatus(LocalDateTime from, LocalDateTime to);
}
