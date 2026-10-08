package com.eldercare.service;

import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffCreateDTO;
import com.eldercare.dto.CareStaffUpdateDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.CareStaffScheduleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareStaffServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareStaffServiceTest {

    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private CareServiceOrderMapper careServiceOrderMapper;
    @Mock
    private CareStaffScheduleMapper careStaffScheduleMapper;
    @Mock
    private CareStaffIdentityService careStaffIdentityService;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private CareStaffServiceImpl careStaffService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String username, List<String> roles, List<String> perms) {
        LoginUser user = new LoginUser(userId, username, "x", true, roles, perms);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void create_ok() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:add"));
        when(careStaffMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            CareStaff s = inv.getArgument(0);
            s.setId(10L);
            return 1;
        }).when(careStaffMapper).insert(any(CareStaff.class));

        CareStaffCreateDTO dto = new CareStaffCreateDTO();
        dto.setUserId(8L);
        dto.setEmployeeNo("CS010");
        dto.setName("新护理员");
        dto.setPhone("13900001111");

        Long id = careStaffService.create(dto, null);
        assertEquals(10L, id);
        verify(careStaffIdentityService).validateUserBinding(8L, null);
        ArgumentCaptor<CareStaff> cap = ArgumentCaptor.forClass(CareStaff.class);
        verify(careStaffMapper).insert(cap.capture());
        assertEquals(CareStaffStatuses.ENABLED, cap.getValue().getStatus());
        assertEquals(8L, cap.getValue().getUserId());
    }

    @Test
    void create_userBindingFail() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:add"));
        doThrow(new BusinessException(ResultCode.CONFLICT, "系统用户已经绑定护理员"))
                .when(careStaffIdentityService).validateUserBinding(2L, null);

        CareStaffCreateDTO dto = new CareStaffCreateDTO();
        dto.setUserId(2L);
        dto.setEmployeeNo("CS099");
        dto.setName("重复");

        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(careStaffMapper, never()).insert(any(CareStaff.class));
    }

    @Test
    void create_employeeNoDuplicate() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:add"));
        when(careStaffMapper.selectCount(any())).thenReturn(1L);
        CareStaffCreateDTO dto = new CareStaffCreateDTO();
        dto.setUserId(8L);
        dto.setEmployeeNo("CS001");
        dto.setName("重复工号");

        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void update_onlyProfileFields() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:update"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(CareStaffStatuses.ENABLED);
        staff.setEmployeeNo("CS001");
        when(careStaffMapper.selectById(1L)).thenReturn(staff);

        CareStaffUpdateDTO dto = new CareStaffUpdateDTO();
        dto.setName("改名");
        dto.setPhone("13800009999");
        dto.setPosition("高级护理");
        careStaffService.update(1L, dto, null);

        assertEquals("改名", staff.getName());
        assertEquals(2L, staff.getUserId());
        assertEquals(CareStaffStatuses.ENABLED, staff.getStatus());
        verify(careStaffMapper).updateById(staff);
    }

    @Test
    void me_ok() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care_staff:view"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setName("照护员一号");
        staff.setEmployeeNo("CS001");
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff);
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("care01");
        when(sysUserMapper.selectById(2L)).thenReturn(user);

        assertEquals("care01", careStaffService.me().getUsername());
        assertEquals(1L, careStaffService.me().getId());
    }

    @Test
    void getById_otherStaffForbidden() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care_staff:view"));
        CareStaff target = new CareStaff();
        target.setId(99L);
        target.setUserId(9L);
        when(careStaffMapper.selectById(99L)).thenReturn(target);
        CareStaff self = new CareStaff();
        self.setId(1L);
        when(careStaffIdentityService.requireByUserId(2L)).thenReturn(self);

        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.getById(99L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void enable_ok() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:enable"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(CareStaffStatuses.DISABLED);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);

        careStaffService.enable(1L, null);
        assertEquals(CareStaffStatuses.ENABLED, staff.getStatus());
        verify(careStaffIdentityService).validateUserBinding(2L, 1L);
    }

    @Test
    void disable_withConfirmed_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:disable"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(CareStaffStatuses.ENABLED);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        when(careServiceOrderMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.disable(1L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void disable_ok() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:disable"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(CareStaffStatuses.ENABLED);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        when(careServiceOrderMapper.selectCount(any())).thenReturn(0L);

        careStaffService.disable(1L, null);
        assertEquals(CareStaffStatuses.DISABLED, staff.getStatus());
    }

    @Test
    void delete_withOrders_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:delete"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        when(careServiceOrderMapper.selectCount(any())).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.delete(1L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void delete_ok_keepsSysUser() {
        login(1L, "admin", List.of("ADMIN"), List.of("care_staff:delete"));
        CareStaff staff = new CareStaff();
        staff.setId(3L);
        staff.setUserId(9L);
        when(careStaffMapper.selectById(3L)).thenReturn(staff);
        when(careServiceOrderMapper.selectCount(any())).thenReturn(0L);
        when(careStaffScheduleMapper.selectCount(any())).thenReturn(0L);

        careStaffService.delete(3L, null);
        verify(careStaffMapper).deleteById(3L);
        verify(sysUserMapper, never()).deleteById(9L);
    }

    @Test
    void family_deniedOnCreate() {
        login(3L, "family01", List.of("FAMILY"), List.of());
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "家属请使用家属端接口访问老人数据"))
                .when(dataPermissionService).denyFamilyOnAdminApi();
        CareStaffCreateDTO dto = new CareStaffCreateDTO();
        dto.setUserId(1L);
        dto.setEmployeeNo("X");
        dto.setName("X");
        BusinessException ex = assertThrows(BusinessException.class, () -> careStaffService.create(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
