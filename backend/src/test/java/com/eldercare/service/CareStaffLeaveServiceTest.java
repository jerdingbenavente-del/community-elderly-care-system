package com.eldercare.service;

import com.eldercare.common.CareLeaveStatuses;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffLeaveCreateDTO;
import com.eldercare.dto.CareStaffLeaveReviewDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffLeaveApplication;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffLeaveApplicationMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareStaffLeaveServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareStaffLeaveServiceTest {

    @Mock
    private CareStaffLeaveApplicationMapper leaveMapper;
    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private CareServiceOrderMapper careServiceOrderMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private CareStaffIdentityService careStaffIdentityService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CareStaffLeaveServiceImpl careStaffLeaveService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String username, List<String> roles) {
        LoginUser user = new LoginUser(userId, username, "x", true, roles, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareStaff staff(Long id, Long userId) {
        CareStaff s = new CareStaff();
        s.setId(id);
        s.setUserId(userId);
        s.setName("护理员一号");
        s.setEmployeeNo("CS001");
        s.setStatus(1);
        return s;
    }

    @Test
    void create_rejectsInvalidTimeRange() {
        login(2L, "care01", List.of("CARE_STAFF"));
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff(1L, 2L));
        CareStaffLeaveCreateDTO dto = new CareStaffLeaveCreateDTO();
        dto.setStartTime(LocalDateTime.of(2026, 10, 20, 12, 0));
        dto.setEndTime(LocalDateTime.of(2026, 10, 20, 9, 0));
        dto.setReason("有事");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffLeaveService.create(dto, request));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void create_success() {
        login(2L, "care01", List.of("CARE_STAFF"));
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff(1L, 2L));
        when(leaveMapper.selectList(any())).thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            CareStaffLeaveApplication a = inv.getArgument(0);
            a.setId(10L);
            return 1;
        }).when(leaveMapper).insert(any(CareStaffLeaveApplication.class));

        CareStaffLeaveCreateDTO dto = new CareStaffLeaveCreateDTO();
        dto.setStartTime(LocalDateTime.of(2026, 10, 20, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 10, 20, 12, 0));
        dto.setReason("临时有事");
        Long id = careStaffLeaveService.create(dto, request);
        assertEquals(10L, id);
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approve_releasesConfirmedFutureOrders() {
        login(1L, "admin", List.of("ADMIN"));
        CareStaffLeaveApplication leave = new CareStaffLeaveApplication();
        leave.setId(5L);
        leave.setCareStaffId(1L);
        leave.setStartTime(LocalDateTime.of(2026, 10, 20, 9, 0));
        leave.setEndTime(LocalDateTime.of(2026, 10, 20, 12, 0));
        leave.setStatus(CareLeaveStatuses.PENDING);
        when(leaveMapper.selectById(5L)).thenReturn(leave);
        when(leaveMapper.update(isNull(), any())).thenReturn(1);

        CareServiceOrder future = new CareServiceOrder();
        future.setId(100L);
        future.setCareStaffId(1L);
        future.setStatus(CareOrderStatuses.CONFIRMED);
        future.setScheduledStartTime(LocalDateTime.of(2026, 10, 20, 10, 0));
        future.setScheduledEndTime(LocalDateTime.of(2026, 10, 20, 11, 0));
        when(careServiceOrderMapper.selectList(any())).thenReturn(List.of(future));
        when(careServiceOrderMapper.update(isNull(), any())).thenReturn(1);

        careStaffLeaveService.approve(5L, new CareStaffLeaveReviewDTO(), request);

        // leave status update + order release update + affected_order_count update
        verify(careServiceOrderMapper).update(isNull(), any());
    }

    @Test
    void approve_skipsInServiceAndPastConfirmed() {
        login(1L, "admin", List.of("ADMIN"));
        CareStaffLeaveApplication leave = new CareStaffLeaveApplication();
        leave.setId(5L);
        leave.setCareStaffId(1L);
        leave.setStartTime(LocalDateTime.now().minusHours(2));
        leave.setEndTime(LocalDateTime.now().plusHours(2));
        leave.setStatus(CareLeaveStatuses.PENDING);
        when(leaveMapper.selectById(5L)).thenReturn(leave);
        when(leaveMapper.update(isNull(), any())).thenReturn(1);

        CareServiceOrder past = new CareServiceOrder();
        past.setId(101L);
        past.setCareStaffId(1L);
        past.setStatus(CareOrderStatuses.CONFIRMED);
        past.setScheduledStartTime(LocalDateTime.now().minusMinutes(30));
        past.setScheduledEndTime(LocalDateTime.now().plusMinutes(30));
        when(careServiceOrderMapper.selectList(any())).thenReturn(List.of(past));

        careStaffLeaveService.approve(5L, null, request);
        verify(careServiceOrderMapper, never()).updateById(any(CareServiceOrder.class));
    }

    @Test
    void approve_duplicateReturns409() {
        login(1L, "admin", List.of("ADMIN"));
        CareStaffLeaveApplication leave = new CareStaffLeaveApplication();
        leave.setId(5L);
        leave.setStatus(CareLeaveStatuses.PENDING);
        when(leaveMapper.selectById(5L)).thenReturn(leave);
        when(leaveMapper.update(isNull(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffLeaveService.approve(5L, null, request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void staffCannotApprove() {
        login(2L, "care01", List.of("CARE_STAFF"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffLeaveService.approve(5L, null, request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void getById_staffCannotViewOthers() {
        login(2L, "care01", List.of("CARE_STAFF"));
        CareStaffLeaveApplication leave = new CareStaffLeaveApplication();
        leave.setId(5L);
        leave.setCareStaffId(99L);
        leave.setStatus(CareLeaveStatuses.PENDING);
        when(leaveMapper.selectById(5L)).thenReturn(leave);
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff(1L, 2L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careStaffLeaveService.getById(5L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
