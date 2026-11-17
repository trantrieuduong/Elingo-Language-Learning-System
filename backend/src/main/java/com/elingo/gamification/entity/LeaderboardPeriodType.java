package com.elingo.gamification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Leaderboard time period: WEEKLY (Monday to Sunday) or MONTHLY (Calendar month)")
public enum LeaderboardPeriodType {
    WEEKLY,
    MONTHLY
}
