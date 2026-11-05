package com.elingo.battle.dto.response;

public record BattleStatsResponse(
        long totalMatches,
        long wins,
        long losses,
        long draws,
        double winRate
) {
}

