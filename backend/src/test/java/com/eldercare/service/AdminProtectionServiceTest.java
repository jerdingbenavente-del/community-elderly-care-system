package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.impl.AdminProtectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProtectionServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private AdminProtectionServiceImpl adminProtectionService;

    @Test
    void removeLastAdminRole_shouldReject() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(UserStatuses.ENABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.countAdminRoleOfUser(1L)).thenReturn(1L);
        when(sysUserMapper.countEnabledAdmins()).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminProtectionService.assertCanRemoveAdminRole(1L, false));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void removeAdminWhenTwoExist_ok() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(UserStatuses.ENABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.countAdminRoleOfUser(1L)).thenReturn(1L);
        when(sysUserMapper.countEnabledAdmins()).thenReturn(2L);
        assertDoesNotThrow(() -> adminProtectionService.assertCanRemoveAdminRole(1L, false));
    }

    @Test
    void disableLastAdmin_shouldReject() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(UserStatuses.ENABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.countAdminRoleOfUser(1L)).thenReturn(1L);
        when(sysUserMapper.countEnabledAdmins()).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminProtectionService.assertCanDisableUser(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void deleteLastAdmin_shouldReject() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(UserStatuses.ENABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.countAdminRoleOfUser(1L)).thenReturn(1L);
        when(sysUserMapper.countEnabledAdmins()).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminProtectionService.assertCanDeleteUser(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void disabledUserNotCountedAsEnabledAdmin() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setStatus(UserStatuses.DISABLED);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        assertDoesNotThrow(() -> adminProtectionService.assertCanRemoveAdminRole(1L, false));
    }
}
