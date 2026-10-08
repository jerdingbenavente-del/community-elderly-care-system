package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.ElderFamily;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.service.UserBusinessIdentityService;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class UserBusinessIdentityServiceImpl implements UserBusinessIdentityService {

    private final CareStaffMapper careStaffMapper;
    private final ElderFamilyMapper elderFamilyMapper;

    public UserBusinessIdentityServiceImpl(CareStaffMapper careStaffMapper,
                                           ElderFamilyMapper elderFamilyMapper) {
        this.careStaffMapper = careStaffMapper;
        this.elderFamilyMapper = elderFamilyMapper;
    }

    @Override
    public boolean isBoundToCareStaff(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = careStaffMapper.selectCount(new LambdaQueryWrapper<CareStaff>()
                .eq(CareStaff::getUserId, userId));
        return count != null && count > 0;
    }

    @Override
    public boolean isBoundToFamily(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = elderFamilyMapper.selectCount(new LambdaQueryWrapper<ElderFamily>()
                .eq(ElderFamily::getFamilyUserId, userId)
                .eq(ElderFamily::getStatus, 1));
        return count != null && count > 0;
    }

    @Override
    public void assertCanDisableOrDelete(Long userId) {
        if (isBoundToCareStaff(userId)) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "该系统账号已绑定护理员业务身份，请先按护理员业务规则处理，不能直接停用或删除账号");
        }
        if (isBoundToFamily(userId)) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "该系统账号已绑定家属业务身份，请先处理家属绑定关系，不能直接停用或删除账号");
        }
    }

    @Override
    public void assertRolesKeepBusinessRequirements(Long userId, Collection<String> newRoleCodes) {
        if (isBoundToCareStaff(userId) && (newRoleCodes == null || !newRoleCodes.contains("CARE_STAFF"))) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "绑定护理员的用户必须保留 CARE_STAFF 角色");
        }
        if (isBoundToFamily(userId) && (newRoleCodes == null || !newRoleCodes.contains("FAMILY"))) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "绑定家属业务身份的用户必须保留 FAMILY 角色");
        }
    }
}
