package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.BusinessAccountCreateDTO;
import com.eldercare.dto.ChangePasswordDTO;
import com.eldercare.dto.SysUserCreateDTO;
import com.eldercare.dto.SysUserProfileUpdateDTO;
import com.eldercare.dto.SysUserQueryDTO;
import com.eldercare.dto.SysUserUpdateDTO;
import com.eldercare.dto.UserRoleUpdateDTO;
import com.eldercare.service.BusinessAccountService;
import com.eldercare.service.SysUserService;
import com.eldercare.vo.BusinessAccountVO;
import com.eldercare.vo.RoleVO;
import com.eldercare.vo.SysUserVO;
import com.eldercare.vo.UserSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 系统用户管理：复用既有 /api/system/users 路径，不另建 /api/sys-users。
 */
@RestController
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserService sysUserService;
    private final BusinessAccountService businessAccountService;

    public SysUserController(SysUserService sysUserService,
                             BusinessAccountService businessAccountService) {
        this.sysUserService = sysUserService;
        this.businessAccountService = businessAccountService;
    }

    /** 兼容旧只读列表 */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<List<UserSummaryVO>> listAll() {
        return Result.success(sysUserService.listUsers());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<SysUserVO>> page(@Valid SysUserQueryDTO query) {
        return Result.success(sysUserService.page(query));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<SysUserVO> me() {
        return Result.success(sysUserService.me());
    }

    /** 当前用户修改自己的姓名/手机号（不要求 system:user:update） */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<SysUserVO> updateMe(@Valid @RequestBody SysUserProfileUpdateDTO dto,
                                      HttpServletRequest request) {
        return Result.success(sysUserService.updateMe(dto, request));
    }

    /** 当前用户上传头像 */
    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public Result<SysUserVO> uploadMyAvatar(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        return Result.success(sysUserService.uploadMyAvatar(file, request));
    }

    /**
     * P8 业务开户：仅 FAMILY / CARE_STAFF；后端自动生成用户名与初始密码，不接受前端传入。
     */
    @PostMapping("/business-accounts")
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<BusinessAccountVO> createBusinessAccount(@Valid @RequestBody BusinessAccountCreateDTO dto,
                                                           HttpServletRequest request) {
        return Result.success(businessAccountService.create(dto, request));
    }

    /** 当前登录用户修改密码（含首次强制改密） */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        sysUserService.changePassword(dto, request);
        return Result.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<SysUserVO> detail(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Long> create(@Valid @RequestBody SysUserCreateDTO dto, HttpServletRequest request) {
        return Result.success(sysUserService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody SysUserUpdateDTO dto,
                               HttpServletRequest request) {
        sysUserService.update(id, dto, request);
        return Result.success();
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('system:user:enable')")
    public Result<Void> enable(@PathVariable Long id, HttpServletRequest request) {
        sysUserService.enable(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:user:disable')")
    public Result<Void> disable(@PathVariable Long id, HttpServletRequest request) {
        sysUserService.disable(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        sysUserService.resetPassword(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        sysUserService.delete(id, request);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:role')")
    public Result<List<RoleVO>> roles(@PathVariable Long id) {
        return Result.success(sysUserService.listRoles(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:role')")
    public Result<Void> updateRoles(@PathVariable Long id,
                                    @Valid @RequestBody UserRoleUpdateDTO dto,
                                    HttpServletRequest request) {
        sysUserService.updateRoles(id, dto, request);
        return Result.success();
    }
}
