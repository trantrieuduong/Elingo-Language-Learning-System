package com.elingo.gamification.util;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

public final class LeaderboardKeyUtils {
    private LeaderboardKeyUtils() {}

    public static final String PREFIX_WEEKLY = "leaderboard:weekly:";
    public static final String PREFIX_MONTHLY = "leaderboard:monthly:";

    public static String getWeeklyPeriodKey(LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return "%d-W%02d".formatted(year, week);
    }

    public static String getMonthlyPeriodKey(LocalDate date) {
        return "%d-%02d".formatted(date.getYear(), date.getMonthValue());
    }

    public static String getWeeklyRedisKey(LocalDate date) {
        return PREFIX_WEEKLY + getWeeklyPeriodKey(date);
    }

    public static String getMonthlyRedisKey(LocalDate date) {
        return PREFIX_MONTHLY + getMonthlyPeriodKey(date);
    }
}
