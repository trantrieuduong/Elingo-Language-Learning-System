package com.elingo.premium.service.impl;

import com.elingo.common.enums.StatsGranularity;
import com.elingo.premium.dto.projection.DateAmountCountProjection;
import com.elingo.premium.dto.projection.PlanRevenueStat;
import com.elingo.premium.dto.projection.TransactionStatusCount;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.service.RevenueStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueStatsQueryServiceImpl implements RevenueStatsQueryService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DateAmountCountProjection> getRevenueTimeSeries(LocalDateTime from, LocalDateTime to, StatsGranularity granularity) {
        return switch (granularity) {
            case MONTH -> paymentTransactionRepository.getRevenueTimeseriesByMonth(from, to);
            case WEEK -> paymentTransactionRepository.getRevenueTimeseriesByWeek(from, to);
            case DAY -> paymentTransactionRepository.getRevenueTimeseriesByDay(from, to);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanRevenueStat> getRevenueBreakdownByPlan(LocalDateTime from, LocalDateTime to) {
        return paymentTransactionRepository.getRevenueBreakdownByPlan(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionStatusCount> countTransactionsByStatus(LocalDateTime from, LocalDateTime to) {
        return paymentTransactionRepository.countTransactionsByStatus(from, to);
    }
}
