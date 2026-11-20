package com.elingo.admin.stats.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class UserStatsResponses {
    private UserStatsResponses() {}

    public record StatPoint<T, K>(T key, K value) {}

    public record UserGrowthResponse(Long totalBeforeRange,
                                     List<StatPoint<String, Long>> newUsers,
                                     List<StatPoint<String, Long>> cumulativeUsers,
                                     LocalDateTime generatedAt
    ) {}

    public record UserActivityResponse(Long currentDau,
                                       Long currentWau,
                                       Long currentMau,
                                       Double stickinessPercentage,
                                       List<StatPoint<String, Long>> dauTimeSeries,
                                       LocalDateTime generatedAt
    ) {}

    public record UserBreakdownResponse(Long totalUsers,
                                        Long verifiedCount,
                                        Double verifiedPercentage,
                                        Long activeCount,
                                        Double activePercentage,
                                        Long premiumCount,
                                        Double premiumPercentage,
                                        List<StatPoint<Short, Long>> levelDistribution,
                                        LocalDateTime generatedAt
    ) {}
}
