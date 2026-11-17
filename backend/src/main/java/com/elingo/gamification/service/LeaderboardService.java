package com.elingo.gamification.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import org.springframework.data.domain.Pageable;

public interface LeaderboardService {
    LeaderboardResponse getRealtimeLeaderboard(Long currentUserId, LeaderboardPeriodType periodType, Pageable pageable);
    MyLeaderboardRankResponse getMyRank(Long currentUserId, LeaderboardPeriodType periodType);
    PageResponse<LeaderboardItemResponse> getHistoricalLeaderboard(LeaderboardPeriodType periodType, String periodKey, Pageable pageable);
    void recordXpForLeaderboard(Long userId, int amount);
    void snapshotLeaderboard(LeaderboardPeriodType periodType, String periodKey);
    void syncLeaderboardFromDatabase(LeaderboardPeriodType periodType);
    void warmupLeaderboardIfAbsent(LeaderboardPeriodType periodType);
}
