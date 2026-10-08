package com.eldercare.common;

/**
 * 社区活动状态。
 */
public final class ActivityStatuses {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    private ActivityStatuses() {
    }

    public static boolean isTerminal(String status) {
        return CANCELLED.equals(status) || COMPLETED.equals(status);
    }
}
