package com.elingo.admin.stats.util;

import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.common.enums.StatsGranularity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatsZeroFiller {

    private StatsZeroFiller() {
    }

    public record UserGrowthSeriesResult(
            List<UserStatsResponses.StatPoint<String, Long>> newUsers,
            List<UserStatsResponses.StatPoint<String, Long>> cumulativeUsers
    ) {}

    /**
     * Fills missing time periods with zero for new users and calculates cumulative users
     */
    public static UserGrowthSeriesResult fillUserGrowthSeries(
            LocalDate from,
            LocalDate to,
            StatsGranularity granularity,
            Map<String, Long> rawDataMap,
            long initialCumulative) {

        List<String> allPeriods = StatsDateUtils.generatePeriodKeys(from, to, granularity);
        List<UserStatsResponses.StatPoint<String, Long>> newUsersList = new ArrayList<>(allPeriods.size());
        List<UserStatsResponses.StatPoint<String, Long>> cumulativeList = new ArrayList<>(allPeriods.size());
        long cumulative = initialCumulative;

        for (String period : allPeriods) {
            Long val = rawDataMap.getOrDefault(period, 0L);
            cumulative += val;
            newUsersList.add(new UserStatsResponses.StatPoint<>(period, val));
            cumulativeList.add(new UserStatsResponses.StatPoint<>(period, cumulative));
        }

        return new UserGrowthSeriesResult(newUsersList, cumulativeList);
    }

    /**
     * Fills missing dates with zero for DAU time series
     */
    public static List<UserStatsResponses.StatPoint<String, Long>> fillDauSeries(
            LocalDate from,
            LocalDate to,
            Map<String, Long> rawDataMap) {

        List<String> allPeriods = StatsDateUtils.generatePeriodKeys(from, to, StatsGranularity.DAY);
        List<UserStatsResponses.StatPoint<String, Long>> result = new ArrayList<>(allPeriods.size());

        for (String period : allPeriods) {
            Long activeUsers = rawDataMap.getOrDefault(period, 0L);
            result.add(new UserStatsResponses.StatPoint<>(period, activeUsers));
        }
        return result;
    }

    public record RawRevenueData(BigDecimal revenue, Long transactionCount) {}

    /**
     * Fills missing time periods with zero for revenue time series
     */
    public static List<RevenueStatsResponses.RevenuePoint> fillRevenueSeries(
            LocalDate from,
            LocalDate to,
            StatsGranularity granularity,
            Map<String, RawRevenueData> rawDataMap) {

        List<String> allPeriods = StatsDateUtils.generatePeriodKeys(from, to, granularity);
        List<RevenueStatsResponses.RevenuePoint> result = new ArrayList<>(allPeriods.size());
        BigDecimal cumulativeRevenue = BigDecimal.ZERO;

        for (String period : allPeriods) {
            RawRevenueData raw = rawDataMap.get(period);
            BigDecimal revenue = raw != null && raw.revenue() != null ? raw.revenue() : BigDecimal.ZERO;
            Long count = raw != null && raw.transactionCount() != null ? raw.transactionCount() : 0L;
            cumulativeRevenue = cumulativeRevenue.add(revenue);

            LocalDate datePoint = LocalDate.parse(period.length() == 7 ? period + "-01" : period);
            result.add(new RevenueStatsResponses.RevenuePoint(
                    datePoint,
                    revenue,
                    count,
                    cumulativeRevenue
            ));
        }
        return result;
    }

    public record RawLearningActivity(
            Long flashcard,
            Long dictation,
            Long shadowing,
            Long battle,
            Long checkIn,
            Long total
    ) {}

    /**
     * Fills missing dates with zero for learning activities stacked series
     */
    @SuppressWarnings("java:S3776") // Độ phức tạp chủ yếu do các toán tử 3 ngôi gán giá trị mặc định 0L cho từng trường
    public static List<LearningStatsResponses.LearningActivityPoint> fillLearningActivitySeries(
            LocalDate from,
            LocalDate to,
            Map<String, RawLearningActivity> rawDataMap) {

        List<String> allPeriods = StatsDateUtils.generatePeriodKeys(from, to, StatsGranularity.DAY);
        List<LearningStatsResponses.LearningActivityPoint> result = new ArrayList<>(allPeriods.size());

        for (String period : allPeriods) {
            RawLearningActivity raw = rawDataMap.get(period);
            LocalDate datePoint = LocalDate.parse(period);
            if (raw != null) {
                result.add(new LearningStatsResponses.LearningActivityPoint(
                        datePoint,
                        raw.flashcard() != null ? raw.flashcard() : 0L,
                        raw.dictation() != null ? raw.dictation() : 0L,
                        raw.shadowing() != null ? raw.shadowing() : 0L,
                        raw.battle() != null ? raw.battle() : 0L,
                        raw.checkIn() != null ? raw.checkIn() : 0L,
                        raw.total() != null ? raw.total() : 0L
                ));
            } else {
                result.add(new LearningStatsResponses.LearningActivityPoint(
                        datePoint, 0L, 0L, 0L, 0L, 0L, 0L
                ));
            }
        }
        return result;
    }

    /**
     * Fills missing dates with zero for social engagement (likes and comments) series
     */
    public static List<CommunityStatsResponses.SocialEngagementPoint> fillSocialEngagementSeries(
            LocalDate from,
            LocalDate to,
            Map<String, Long> likesMap,
            Map<String, Long> commentsMap) {

        List<String> allPeriods = StatsDateUtils.generatePeriodKeys(from, to, StatsGranularity.DAY);
        List<CommunityStatsResponses.SocialEngagementPoint> result = new ArrayList<>(allPeriods.size());

        for (String period : allPeriods) {
            Long likes = likesMap.getOrDefault(period, 0L);
            Long comments = commentsMap.getOrDefault(period, 0L);
            Long total = likes + comments;
            LocalDate datePoint = LocalDate.parse(period);

            result.add(new CommunityStatsResponses.SocialEngagementPoint(
                    datePoint,
                    likes,
                    comments,
                    total
            ));
        }
        return result;
    }
}
