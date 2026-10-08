package com.eldercare.common;

public final class MedicationStatuses {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    /** 今日计划内、尚未到服药时刻 */
    public static final String REMIND_PENDING = "PENDING";
    /** 已到服药时刻 */
    public static final String REMIND_DUE = "DUE";

    private MedicationStatuses() {
    }
}
