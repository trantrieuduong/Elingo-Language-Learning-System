package com.elingo.gamification.service;

import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GamificationStatsQueryService {
    List<XpEventRepository.DateCountProjection> countDauTimeSeries(LocalDateTime from, LocalDateTime to);
    long countDistinctActiveUsers(LocalDateTime from, LocalDateTime to);
    List<UserXpRepository.LevelDistribution> countUsersByLevel();
    List<XpEventRepository.ActivityByDateProjection> countActivityBySourceAndDate(LocalDateTime from, LocalDateTime to);
}
