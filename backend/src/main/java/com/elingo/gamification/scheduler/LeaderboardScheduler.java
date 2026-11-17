package com.elingo.gamification.scheduler;

import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.util.LeaderboardKeyUtils;
import com.elingo.gamification.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "LEADERBOARD-SCHEDULER")
public class LeaderboardScheduler {

    private final LeaderboardService leaderboardService;

    /**
     * Chốt bảng xếp hạng tuần vào 23:59:45 mỗi Chủ nhật.
     */
    @Scheduled(cron = "45 59 23 * * SUN")
    public void snapshotWeeklyLeaderboard() {
        LocalDate today = LocalDate.now();
        String periodKey = LeaderboardKeyUtils.getWeeklyPeriodKey(today);
        log.info("Weekly leaderboard snapshot scheduled task started periodKey={}", periodKey);
        try {
            leaderboardService.snapshotLeaderboard(LeaderboardPeriodType.WEEKLY, periodKey);
            log.info("Weekly leaderboard snapshot scheduled task completed periodKey={}", periodKey);
        } catch (Exception ex) {
            log.error("Weekly leaderboard snapshot scheduled task failed periodKey={}", periodKey, ex);
        }
    }

    /**
     * Chốt bảng xếp hạng tháng vào 23:59:50 ngày cuối cùng của tháng.
     */
    @Scheduled(cron = "50 59 23 * * *")
    public void snapshotMonthlyLeaderboard() {
        LocalDate today = LocalDate.now();
        if (!today.equals(today.with(TemporalAdjusters.lastDayOfMonth()))) {
            return;
        }

        String periodKey = LeaderboardKeyUtils.getMonthlyPeriodKey(today);
        log.info("Monthly leaderboard snapshot scheduled task started periodKey={}", periodKey);
        try {
            leaderboardService.snapshotLeaderboard(LeaderboardPeriodType.MONTHLY, periodKey);
            log.info("Monthly leaderboard snapshot scheduled task completed periodKey={}", periodKey);
        } catch (Exception ex) {
            log.error("Monthly leaderboard snapshot scheduled task failed periodKey={}", periodKey, ex);
        }
    }
}
