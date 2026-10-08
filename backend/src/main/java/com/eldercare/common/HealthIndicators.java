package com.eldercare.common;

/**
 * 健康指标类型（辅助提醒，非医疗诊断）。
 */
public final class HealthIndicators {

    public static final String SYSTOLIC_PRESSURE = "SYSTOLIC_PRESSURE";
    public static final String DIASTOLIC_PRESSURE = "DIASTOLIC_PRESSURE";
    public static final String BLOOD_GLUCOSE = "BLOOD_GLUCOSE";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String HEART_RATE = "HEART_RATE";

    public static final String DIRECTION_HIGH = "HIGH";
    public static final String DIRECTION_LOW = "LOW";

    public static final String STATUS_UNHANDLED = "UNHANDLED";
    public static final String STATUS_HANDLED = "HANDLED";

    private HealthIndicators() {
    }
}
