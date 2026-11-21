package com.elingo.premium.service;

import com.elingo.common.enums.StatsGranularity;
import com.elingo.premium.repository.PaymentTransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RevenueStatsQueryService {
    List<PaymentTransactionRepository.DateAmountCountProjection> getRevenueTimeSeries(LocalDateTime from, LocalDateTime to, StatsGranularity granularity);
    List<PaymentTransactionRepository.PlanRevenueStat> getRevenueBreakdownByPlan(LocalDateTime from, LocalDateTime to);
    List<PaymentTransactionRepository.TransactionStatusCount> countTransactionsByStatus(LocalDateTime from, LocalDateTime to);
}
