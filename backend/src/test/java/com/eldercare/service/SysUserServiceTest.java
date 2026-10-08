package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.config.UploadProperties;
import com.eldercare.dto.SysUserCreateDTO;
import com.eldercare.dto.SysUserUpdateDTO;
import com.eldercare.dto.UserRoleUpdateDTO;
import com.eldercare.entity.SysRole;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysRoleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.SysUserRoleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserRoleMapper sysUserRoleMapper;
    @Mock private CareStaffMapper careStaffMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminProtectionService adminProtectionService;
    @Mock private UserBusinessIdentityService userBusinessIdentityService;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private OperationLogService operationLogService;
    @Mock private UploadProperties uploadProperties;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"),
                List.of("system:user:add", "system:user:list", "system:user:update",
                        "system:user:delete", "system:user:enable", "system:user:disable", "system:user:role"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private SysRole role(Long id, String code) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleCode(code);
        r.setRoleName(code);
        r.setStatus(1);
        return r;
    }

    @Test
    void create_ok_encryptPassword() {
        loginAdmin();
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectById(2L)).thenReturn(role(2L, "CARE_STAFF"));
        when(passwordEncoder.encode("Care@123")).thenReturn("ENCODED");
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(10L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        SysUserCreateDTO dto = new SysUserCreateDTO();
        dto.setUsername("care_new");
        dto.setPassword("Care@123");
        dto.setRealName("新护理");
        dto.setRoleIds(List.of(2L));

        Long id = sysUserService.create(dto, null);
        assertEquals(10L, id);
        ArgumentCaptor<SysUser> cap = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(cap.capture());
        assertEquals("ENCODED", cap.getValue().getPasswordHash());
        assertNotEquals("Care@123", cap.getValue().getPasswordHash());
        assertEquals(UserStatuses.ENABLED, cap.getValue().getStatus());
    }

    @Test
    void create_duplicateUsername() {
        loginAdmin();
        when(sysUserMapper.selectCount(any())).thenReturn(1L);
        SysUserCreateDTO dto = new SysUserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("Admin@123");
        dto.setRoleIds(List.of(1L));
        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_roleNotFound() {
        loginAdmin();
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectById(99L)).thenReturn(null);
        SysUserCreateDTO dto = new SysUserCreateDTO();
        dto.setUsername("u1");
        dto.setPassword("Pass@123");
        dto.setRoleIds(List.of(99L));
        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.create(dto, null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void update_profileOnly() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("plain");
        user.setStatus(1);
        when(sysUserMapper.selectById(5L)).thenReturn(user);
        SysUserUpdateDTO dto = new SysUserUpdateDTO();
        dto.setRealName("新名");
        dto.setPhone("13900000000");
        sysUserService.update(5L, dto, null);
        assertEquals("plain", user.getUsername());
        assertEquals("新名", user.getRealName());
        verify(sysUserMapper).updateById(user);
    }

    @Test
    void me_ok() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setStatus(1);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());
        assertEquals("admin", sysUserService.me().getUsername());
    }

    @Test
    void updateRoles_callsProtections() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(2L);
        user.setStatus(1);
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        when(sysRoleMapper.selectById(2L)).thenReturn(role(2L, "CARE_STAFF"));

        UserRoleUpdateDTO dto = new UserRoleUpdateDTO();
        dto.setRoleIds(List.of(2L));
        sysUserService.updateRoles(2L, dto, null);

        verify(adminProtectionService).assertCanRemoveAdminRole(2L, false);
        verify(userBusinessIdentityService).assertRolesKeepBusinessRequirements(any(), any());
        verify(sysUserRoleMapper).delete(any());
    }

    @Test
    void disable_protected() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(2L);
        user.setStatus(1);
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        doThrow(new BusinessException(ResultCode.CONFLICT, "绑定护理员"))
                .when(userBusinessIdentityService).assertCanDisableOrDelete(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.disable(2L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void delete_ok() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(9L);
        user.setStatus(1);
        when(sysUserMapper.selectById(9L)).thenReturn(user);

        sysUserService.delete(9L, null);
        verify(adminProtectionService).assertCanDeleteUser(9L);
        verify(userBusinessIdentityService).assertCanDisableOrDelete(9L);
        verify(sysUserMapper).deleteById(9L);
    }

    @Test
    void delete_cannotDeleteSelf() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(1);
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.delete(1L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(sysUserMapper, never()).deleteById(1L);
    }

    @Test
    void enable_ok() {
        loginAdmin();
        SysUser user = new SysUser();
        user.setId(9L);
        user.setStatus(0);
        when(sysUserMapper.selectById(9L)).thenReturn(user);
        sysUserService.enable(9L, null);
        assertEquals(UserStatuses.ENABLED, user.getStatus());
    }
}
