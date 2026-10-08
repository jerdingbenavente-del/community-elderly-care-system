package com.eldercare.service;

import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareStaffIdentityServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareStaffIdentityServiceTest {

    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private CareStaffIdentityServiceImpl careStaffIdentityService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentCareStaff_ok() {
        LoginUser user = new LoginUser(2L, "care01", "x", true, List.of("CARE_STAFF"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(CareStaffStatuses.ENABLED);
        when(careStaffMapper.selectOne(any())).thenReturn(staff);

        assertEquals(1L, careStaffIdentityService.requireCurrentCareStaff().getId());
    }

    @Test
    void requireCurrentCareStaff_notStaff() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(careStaffMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffIdentityService.requireCurrentCareStaff());
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void validateUserBinding_ok() {
        SysUser user = new SysUser();
        user.setId(8L);
        user.setStatus(1);
        when(sysUserMapper.selectById(8L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(8L)).thenReturn(List.of("CARE_STAFF"));
        when(careStaffMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> careStaffIdentityService.validateUserBinding(8L, null));
    }

    @Test
    void validateUserBinding_noRole() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setStatus(1);
        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(3L)).thenReturn(List.of("FAMILY"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffIdentityService.validateUserBinding(3L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void validateUserBinding_alreadyBound() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setStatus(1);
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(2L)).thenReturn(List.of("CARE_STAFF"));
        when(careStaffMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffIdentityService.validateUserBinding(2L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void requireEnabledCurrent_disabled() {
        LoginUser user = new LoginUser(2L, "care01", "x", true, List.of("CARE_STAFF"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(CareStaffStatuses.DISABLED);
        when(careStaffMapper.selectOne(any())).thenReturn(staff);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffIdentityService.requireEnabledCurrentCareStaff());
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }
}
