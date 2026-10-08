package com.eldercare.service;

/**
 * 系统用户与业务身份绑定检查。
 */
public interface UserBusinessIdentityService {

    boolean isBoundToCareStaff(Long userId);

    boolean isBoundToFamily(Long userId);

    void assertCanDisableOrDelete(Long userId);

    void assertRolesKeepBusinessRequirements(Long userId, java.util.Collection<String> newRoleCodes);
}
