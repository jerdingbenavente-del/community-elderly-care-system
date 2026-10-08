package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.dto.BusinessAccountCreateDTO;
import com.eldercare.entity.SysRole;
import com.eldercare.entity.SysUser;
import com.eldercare.entity.SysUserRole;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysRoleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.SysUserRoleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.BusinessAccountService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.service.UsernameGenerateService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.BusinessAccountVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessAccountServiceImpl implements BusinessAccountService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final UsernameGenerateService usernameGenerateService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public BusinessAccountServiceImpl(SysUserMapper sysUserMapper,
                                      SysRoleMapper sysRoleMapper,
                                      SysUserRoleMapper sysUserRoleMapper,
                                      PasswordEncoder passwordEncoder,
                                      UsernameGenerateService usernameGenerateService,
                                      DataPermissionService dataPermissionService,
                                      OperationLogService operationLogService) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.usernameGenerateService = usernameGenerateService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public BusinessAccountVO create(BusinessAccountCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();

        String roleCode = dto.getRoleCode().trim().toUpperCase();
        if (!"FAMILY".equals(roleCode) && !"CARE_STAFF".equals(roleCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持为 FAMILY 或 CARE_STAFF 自动开户");
        }

        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getStatus, 1)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }

        String realName = dto.getName().trim();
        String username = usernameGenerateService.generateUniqueUsername(realName);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(AccountSecurityConstants.INITIAL_PASSWORD));
        user.setRealName(realName);
        user.setStatus(UserStatuses.ENABLED);
        user.setMustChangePassword(true);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "账号自动生成失败，请重新尝试");
        }

        SysUserRole ur = new SysUserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(role.getId());
        sysUserRoleMapper.insert(ur);

        operationLogService.record("system", "BUSINESS_ACCOUNT_CREATE", "sys_user",
                String.valueOf(user.getId()), "SUCCESS", request);

        BusinessAccountVO vo = new BusinessAccountVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleCode(roleCode);
        vo.setMustChangePassword(true);
        return vo;
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可业务开户");
        }
    }
}
