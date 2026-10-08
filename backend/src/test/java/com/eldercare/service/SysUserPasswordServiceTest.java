package com.eldercare.service;

import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.dto.ChangePasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserPasswordServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserRoleMapper sysUserRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminProtectionService adminProtectionService;
    @Mock private UserBusinessIdentityService userBusinessIdentityService;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long id, String username, String... roles) {
        LoginUser user = new LoginUser(id, username, "x", true, List.of(roles), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void changePassword_success_clearsMustChangeFlag() {
        loginAs(10L, "wxl@48219", "FAMILY");
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(UserStatuses.ENABLED);
        user.setPasswordHash("OLD_HASH");
        user.setMustChangePassword(true);
        when(sysUserMapper.selectById(10L)).thenReturn(user);
        when(passwordEncoder.matches("123456", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.matches("abc123", "OLD_HASH")).thenReturn(false);
        when(passwordEncoder.encode("abc123")).thenReturn("NEW_HASH");

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("abc123");
        dto.setConfirmPassword("abc123");
        sysUserService.changePassword(dto, null);

        ArgumentCaptor<SysUser> cap = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(cap.capture());
        assertEquals("NEW_HASH", cap.getValue().getPasswordHash());
        assertFalse(cap.getValue().mustChangePassword());
    }

    @Test
    void changePassword_wrongOld() {
        loginAs(10L, "u", "FAMILY");
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(UserStatuses.ENABLED);
        user.setPasswordHash("OLD_HASH");
        when(sysUserMapper.selectById(10L)).thenReturn(user);
        when(passwordEncoder.matches("bad", "OLD_HASH")).thenReturn(false);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("bad");
        dto.setNewPassword("abc123");
        dto.setConfirmPassword("abc123");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void changePassword_confirmMismatch() {
        loginAs(10L, "u", "FAMILY");
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(UserStatuses.ENABLED);
        user.setPasswordHash("OLD_HASH");
        when(sysUserMapper.selectById(10L)).thenReturn(user);
        when(passwordEncoder.matches("123456", "OLD_HASH")).thenReturn(true);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("abc123");
        dto.setConfirmPassword("abc124");
        assertThrows(BusinessException.class, () -> sysUserService.changePassword(dto, null));
    }

    @Test
    void changePassword_pureDigitsRejected() {
        loginAs(10L, "u", "FAMILY");
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(UserStatuses.ENABLED);
        user.setPasswordHash("OLD_HASH");
        when(sysUserMapper.selectById(10L)).thenReturn(user);
        when(passwordEncoder.matches("123456", "OLD_HASH")).thenReturn(true);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("654321");
        dto.setConfirmPassword("654321");
        assertThrows(BusinessException.class, () -> sysUserService.changePassword(dto, null));
    }

    @Test
    void changePassword_disabledRejected() {
        loginAs(10L, "u", "FAMILY");
        SysUser user = new SysUser();
        user.setId(10L);
        user.setStatus(UserStatuses.DISABLED);
        user.setPasswordHash("OLD_HASH");
        when(sysUserMapper.selectById(10L)).thenReturn(user);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123456");
        dto.setNewPassword("abc123");
        dto.setConfirmPassword("abc123");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void resetPassword_setsInitialAndMustChange() {
        loginAs(1L, "admin", "ADMIN");
        SysUser target = new SysUser();
        target.setId(10L);
        target.setStatus(UserStatuses.ENABLED);
        target.setPasswordHash("OLD");
        target.setMustChangePassword(false);
        when(sysUserMapper.selectById(10L)).thenReturn(target);
        when(passwordEncoder.encode(AccountSecurityConstants.INITIAL_PASSWORD)).thenReturn("RESET_HASH");

        sysUserService.resetPassword(10L, null);

        ArgumentCaptor<SysUser> cap = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(cap.capture());
        assertEquals("RESET_HASH", cap.getValue().getPasswordHash());
        assertTrue(cap.getValue().mustChangePassword());
        verify(dataPermissionService).denyFamilyOnAdminApi();
    }

    @Test
    void resetPassword_cannotResetSelf() {
        loginAs(1L, "admin", "ADMIN");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.resetPassword(1L, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }
}
