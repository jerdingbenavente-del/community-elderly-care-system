package com.eldercare.common;

/**
 * 考勤状态。库内只落 WORKING / COMPLETED。
 * NOT_CHECKED、LEAVE 为查询时的展示状态，不单独入库。
 */
public final class CareAttendanceStatuses {

    public static final String NOT_CHECKED = "NOT_CHECKED";
    public static final String WORKING = "WORKING";
    public static final String COMPLETED = "COMPLETED";
    public static final String LEAVE = "LEAVE";

    private CareAttendanceStatuses() {
    }
}
