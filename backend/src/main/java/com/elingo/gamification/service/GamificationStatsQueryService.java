package com.elingo.gamification.service;

import com.elingo.gamification.dto.projection.ActivityByDateProjection;
import com.elingo.gamification.dto.projection.DateCountProjection;
import com.elingo.gamification.dto.projection.LevelDistribution;

import java.time.LocalDateTime;
import java.util.List;

public interface GamificationStatsQueryService {
    List<DateCountProjection> countDauTimeSeries(LocalDateTime from, LocalDateTime to);
    long countDistinctActiveUsers(LocalDateTime from, LocalDateTime to);
    List<LevelDistribution> countUsersByLevel();
    List<ActivityByDateProjection> countActivityBySourceAndDate(LocalDateTime from, LocalDateTime to);
}
