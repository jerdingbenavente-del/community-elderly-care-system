package com.eldercare.common;

import java.util.regex.Pattern;

/**
 * P8 账号安全常量：初始密码仅用于系统开户/重置，不用于用户自设密码校验放行。
 */
public final class AccountSecurityConstants {

    /** 业务开户与管理员重置的统一初始明文密码（入库前必须 BCrypt） */
    public static final String INITIAL_PASSWORD = "123456";

    /** 用户名自动生成最大重试次数 */
    public static final int USERNAME_GEN_MAX_ATTEMPTS = 10;

    /**
     * 用户自设/管理员设置的新密码：6-20 位，须含字母与数字，仅允许字母数字。
     */
    public static final Pattern NEW_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$");

    private AccountSecurityConstants() {
    }
}
