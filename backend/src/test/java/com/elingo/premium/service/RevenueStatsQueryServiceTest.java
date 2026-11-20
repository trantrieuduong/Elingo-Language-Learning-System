package com.elingo.premium.service;

import com.elingo.common.enums.StatsGranularity;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.service.impl.RevenueStatsQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RevenueStatsQueryServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private RevenueStatsQueryServiceImpl revenueStatsQueryService;

    private final LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 11, 3, 23, 59, 59);

    @Nested
    @DisplayName("getRevenueTimeSeries Tests")
    class GetRevenueTimeSeriesTests {

        @Test
        @DisplayName("Should query month timeseries when granularity is MONTH")
        void getRevenueTimeSeries_Month_CallsMonthQuery() {
            PaymentTransactionRepository.DateAmountCountProjection proj = mock(PaymentTransactionRepository.DateAmountCountProjection.class);
            when(paymentTransactionRepository.getRevenueTimeseriesByMonth(from, to)).thenReturn(List.of(proj));

            List<PaymentTransactionRepository.DateAmountCountProjection> result =
                    revenueStatsQueryService.getRevenueTimeSeries(from, to, StatsGranularity.MONTH);

            assertThat(result).hasSize(1).contains(proj);
            verify(paymentTransactionRepository).getRevenueTimeseriesByMonth(from, to);
            verify(paymentTransactionRepository, never()).getRevenueTimeseriesByDay(any(), any());
        }

        @Test
        @DisplayName("Should query day timeseries when granularity is DAY")
        void getRevenueTimeSeries_Day_CallsDayQuery() {
            PaymentTransactionRepository.DateAmountCountProjection proj = mock(PaymentTransactionRepository.DateAmountCountProjection.class);
            when(paymentTransactionRepository.getRevenueTimeseriesByDay(from, to)).thenReturn(List.of(proj));

            List<PaymentTransactionRepository.DateAmountCountProjection> result =
                    revenueStatsQueryService.getRevenueTimeSeries(from, to, StatsGranularity.DAY);

            assertThat(result).hasSize(1).contains(proj);
            verify(paymentTransactionRepository).getRevenueTimeseriesByDay(from, to);
            verify(paymentTransactionRepository, never()).getRevenueTimeseriesByMonth(any(), any());
        }

        @Test
        @DisplayName("Should query day timeseries when granularity is WEEK")
        void getRevenueTimeSeries_Week_CallsDayQuery() {
            when(paymentTransactionRepository.getRevenueTimeseriesByDay(from, to)).thenReturn(List.of());

            List<PaymentTransactionRepository.DateAmountCountProjection> result =
                    revenueStatsQueryService.getRevenueTimeSeries(from, to, StatsGranularity.WEEK);

            assertThat(result).isEmpty();
            verify(paymentTransactionRepository).getRevenueTimeseriesByDay(from, to);
        }
    }

    @Nested
    @DisplayName("getRevenueBreakdownByPlan Tests")
    class GetRevenueBreakdownByPlanTests {

        @Test
        @DisplayName("Should delegate to paymentTransactionRepository.getRevenueBreakdownByPlan")
        void getRevenueBreakdownByPlan_Success() {
            PaymentTransactionRepository.PlanRevenueStat stat = mock(PaymentTransactionRepository.PlanRevenueStat.class);
            when(paymentTransactionRepository.getRevenueBreakdownByPlan(from, to)).thenReturn(List.of(stat));

            List<PaymentTransactionRepository.PlanRevenueStat> result =
                    revenueStatsQueryService.getRevenueBreakdownByPlan(from, to);

            assertThat(result).hasSize(1).contains(stat);
            verify(paymentTransactionRepository).getRevenueBreakdownByPlan(from, to);
        }
    }

    @Nested
    @DisplayName("countTransactionsByStatus Tests")
    class CountTransactionsByStatusTests {

        @Test
        @DisplayName("Should delegate to paymentTransactionRepository.countTransactionsByStatus")
        void countTransactionsByStatus_Success() {
            PaymentTransactionRepository.TransactionStatusCount count = mock(PaymentTransactionRepository.TransactionStatusCount.class);
            when(paymentTransactionRepository.countTransactionsByStatus(from, to)).thenReturn(List.of(count));

            List<PaymentTransactionRepository.TransactionStatusCount> result =
                    revenueStatsQueryService.countTransactionsByStatus(from, to);

            assertThat(result).hasSize(1).contains(count);
            verify(paymentTransactionRepository).countTransactionsByStatus(from, to);
        }
    }
}
