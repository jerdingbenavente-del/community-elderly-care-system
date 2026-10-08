package com.eldercare.service;

/**
 * 家属数据权限：后续健康/服务等模块应复用本服务，禁止在 Controller 复制判断。
 */
public interface DataPermissionService {

    boolean isFamilyBound(Long familyUserId, Long elderId);

    /**
     * 校验当前家属用户是否可访问目标老人；失败抛出 403。
     */
    void checkFamilyAccess(Long familyUserId, Long elderId);

    /**
     * 管理端接口拒绝「纯家属」角色绕过（须走 /api/family/**）。
     * 多角色（如 FAMILY+CARE_STAFF）允许进入管理端 API，但其家属业务数据权限须在业务层单独 checkFamilyAccess。
     */
    void denyFamilyOnAdminApi();
}
