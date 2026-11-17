package com.elingo.gamification.listener;

import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j(topic = "LEADERBOARD-WARMUP")
@Component
@RequiredArgsConstructor
public class LeaderboardWarmupListener {

    private final LeaderboardService leaderboardService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupLeaderboards() {
        try {
            log.info("Starting Leaderboard warmup on application ready...");
            leaderboardService.warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);
            leaderboardService.warmupLeaderboardIfAbsent(LeaderboardPeriodType.MONTHLY);
            log.info("Leaderboard warmup on application ready completed");
        } catch (Exception ex) {
            log.warn("Leaderboard warmup on application ready failed: {}", ex.getMessage());
        }
    }
}
