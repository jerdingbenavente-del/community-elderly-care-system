package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CareStaffIdentityServiceImpl implements CareStaffIdentityService {

    private final CareStaffMapper careStaffMapper;
    private final SysUserMapper sysUserMapper;

    public CareStaffIdentityServiceImpl(CareStaffMapper careStaffMapper, SysUserMapper sysUserMapper) {
        this.careStaffMapper = careStaffMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public CareStaff getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return careStaffMapper.selectOne(new LambdaQueryWrapper<CareStaff>()
                .eq(CareStaff::getUserId, userId)
                .last("LIMIT 1"));
    }

    @Override
    public CareStaff requireByUserId(Long userId) {
        CareStaff staff = getByUserId(userId);
        if (staff == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前用户不是护理员");
        }
        return staff;
    }

    @Override
    public CareStaff getCurrentCareStaff() {
        LoginUser user = SecurityUtils.requireLoginUser();
        return getByUserId(user.getUserId());
    }

    @Override
    public CareStaff requireCurrentCareStaff() {
        return requireByUserId(SecurityUtils.requireLoginUser().getUserId());
    }

    @Override
    public CareStaff requireEnabledCurrentCareStaff() {
        CareStaff staff = requireCurrentCareStaff();
        if (!CareStaffStatuses.isEnabled(staff.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "护理员已停用");
        }
        return staff;
    }

    @Override
    public void validateUserBinding(Long userId, Long excludeCareStaffId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "系统用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "系统用户状态异常");
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        if (roles == null || !roles.contains("CARE_STAFF")) {
            throw new BusinessException(ResultCode.CONFLICT, "系统用户未拥有 CARE_STAFF 角色");
        }
        LambdaQueryWrapper<CareStaff> wrapper = new LambdaQueryWrapper<CareStaff>()
                .eq(CareStaff::getUserId, userId);
        if (excludeCareStaffId != null) {
            wrapper.ne(CareStaff::getId, excludeCareStaffId);
        }
        Long bound = careStaffMapper.selectCount(wrapper);
        if (bound != null && bound > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "系统用户已经绑定护理员");
        }
    }
}
