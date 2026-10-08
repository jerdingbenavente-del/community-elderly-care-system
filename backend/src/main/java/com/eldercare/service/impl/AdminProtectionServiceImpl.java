package com.eldercare.service.impl;

import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.AdminProtectionService;
import org.springframework.stereotype.Service;

@Service
public class AdminProtectionServiceImpl implements AdminProtectionService {

    private final SysUserMapper sysUserMapper;

    public AdminProtectionServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public long countEnabledAdmins() {
        return sysUserMapper.countEnabledAdmins();
    }

    @Override
    public boolean isUserEnabledAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !UserStatuses.isEnabled(user.getStatus())) {
            return false;
        }
        return sysUserMapper.countAdminRoleOfUser(userId) > 0;
    }

    @Override
    public void assertCanRemoveAdminRole(Long userId, boolean newRolesContainAdmin) {
        if (newRolesContainAdmin) {
            return;
        }
        if (!isUserEnabledAdmin(userId)) {
            return;
        }
        if (countEnabledAdmins() <= 1) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "系统至少需要保留一个启用状态的管理员，不能移除最后一个管理员角色");
        }
    }

    @Override
    public void assertCanDisableUser(Long userId) {
        if (isUserEnabledAdmin(userId) && countEnabledAdmins() <= 1) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "不能停用最后一个有效管理员");
        }
    }

    @Override
    public void assertCanDeleteUser(Long userId) {
        if (isUserEnabledAdmin(userId) && countEnabledAdmins() <= 1) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "不能删除最后一个有效管理员");
        }
    }
}
