package com.eldercare.service;

import com.eldercare.common.CareAttendanceStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffAttendanceQueryDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffAttendance;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffAttendanceMapper;
import com.eldercare.mapper.CareStaffLeaveApplicationMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareStaffAttendanceServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareStaffAttendanceServiceTest {

    @Mock
    private CareStaffAttendanceMapper attendanceMapper;
    @Mock
    private CareStaffLeaveApplicationMapper leaveMapper;
    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private CareStaffIdentityService careStaffIdentityService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CareStaffAttendanceServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareStaff staff() {
        CareStaff s = new CareStaff();
        s.setId(1L);
        s.setUserId(2L);
        s.setName("护理员一号");
        s.setEmployeeNo("CS001");
        s.setStatus(1);
        return s;
    }

    @Test
    void today_withoutRecord_isNotChecked() {
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff());
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        when(leaveMapper.selectCount(any())).thenReturn(0L);
        var vo = service.today();
        assertEquals(CareAttendanceStatuses.NOT_CHECKED, vo.getStatus());
        assertEquals(LocalDate.now(), vo.getAttendanceDate());
        assertNull(vo.getCheckInTime());
    }

    @Test
    void checkIn_success_usesServerTime() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        when(leaveMapper.selectCount(any())).thenReturn(0L);
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        LocalDateTime before = LocalDateTime.now().minusSeconds(2);
        service.checkIn(request);
        ArgumentCaptor<CareStaffAttendance> cap = ArgumentCaptor.forClass(CareStaffAttendance.class);
        verify(attendanceMapper).insert(cap.capture());
        CareStaffAttendance saved = cap.getValue();
        assertEquals(CareAttendanceStatuses.WORKING, saved.getStatus());
        assertEquals(1L, saved.getCareStaffId());
        assertEquals(LocalDate.now(), saved.getAttendanceDate());
        assertNotNull(saved.getCheckInTime());
        assertTrue(!saved.getCheckInTime().isBefore(before));
        assertNull(saved.getCheckOutTime());
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkIn_duplicate_409() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        when(leaveMapper.selectCount(any())).thenReturn(0L);
        CareStaffAttendance existing = new CareStaffAttendance();
        existing.setStatus(CareAttendanceStatuses.WORKING);
        when(attendanceMapper.selectOne(any())).thenReturn(existing);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkIn(request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(attendanceMapper, never()).insert(any(CareStaffAttendance.class));
    }

    @Test
    void checkIn_uniqueConflict_409() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        when(leaveMapper.selectCount(any())).thenReturn(0L);
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        when(attendanceMapper.insert(any(CareStaffAttendance.class))).thenThrow(new DuplicateKeyException("dup"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkIn(request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void checkOut_withoutCheckIn_409() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkOut(request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(attendanceMapper, never()).update(any(), any());
    }

    @Test
    void checkOut_success() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        CareStaffAttendance existing = new CareStaffAttendance();
        existing.setId(8L);
        existing.setCareStaffId(1L);
        existing.setStatus(CareAttendanceStatuses.WORKING);
        when(attendanceMapper.selectOne(any())).thenReturn(existing);
        when(attendanceMapper.update(any(), any())).thenReturn(1);
        service.checkOut(request);
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkOut_repeat_409() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        CareStaffAttendance existing = new CareStaffAttendance();
        existing.setStatus(CareAttendanceStatuses.COMPLETED);
        when(attendanceMapper.selectOne(any())).thenReturn(existing);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkOut(request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void checkIn_onApprovedLeave_409() {
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff());
        when(leaveMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkIn(request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(attendanceMapper, never()).insert(any(CareStaffAttendance.class));
    }

    @Test
    void today_onApprovedLeave_showsLeave() {
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff());
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        when(leaveMapper.selectCount(any())).thenReturn(1L);
        assertEquals(CareAttendanceStatuses.LEAVE, service.today().getStatus());
    }

    @Test
    void pageMine_scopesToSelf() {
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff());
        when(attendanceMapper.selectPage(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        CareStaffAttendanceQueryDTO query = new CareStaffAttendanceQueryDTO();
        query.setCareStaffId(99L);
        service.pageMine(query);
        verify(attendanceMapper).selectPage(any(), any());
    }

    @Test
    void pageAdmin_forbiddenForNonAdmin() {
        LoginUser user = new LoginUser(2L, "care01", "x", true, List.of("CARE_STAFF"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.pageAdmin(new CareStaffAttendanceQueryDTO()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void pageAdmin_allowsAdmin() {
        loginAdmin();
        when(attendanceMapper.selectPage(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        service.pageAdmin(new CareStaffAttendanceQueryDTO());
        verify(attendanceMapper).selectPage(any(), any());
    }
}
