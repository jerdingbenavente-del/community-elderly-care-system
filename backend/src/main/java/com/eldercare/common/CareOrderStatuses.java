package com.eldercare.common;

/**
 * 照护服务订单状态（集中常量，禁止 Controller 随意改状态）。
 */
public final class CareOrderStatuses {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String IN_SERVICE = "IN_SERVICE";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static final String ITEM_ENABLED = "ENABLED";
    public static final String ITEM_DISABLED = "DISABLED";

    private CareOrderStatuses() {
    }
}
