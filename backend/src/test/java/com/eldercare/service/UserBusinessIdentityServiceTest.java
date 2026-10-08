package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.service.impl.UserBusinessIdentityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBusinessIdentityServiceTest {

    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private ElderFamilyMapper elderFamilyMapper;

    @InjectMocks
    private UserBusinessIdentityServiceImpl userBusinessIdentityService;

    @Test
    void careStaffBound_cannotDisable() {
        when(careStaffMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userBusinessIdentityService.assertCanDisableOrDelete(2L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void familyBound_cannotDisable() {
        when(careStaffMapper.selectCount(any())).thenReturn(0L);
        when(elderFamilyMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userBusinessIdentityService.assertCanDisableOrDelete(3L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void careStaffMustKeepRole() {
        when(careStaffMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userBusinessIdentityService.assertRolesKeepBusinessRequirements(2L, Set.of("FAMILY")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void familyMustKeepRole() {
        when(careStaffMapper.selectCount(any())).thenReturn(0L);
        when(elderFamilyMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userBusinessIdentityService.assertRolesKeepBusinessRequirements(3L, List.of("ADMIN")));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void unbound_ok() {
        when(careStaffMapper.selectCount(any())).thenReturn(0L);
        when(elderFamilyMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> userBusinessIdentityService.assertCanDisableOrDelete(9L));
        assertDoesNotThrow(() -> userBusinessIdentityService.assertRolesKeepBusinessRequirements(9L, Set.of("ADMIN")));
    }
}
