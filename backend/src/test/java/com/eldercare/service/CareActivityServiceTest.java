package com.eldercare.service;

import com.eldercare.common.ActivityStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareActivitySaveDTO;
import com.eldercare.entity.CareActivity;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareActivityMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareActivityServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareActivityServiceTest {

    @Mock private CareActivityMapper careActivityMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private CareActivityServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareActivitySaveDTO validDto() {
        CareActivitySaveDTO dto = new CareActivitySaveDTO();
        dto.setActivityName("老年健康操");
        dto.setActivityDate(LocalDate.of(2026, 10, 10));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(10, 0));
        dto.setLocation("一楼活动室");
        dto.setDescription("适合老年人的轻度运动");
        return dto;
    }

    @Test
    void createRejectsInvalidTime() {
        loginAdmin();
        CareActivitySaveDTO dto = validDto();
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(9, 0));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto, request));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void publishDuplicateConflicts() {
        loginAdmin();
        CareActivity row = new CareActivity();
        row.setId(1L);
        row.setStatus(ActivityStatuses.PUBLISHED);
        row.setActivityDate(LocalDate.of(2030, 1, 1));
        row.setEndTime(LocalTime.of(10, 0));
        when(careActivityMapper.selectById(1L)).thenReturn(row);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.publish(1L, request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void cancelDuplicateConflicts() {
        loginAdmin();
        CareActivity row = new CareActivity();
        row.setId(2L);
        row.setStatus(ActivityStatuses.CANCELLED);
        row.setActivityDate(LocalDate.of(2030, 1, 1));
        row.setEndTime(LocalTime.of(10, 0));
        when(careActivityMapper.selectById(2L)).thenReturn(row);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancel(2L, request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void deleteOnlyDraft() {
        loginAdmin();
        CareActivity row = new CareActivity();
        row.setId(3L);
        row.setStatus(ActivityStatuses.PUBLISHED);
        when(careActivityMapper.selectById(3L)).thenReturn(row);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(3L, request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createOk() {
        loginAdmin();
        when(careActivityMapper.insert(any(CareActivity.class))).thenAnswer(inv -> {
            CareActivity a = inv.getArgument(0);
            a.setId(99L);
            return 1;
        });
        Long id = service.create(validDto(), request);
        assertEquals(99L, id);
        verify(operationLogService).record(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void completedCannotUpdate() {
        loginAdmin();
        CareActivity row = new CareActivity();
        row.setId(4L);
        row.setStatus(ActivityStatuses.COMPLETED);
        row.setActivityDate(LocalDate.of(2020, 1, 1));
        row.setEndTime(LocalTime.of(10, 0));
        when(careActivityMapper.selectById(4L)).thenReturn(row);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(4L, validDto(), request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }
}
