package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.ElderFamily;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.DataPermissionService;
import com.eldercare.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class DataPermissionServiceImpl implements DataPermissionService {

    private final ElderFamilyMapper elderFamilyMapper;

    public DataPermissionServiceImpl(ElderFamilyMapper elderFamilyMapper) {
        this.elderFamilyMapper = elderFamilyMapper;
    }

    @Override
    public boolean isFamilyBound(Long familyUserId, Long elderId) {
        if (familyUserId == null || elderId == null) {
            return false;
        }
        Long count = elderFamilyMapper.selectCount(new LambdaQueryWrapper<ElderFamily>()
                .eq(ElderFamily::getFamilyUserId, familyUserId)
                .eq(ElderFamily::getElderId, elderId)
                .eq(ElderFamily::getStatus, 1));
        return count != null && count > 0;
    }

    @Override
    public void checkFamilyAccess(Long familyUserId, Long elderId) {
        if (!isFamilyBound(familyUserId, elderId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据");
        }
    }

    @Override
    public void denyFamilyOnAdminApi() {
        LoginUser user = SecurityUtils.requireLoginUser();
        // Type C：仅拦截纯 FAMILY；多角色可进管理端，家属数据仍靠业务层 checkFamilyAccess
        if (user.isFamilyOnly()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "家属请使用家属端接口访问老人数据");
        }
    }
}
