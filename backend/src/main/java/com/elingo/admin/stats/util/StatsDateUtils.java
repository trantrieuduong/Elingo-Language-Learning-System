package com.elingo.admin.stats.util;

import com.elingo.common.enums.StatsGranularity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class StatsDateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private StatsDateUtils() {
    }

    public static List<String> generatePeriodKeys(LocalDate from, LocalDate to, StatsGranularity granularity) {
        List<String> keys = new ArrayList<>();
        if (from == null || to == null || from.isAfter(to)) {
            return keys;
        }

        switch (granularity) {
            case WEEK -> {
                LocalDate current = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!current.isAfter(end)) {
                    keys.add(current.format(DATE_FORMATTER));
                    current = current.plusWeeks(1);
                }
            }
            case MONTH -> {
                LocalDate current = from.withDayOfMonth(1);
                LocalDate end = to.withDayOfMonth(1);
                while (!current.isAfter(end)) {
                    keys.add(current.format(MONTH_FORMATTER));
                    current = current.plusMonths(1);
                }
            }
            case DAY -> {
                LocalDate current = from;
                while (!current.isAfter(to)) {
                    keys.add(current.format(DATE_FORMATTER));
                    current = current.plusDays(1);
                }
            }
        }
        return keys;
    }
}
