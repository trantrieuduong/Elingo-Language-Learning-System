package com.elingo.battle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.battle")
public record BattleProperties(
        int totalRounds,
        long questionTimeMs,
        long countdownMs,
        long roundGapMs,
        long queueTimeoutMs,
        long reconnectGraceMs,
        long roomTtlMs,
        int minCorrectForXp,
        int minCardsRequired
) {
}
