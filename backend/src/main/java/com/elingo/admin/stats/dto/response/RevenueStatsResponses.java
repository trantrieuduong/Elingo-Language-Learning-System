package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RevenueStatsResponses {
    private RevenueStatsResponses() {}

    @Schema(description = "A single data point representing revenue and transaction volume for a date period")
    public record RevenuePoint(
            @Schema(description = "Date or first day of the period (YYYY-MM-DD)", example = "2026-11-20")
            LocalDate date,

            @Schema(description = "Total successful revenue generated in this period", example = "1500000.00")
            BigDecimal revenue,

            @Schema(description = "Number of successful payment transactions in this period", example = "5")
            Long transactionCount,

            @Schema(description = "Running cumulative revenue up to and including this period", example = "24500000.00")
            BigDecimal cumulativeRevenue
    ) {}

    @Schema(description = "Revenue and successful transaction time-series response with zero-filled gaps")
    public record RevenueTimeSeriesResponse(
            @Schema(description = "Total revenue accumulated across the entire query range", example = "45000000.00")
            BigDecimal totalRevenue,

            @Schema(description = "Total count of successful payment transactions across the query range", example = "150")
            Long totalSuccessTransactions,

            @Schema(description = "Currency code for all monetary amounts", example = "VND")
            String currency,

            @Schema(description = "Continuous time-series points with zero-filled missing dates")
            List<RevenuePoint> points,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Revenue and transaction distribution for a specific subscription plan")
    public record PlanRevenue(
            @Schema(description = "Unique identifier of the subscription plan", example = "1")
            Long planId,

            @Schema(description = "Name of the subscription plan", example = "Premium 1 Month")
            String planName,

            @Schema(description = "Total revenue earned from this plan during the period", example = "12000000.00")
            BigDecimal revenue,

            @Schema(description = "Total successful transactions for this plan", example = "80")
            Long transactionCount,

            @Schema(description = "Percentage contribution of this plan to total plan revenue", example = "60.0")
            Double percentage
    ) {}

    @Schema(description = "Breakdown of payment transactions by their execution status and overall success rate")
    public record TransactionStatusBreakdown(
            @Schema(description = "Total number of payment transactions created in the period", example = "200")
            Long totalTransactions,

            @Schema(description = "Number of successfully completed transactions", example = "180")
            Long successCount,

            @Schema(description = "Number of transactions currently pending payment or confirmation", example = "10")
            Long pendingCount,

            @Schema(description = "Number of failed payment transactions", example = "8")
            Long failedCount,

            @Schema(description = "Number of refunded transactions", example = "2")
            Long refundedCount,

            @Schema(description = "Percentage of successful transactions over total transactions", example = "90.0")
            Double successRate
    ) {}

    @Schema(description = "Comprehensive revenue breakdown by subscription plan and transaction statuses")
    public record RevenueBreakdownResponse(
            @Schema(description = "List of plan-specific revenue contributions")
            List<PlanRevenue> byPlan,

            @Schema(description = "Payment transaction status counts and success rate")
            TransactionStatusBreakdown statusBreakdown,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Active subscription count and percentage share for a specific plan")
    public record PlanSubscription(
            @Schema(description = "Unique identifier of the subscription plan", example = "2")
            Long planId,

            @Schema(description = "Name of the subscription plan", example = "Premium 1 Year")
            String planName,

            @Schema(description = "Number of currently active subscriptions for this plan", example = "120")
            Long activeCount,

            @Schema(description = "Share of this plan over total active subscriptions", example = "40.0")
            Double percentage
    ) {}

    @Schema(description = "Active subscription overview and premium penetration metrics across all system users")
    public record SubscriptionStatsResponse(
            @Schema(description = "Total active subscriptions currently in effect", example = "300")
            Long totalActiveSubscriptions,

            @Schema(description = "Number of distinct users currently holding active Premium status", example = "290")
            Long totalPremiumUsers,

            @Schema(description = "Total registered users across the entire system", example = "2500")
            Long totalSystemUsers,

            @Schema(description = "Premium penetration rate as a percentage (totalPremiumUsers / totalSystemUsers * 100)", example = "11.6")
            Double premiumRate,

            @Schema(description = "List of active subscription distributions grouped by plan")
            List<PlanSubscription> activeByPlan,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}
}
