package com.eldercare.common;

/**
 * 护理员业务状态：复用库表 TINYINT（1在职/可用，0离职/停用）。
 */
public final class CareStaffStatuses {

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    private CareStaffStatuses() {
    }

    public static boolean isEnabled(Integer status) {
        return status != null && status == ENABLED;
    }
}
