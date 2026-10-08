package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.config.UploadProperties;
import com.eldercare.dto.ChangePasswordDTO;
import com.eldercare.dto.SysUserCreateDTO;
import com.eldercare.dto.SysUserProfileUpdateDTO;
import com.eldercare.dto.SysUserQueryDTO;
import com.eldercare.dto.SysUserUpdateDTO;
import com.eldercare.dto.UserRoleUpdateDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.SysRole;
import com.eldercare.entity.SysUser;
import com.eldercare.entity.SysUserRole;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysRoleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.SysUserRoleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.AdminProtectionService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.service.SysUserService;
import com.eldercare.service.UserBusinessIdentityService;
import com.eldercare.utils.PasswordRules;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.RoleVO;
import com.eldercare.vo.SysUserVO;
import com.eldercare.vo.UserSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Set<String> AVATAR_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final CareStaffMapper careStaffMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminProtectionService adminProtectionService;
    private final UserBusinessIdentityService userBusinessIdentityService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;
    private final UploadProperties uploadProperties;

    public SysUserServiceImpl(SysUserMapper sysUserMapper,
                              SysRoleMapper sysRoleMapper,
                              SysUserRoleMapper sysUserRoleMapper,
                              CareStaffMapper careStaffMapper,
                              PasswordEncoder passwordEncoder,
                              AdminProtectionService adminProtectionService,
                              UserBusinessIdentityService userBusinessIdentityService,
                              DataPermissionService dataPermissionService,
                              OperationLogService operationLogService,
                              UploadProperties uploadProperties) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.careStaffMapper = careStaffMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminProtectionService = adminProtectionService;
        this.userBusinessIdentityService = userBusinessIdentityService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public List<UserSummaryVO> listUsers() {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId))
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public PageResult<SysUserVO> page(SysUserQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        Page<SysUser> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getRealName())) {
            wrapper.like(SysUser::getRealName, query.getRealName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getRoleCode())) {
            String roleCode = query.getRoleCode().trim();
            SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleCode)
                    .last("LIMIT 1"));
            if (role == null) {
                return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
            }
            List<Long> userIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getRoleId, role.getId()))
                    .stream().map(SysUserRole::getUserId).distinct().collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
            }
            wrapper.in(SysUser::getId, userIds);
        }
        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        List<SysUserVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SysUserVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        return toVo(requireUser(id));
    }

    @Override
    public SysUserVO me() {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        SysUser user = requireUser(loginUser.getUserId());
        return toVo(user);
    }

    @Override
    @Transactional
    public SysUserVO updateMe(SysUserProfileUpdateDTO dto, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        SysUser user = requireUser(loginUser.getUserId());
        if (!UserStatuses.isEnabled(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }

        String realName = dto.getRealName().trim();
        String phone = dto.getPhone() == null ? null : dto.getPhone().trim();
        if (phone != null && phone.isEmpty()) {
            phone = null;
        }

        user.setRealName(realName);
        user.setPhone(phone);
        sysUserMapper.updateById(user);
        syncCareStaffNameIfBound(user.getId(), realName);

        operationLogService.record("system", "USER_UPDATE_ME", "sys_user",
                String.valueOf(user.getId()), "SUCCESS", request);
        return toVo(requireUser(user.getId()));
    }

    @Override
    @Transactional
    public SysUserVO uploadMyAvatar(MultipartFile file, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        SysUser user = requireUser(loginUser.getUserId());
        if (!UserStatuses.isEnabled(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择头像文件");
        }
        if (file.getSize() > uploadProperties.getMaxAvatarBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像文件不能超过 2MB");
        }

        String ext = resolveAllowedImageExt(file);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadProperties.getBaseDir(), uploadProperties.getAvatarSubdir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "非法文件路径");
            }
            file.transferTo(target.toFile());
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "头像保存失败");
        }

        String url = "/uploads/" + uploadProperties.getAvatarSubdir() + "/" + filename;
        user.setAvatar(url);
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_UPLOAD_AVATAR", "sys_user",
                String.valueOf(user.getId()), "SUCCESS", request);
        return toVo(requireUser(user.getId()));
    }

    @Override
    @Transactional
    public Long create(SysUserCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        String username = dto.getUsername().trim();
        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        List<SysRole> roles = resolveRoles(dto.getRoleIds());
        assertOnlyAdminCanAssignAdmin(roles);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(UserStatuses.ENABLED);
        user.setMustChangePassword(false);
        sysUserMapper.insert(user);
        replaceUserRoles(user.getId(), roles);

        operationLogService.record("system", "USER_CREATE", "sys_user",
                String.valueOf(user.getId()), "SUCCESS", request);
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysUserUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        SysUser user = requireUser(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_UPDATE", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void enable(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        SysUser user = requireUser(id);
        user.setStatus(UserStatuses.ENABLED);
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_ENABLE", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void disable(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireUser(id);
        adminProtectionService.assertCanDisableUser(id);
        userBusinessIdentityService.assertCanDisableOrDelete(id);
        SysUser user = requireUser(id);
        user.setStatus(UserStatuses.DISABLED);
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_DISABLE", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireUser(id);
        LoginUser operator = SecurityUtils.requireLoginUser();
        if (operator.getUserId().equals(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "不能删除当前登录账号");
        }
        adminProtectionService.assertCanDeleteUser(id);
        userBusinessIdentityService.assertCanDisableOrDelete(id);
        sysUserMapper.deleteById(id);
        operationLogService.record("system", "USER_DELETE", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public List<RoleVO> listRoles(Long userId) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireUser(userId);
        return loadRoleDetails(userId);
    }

    @Override
    @Transactional
    public void updateRoles(Long id, UserRoleUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireUser(id);
        List<SysRole> roles = resolveRoles(dto.getRoleIds());
        assertOnlyAdminCanAssignAdmin(roles);

        Set<String> newCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toCollection(LinkedHashSet::new));
        adminProtectionService.assertCanRemoveAdminRole(id, newCodes.contains("ADMIN"));
        userBusinessIdentityService.assertRolesKeepBusinessRequirements(id, newCodes);

        replaceUserRoles(id, roles);
        operationLogService.record("system", "USER_ROLE_UPDATE", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDTO dto, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.requireLoginUser();
        SysUser user = requireUser(loginUser.getUserId());
        if (!UserStatuses.isEnabled(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码错误");
        }
        PasswordRules.assertConfirmMatch(dto.getNewPassword(), dto.getConfirmPassword());
        PasswordRules.assertValidNewPassword(dto.getNewPassword());
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setMustChangePassword(false);
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_CHANGE_PASSWORD", "sys_user",
                String.valueOf(user.getId()), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        LoginUser operator = SecurityUtils.requireLoginUser();
        if (operator.getUserId().equals(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能通过重置接口修改自己的密码，请使用修改密码");
        }
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(AccountSecurityConstants.INITIAL_PASSWORD));
        user.setMustChangePassword(true);
        sysUserMapper.updateById(user);
        operationLogService.record("system", "USER_RESET_PASSWORD", "sys_user",
                String.valueOf(id), "SUCCESS", request);
    }

    private void syncCareStaffNameIfBound(Long userId, String realName) {
        CareStaff staff = careStaffMapper.selectOne(new LambdaQueryWrapper<CareStaff>()
                .eq(CareStaff::getUserId, userId)
                .last("LIMIT 1"));
        if (staff == null) {
            return;
        }
        staff.setName(realName);
        careStaffMapper.updateById(staff);
    }

    private String resolveAllowedImageExt(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!AVATAR_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/jpeg/png/webp 图片");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件类型不是允许的图片格式");
        }
        return ext.equals("jpeg") ? "jpg" : ext;
    }

    private List<SysRole> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色列表不能为空");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(roleIds);
        List<SysRole> roles = new ArrayList<>();
        for (Long roleId : uniqueIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null || role.getStatus() == null || role.getStatus() != 1) {
                throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
            }
            roles.add(role);
        }
        return roles;
    }

    private void assertOnlyAdminCanAssignAdmin(List<SysRole> roles) {
        boolean assignAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
        if (assignAdmin && !SecurityUtils.requireLoginUser().hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权分配管理员角色");
        }
    }

    private void replaceUserRoles(Long userId, List<SysRole> roles) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (SysRole role : roles) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            sysUserRoleMapper.insert(ur);
        }
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可管理系统用户");
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统用户不存在");
        }
        return user;
    }

    private List<RoleVO> loadRoleDetails(Long userId) {
        List<SysUserRole> links = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        List<RoleVO> result = new ArrayList<>();
        for (SysUserRole link : links) {
            SysRole role = sysRoleMapper.selectById(link.getRoleId());
            if (role != null) {
                RoleVO vo = new RoleVO();
                vo.setId(role.getId());
                vo.setRoleCode(role.getRoleCode());
                vo.setRoleName(role.getRoleName());
                result.add(vo);
            }
        }
        return result;
    }

    private SysUserVO toVo(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setMustChangePassword(user.mustChangePassword());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        List<RoleVO> details = loadRoleDetails(user.getId());
        vo.setRoleDetails(details);
        vo.setRoles(details.stream().map(RoleVO::getRoleCode).collect(Collectors.toList()));
        return vo;
    }

    private UserSummaryVO toSummary(SysUser user) {
        UserSummaryVO vo = new UserSummaryVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
