package com.eldercare.common;

/**
 * 照护服务订单支付状态（与 CareOrderStatuses 业务五态独立）。
 */
public final class CarePaymentStatuses {

    public static final String UNPAID = "UNPAID";
    public static final String PAID = "PAID";

    private CarePaymentStatuses() {
    }
}
