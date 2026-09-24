package com.eldercare.common;

/**
 * 系统账号状态：复用 TINYINT（1启用，0停用）。
 */
public final class UserStatuses {

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    private UserStatuses() {
    }

    public static boolean isEnabled(Integer status) {
        return status != null && status == ENABLED;
    }
}
