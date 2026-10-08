package com.eldercare.utils;

import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 用户自设密码复杂度校验（不含系统初始密码 123456 例外场景）。
 */
public final class PasswordRules {

    private PasswordRules() {
    }

    public static void assertValidNewPassword(String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能为空");
        }
        if (newPassword.contains(" ") || newPassword.contains("\t")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能包含空格");
        }
        if (!AccountSecurityConstants.NEW_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "密码须为6-20位，且同时包含英文字母和数字");
        }
    }

    public static void assertConfirmMatch(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(confirmPassword) || !confirmPassword.equals(newPassword)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }
    }
}
