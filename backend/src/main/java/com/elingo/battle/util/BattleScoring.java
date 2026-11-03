package com.elingo.battle.util;

public final class BattleScoring {

    public static final int BASE_POINTS = 100;
    public static final int MAX_SPEED_BONUS = 50;

    private BattleScoring() {}

    /**
     * Đúng: 100 + round(50 * còn_lại / giới_hạn).
     * Sai hoặc không trả lời: 0.
     */
    public static int points(boolean correct, long responseTimeMs, long timeLimitMs) {
        if (!correct) return 0;
        long remaining = Math.max(0, timeLimitMs - responseTimeMs);
        return BASE_POINTS + (int) Math.round((double) MAX_SPEED_BONUS * remaining / timeLimitMs);
    }
}

