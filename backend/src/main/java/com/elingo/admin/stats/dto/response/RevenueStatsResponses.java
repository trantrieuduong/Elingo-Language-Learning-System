package com.elingo.admin.stats.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RevenueStatsResponses {
    private RevenueStatsResponses() {}

    public record RevenuePoint(LocalDate date,
                               BigDecimal revenue,
                               Long transactionCount,
                               BigDecimal cumulativeRevenue
    ) {}

    public record RevenueTimeSeriesResponse(BigDecimal totalRevenue,
                                            Long totalSuccessTransactions,
                                            String currency,
                                            List<RevenuePoint> points,
                                            LocalDateTime generatedAt
    ) {}

    public record PlanRevenue(Long planId,
                              String planName,
                              BigDecimal revenue,
                              Long transactionCount,
                              Double percentage
    ) {}

    public record TransactionStatusBreakdown(Long totalTransactions,
                                             Long successCount,
                                             Long pendingCount,
                                             Long failedCount,
                                             Long refundedCount,
                                             Double successRate
    ) {}

    public record RevenueBreakdownResponse(List<PlanRevenue> byPlan,
                                           TransactionStatusBreakdown statusBreakdown,
                                           LocalDateTime generatedAt
    ) {}

    public record PlanSubscription(Long planId,
                                   String planName,
                                   Long activeCount,
                                   Double percentage
    ) {}

    public record SubscriptionStatsResponse(Long totalActiveSubscriptions,
                                            Long totalPremiumUsers,
                                            Long totalSystemUsers,
                                            Double premiumRate,
                                            List<PlanSubscription> activeByPlan,
                                            LocalDateTime generatedAt
    ) {}
}
