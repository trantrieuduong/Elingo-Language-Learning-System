package com.elingo.gamification.service;

import com.elingo.gamification.dto.response.StreakResponse;

public interface StreakService {
    StreakResponse getUserStreak(Long userId, Integer year, Integer month);
}
