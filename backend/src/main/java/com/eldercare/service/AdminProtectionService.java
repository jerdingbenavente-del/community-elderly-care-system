package com.eldercare.service;

/**
 * 有效管理员数量保护：防止系统失去最后一个 ENABLED + 未删除 + ADMIN。
 */
public interface AdminProtectionService {

    long countEnabledAdmins();

    boolean isUserEnabledAdmin(Long userId);

    void assertCanRemoveAdminRole(Long userId, boolean newRolesContainAdmin);

    void assertCanDisableUser(Long userId);

    void assertCanDeleteUser(Long userId);
}
