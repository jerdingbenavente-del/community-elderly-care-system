package com.eldercare.service;

import com.eldercare.common.CareScheduleStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffScheduleCreateDTO;
import com.eldercare.dto.CareStaffScheduleQueryDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffSchedule;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.CareStaffScheduleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareStaffScheduleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareStaffScheduleServiceTest {

    @Mock
    private CareStaffScheduleMapper careStaffScheduleMapper;
    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private CareStaffIdentityService careStaffIdentityService;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private CareStaffScheduleServiceImpl careStaffScheduleService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String username, List<String> roles, List<String> perms) {
        LoginUser user = new LoginUser(userId, username, "x", true, roles, perms);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareStaff activeStaff(Long id) {
        CareStaff staff = new CareStaff();
        staff.setId(id);
        staff.setStatus(1);
        staff.setName("护理员");
        return staff;
    }

    @Test
    void create_ok() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        when(careStaffMapper.selectById(1L)).thenReturn(activeStaff(1L));
        when(careStaffScheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            CareStaffSchedule s = inv.getArgument(0);
            s.setId(10L);
            return 1;
        }).when(careStaffScheduleMapper).insert(any(CareStaffSchedule.class));

        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(12, 0));

        Long id = careStaffScheduleService.create(dto, null);
        assertEquals(10L, id);
        ArgumentCaptor<CareStaffSchedule> cap = ArgumentCaptor.forClass(CareStaffSchedule.class);
        verify(careStaffScheduleMapper).insert(cap.capture());
        assertEquals(CareScheduleStatuses.AVAILABLE, cap.getValue().getStatus());
    }

    @Test
    void create_staffNotFound() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        when(careStaffMapper.selectById(9L)).thenReturn(null);
        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(9L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(12, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.create(dto, null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void create_staffDisabled() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        CareStaff staff = activeStaff(1L);
        staff.setStatus(0);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(12, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_invalidTimeRange() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        when(careStaffMapper.selectById(1L)).thenReturn(activeStaff(1L));
        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(12, 0));
        dto.setEndTime(LocalTime.of(9, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.create(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void create_overlapReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        when(careStaffMapper.selectById(1L)).thenReturn(activeStaff(1L));
        CareStaffSchedule existing = new CareStaffSchedule();
        existing.setId(1L);
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(CareScheduleStatuses.AVAILABLE);
        when(careStaffScheduleMapper.selectList(any())).thenReturn(List.of(existing));

        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(11, 0));
        dto.setEndTime(LocalTime.of(14, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_adjacentOk() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:schedule:add"));
        when(careStaffMapper.selectById(1L)).thenReturn(activeStaff(1L));
        CareStaffSchedule existing = new CareStaffSchedule();
        existing.setId(1L);
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setStatus(CareScheduleStatuses.AVAILABLE);
        when(careStaffScheduleMapper.selectList(any())).thenReturn(List.of(existing));
        doAnswer(inv -> {
            CareStaffSchedule s = inv.getArgument(0);
            s.setId(2L);
            return 1;
        }).when(careStaffScheduleMapper).insert(any(CareStaffSchedule.class));

        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(12, 0));
        dto.setEndTime(LocalTime.of(15, 0));

        assertEquals(2L, careStaffScheduleService.create(dto, null));
    }

    @Test
    void create_careStaffForbidden() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:schedule:list"));
        CareStaffScheduleCreateDTO dto = new CareStaffScheduleCreateDTO();
        dto.setCareStaffId(1L);
        dto.setScheduleDate(LocalDate.of(2026, 9, 20));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(12, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.create(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void page_careStaffCannotQueryOther() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:schedule:list"));
        CareStaff self = activeStaff(1L);
        self.setUserId(2L);
        when(careStaffIdentityService.requireByUserId(2L)).thenReturn(self);

        CareStaffScheduleQueryDTO query = new CareStaffScheduleQueryDTO();
        query.setCareStaffId(999L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.page(query));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void page_familyDenied() {
        login(3L, "family01", List.of("FAMILY"), List.of());
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "家属请使用家属端接口访问老人数据"))
                .when(dataPermissionService).denyFamilyOnAdminApi();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.page(new CareStaffScheduleQueryDTO()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void assertScheduleCovers_ok() {
        CareStaffSchedule s = new CareStaffSchedule();
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(12, 0));
        s.setStatus(CareScheduleStatuses.AVAILABLE);
        when(careStaffScheduleMapper.selectList(any())).thenReturn(List.of(s));

        assertDoesNotThrow(() -> careStaffScheduleService.assertScheduleCovers(
                1L,
                LocalDateTime.of(2026, 9, 20, 10, 0),
                LocalDateTime.of(2026, 9, 20, 11, 0)));
    }

    @Test
    void assertScheduleCovers_notCovered() {
        CareStaffSchedule s = new CareStaffSchedule();
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(12, 0));
        s.setStatus(CareScheduleStatuses.AVAILABLE);
        when(careStaffScheduleMapper.selectList(any())).thenReturn(List.of(s));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.assertScheduleCovers(
                        1L,
                        LocalDateTime.of(2026, 9, 20, 11, 30),
                        LocalDateTime.of(2026, 9, 20, 12, 30)));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void assertScheduleCovers_noSchedule() {
        when(careStaffScheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffScheduleService.assertScheduleCovers(
                        1L,
                        LocalDateTime.of(2026, 9, 20, 10, 0),
                        LocalDateTime.of(2026, 9, 20, 11, 0)));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }
}
