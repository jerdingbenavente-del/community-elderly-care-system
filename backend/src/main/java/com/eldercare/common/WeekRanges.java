package com.eldercare.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 周菜单以周一为起始、周日为结束。
 */
public final class WeekRanges {

    private WeekRanges() {
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate sundayOf(LocalDate monday) {
        return monday.plusDays(6);
    }

    /** 当前日期所在周的下一周周一。周日录入时，下一周从次日周一开始。 */
    public static LocalDate nextWeekMonday(LocalDate today) {
        return mondayOf(today).plusWeeks(1);
    }
}
