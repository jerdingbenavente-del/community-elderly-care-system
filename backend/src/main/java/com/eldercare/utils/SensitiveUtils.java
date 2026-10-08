package com.eldercare.utils;

/**
 * 敏感信息脱敏（不改变入库明文策略，仅控制出参）。
 */
public final class SensitiveUtils {

    private SensitiveUtils() {
    }

    /**
     * 身份证：保留前 3 后 4，中间掩码。
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        String value = idCard.trim();
        if (value.length() < 8) {
            return "****";
        }
        int maskLen = value.length() - 7;
        return value.substring(0, 3) + "*".repeat(maskLen) + value.substring(value.length() - 4);
    }
}
