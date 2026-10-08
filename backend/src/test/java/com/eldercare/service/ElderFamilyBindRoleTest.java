package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.dto.ElderFamilyCreateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.impl.ElderFamilyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElderFamilyBindRoleTest {

    @Mock private ElderFamilyMapper elderFamilyMapper;
    @Mock private ElderMapper elderMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private ElderFamilyServiceImpl elderFamilyService;

    @Test
    void bind_familyRole_ok() {
        Elder elder = new Elder();
        elder.setId(1L);
        SysUser user = new SysUser();
        user.setId(3L);
        when(elderMapper.selectById(1L)).thenReturn(elder);
        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(3L)).thenReturn(List.of("FAMILY"));
        when(elderFamilyMapper.selectAnyByElderAndUser(1L, 3L)).thenReturn(null);
        doAnswer(inv -> {
            ElderFamily b = inv.getArgument(0);
            b.setId(50L);
            return 1;
        }).when(elderFamilyMapper).insert(any(ElderFamily.class));

        ElderFamilyCreateDTO dto = new ElderFamilyCreateDTO();
        dto.setElderId(1L);
        dto.setFamilyUserId(3L);
        dto.setRelationship("子女");
        assertEquals(50L, elderFamilyService.bind(dto, null));
    }

    @Test
    void bind_careStaffRole_rejected() {
        Elder elder = new Elder();
        elder.setId(1L);
        SysUser user = new SysUser();
        user.setId(2L);
        when(elderMapper.selectById(1L)).thenReturn(elder);
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(2L)).thenReturn(List.of("CARE_STAFF"));

        ElderFamilyCreateDTO dto = new ElderFamilyCreateDTO();
        dto.setElderId(1L);
        dto.setFamilyUserId(2L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> elderFamilyService.bind(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(elderFamilyMapper, never()).insert(any(ElderFamily.class));
    }

    @Test
    void bind_adminRole_rejected() {
        Elder elder = new Elder();
        elder.setId(1L);
        SysUser user = new SysUser();
        user.setId(1L);
        when(elderMapper.selectById(1L)).thenReturn(elder);
        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(sysUserMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN"));

        ElderFamilyCreateDTO dto = new ElderFamilyCreateDTO();
        dto.setElderId(1L);
        dto.setFamilyUserId(1L);
        assertThrows(BusinessException.class, () -> elderFamilyService.bind(dto, null));
    }
}
