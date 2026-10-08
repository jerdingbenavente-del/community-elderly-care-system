package com.eldercare.service;

/**
 * 根据姓名生成唯一登录用户名：拼音首字母 + @ + 5 位数字（可含前导 0）。
 */
public interface UsernameGenerateService {

    /**
     * 生成尚未占用的 username；最多重试 {@link com.eldercare.common.AccountSecurityConstants#USERNAME_GEN_MAX_ATTEMPTS} 次。
     */
    String generateUniqueUsername(String realName);

    /**
     * 仅生成姓名对应前缀（小写字母数字），不含 @ 与数字后缀。供单测。
     */
    String buildNamePrefix(String realName);
}
