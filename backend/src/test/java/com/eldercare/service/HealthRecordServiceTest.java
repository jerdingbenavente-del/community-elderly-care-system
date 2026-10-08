package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.dto.HealthRecordCreateDTO;
import com.eldercare.dto.HealthRecordUpdateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.HealthRecord;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.HealthRecordMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock
    private HealthRecordMapper healthRecordMapper;
    @Mock
    private ElderMapper elderMapper;
    @Mock
    private HealthWarningService healthWarningService;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    @BeforeEach
    void setSecurity() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of("health:record:add"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_normalRecord_shouldSaveAndGenerateWarnings() {
        Elder elder = new Elder();
        elder.setId(3L);
        elder.setStatus(1);
        when(elderMapper.selectById(3L)).thenReturn(elder);
        doAnswer(inv -> {
            HealthRecord r = inv.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(healthRecordMapper).insert(any(HealthRecord.class));

        HealthRecordCreateDTO dto = new HealthRecordCreateDTO();
        dto.setElderId(3L);
        dto.setMeasuredAt(LocalDateTime.of(2026, 9, 5, 9, 30));
        dto.setSystolicPressure(120);
        dto.setDiastolicPressure(80);
        dto.setHeartRate(75);

        Long id = healthRecordService.create(dto, null);
        assertEquals(100L, id);
        verify(healthWarningService).generateWarningsForRecord(any(HealthRecord.class));
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_illegalHeartRate_shouldReject() {
        Elder elder = new Elder();
        elder.setId(3L);
        elder.setStatus(1);
        when(elderMapper.selectById(3L)).thenReturn(elder);

        HealthRecordCreateDTO dto = new HealthRecordCreateDTO();
        dto.setElderId(3L);
        dto.setMeasuredAt(LocalDateTime.now());
        dto.setHeartRate(300);

        BusinessException ex = assertThrows(BusinessException.class, () -> healthRecordService.create(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(healthRecordMapper, never()).insert(any(HealthRecord.class));
    }

    @Test
    void create_elderNotFound_shouldReject() {
        when(elderMapper.selectById(999L)).thenReturn(null);
        HealthRecordCreateDTO dto = new HealthRecordCreateDTO();
        dto.setElderId(999L);
        dto.setMeasuredAt(LocalDateTime.now());
        dto.setHeartRate(70);

        BusinessException ex = assertThrows(BusinessException.class, () -> healthRecordService.create(dto, null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void create_noIndicator_shouldReject() {
        Elder elder = new Elder();
        elder.setId(3L);
        elder.setStatus(1);
        when(elderMapper.selectById(3L)).thenReturn(elder);

        HealthRecordCreateDTO dto = new HealthRecordCreateDTO();
        dto.setElderId(3L);
        dto.setMeasuredAt(LocalDateTime.now());

        BusinessException ex = assertThrows(BusinessException.class, () -> healthRecordService.create(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void update_shouldRefreshWarnings() {
        HealthRecord existing = new HealthRecord();
        existing.setId(50L);
        existing.setElderId(3L);
        when(healthRecordMapper.selectById(50L)).thenReturn(existing);

        HealthRecordUpdateDTO dto = new HealthRecordUpdateDTO();
        dto.setMeasuredAt(LocalDateTime.now());
        dto.setSystolicPressure(110);
        dto.setDiastolicPressure(70);
        dto.setBloodGlucose(new BigDecimal("5.5"));

        healthRecordService.update(50L, dto, null);

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordMapper).updateById(captor.capture());
        assertEquals(110, captor.getValue().getSystolicPressure());
        verify(healthWarningService).refreshUnhandledWarnings(any(HealthRecord.class));
    }
}
