package com.elingo.gamification.service.impl;

import com.elingo.gamification.dto.projection.ActivityByDateProjection;
import com.elingo.gamification.dto.projection.DateCountProjection;
import com.elingo.gamification.dto.projection.LevelDistribution;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.GamificationStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationStatsQueryServiceImpl implements GamificationStatsQueryService {

    private final XpEventRepository xpEventRepository;
    private final UserXpRepository userXpRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DateCountProjection> countDauTimeSeries(LocalDateTime from, LocalDateTime to) {
        return xpEventRepository.countDauByDay(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctActiveUsers(LocalDateTime from, LocalDateTime to) {
        return xpEventRepository.countDistinctUsersBetween(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelDistribution> countUsersByLevel() {
        return userXpRepository.countUsersByLevel();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityByDateProjection> countActivityBySourceAndDate(LocalDateTime from, LocalDateTime to) {
        return xpEventRepository.countActivityBySourceAndDate(from, to);
    }
}
