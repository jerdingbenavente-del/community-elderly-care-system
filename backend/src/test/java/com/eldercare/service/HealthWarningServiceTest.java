package com.eldercare.service;

import com.eldercare.common.HealthIndicators;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.HealthWarningHandleDTO;
import com.eldercare.entity.HealthWarning;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.HealthWarningMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.HealthWarningServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthWarningServiceTest {

    @Mock
    private HealthWarningMapper healthWarningMapper;
    @Mock
    private HealthThresholdService healthThresholdService;
    @Mock
    private ElderMapper elderMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private HealthWarningServiceImpl healthWarningService;

    @BeforeEach
    void setSecurity() {
        LoginUser user = new LoginUser(2L, "care01", "x", true, List.of("CARE_STAFF"),
                List.of("health:warning:handle"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_shouldSetHandlerFromCurrentUser() {
        HealthWarning warning = new HealthWarning();
        warning.setId(9L);
        warning.setElderId(3L);
        warning.setStatus(HealthIndicators.STATUS_UNHANDLED);
        warning.setGeneratedAt(LocalDateTime.now().minusHours(1));
        when(healthWarningMapper.selectById(9L)).thenReturn(warning);

        HealthWarningHandleDTO dto = new HealthWarningHandleDTO();
        dto.setHandlingResult("已复查并关注");

        healthWarningService.handle(9L, dto, null);

        ArgumentCaptor<HealthWarning> captor = ArgumentCaptor.forClass(HealthWarning.class);
        verify(healthWarningMapper).updateById(captor.capture());
        HealthWarning saved = captor.getValue();
        assertEquals(HealthIndicators.STATUS_HANDLED, saved.getStatus());
        assertEquals(2L, saved.getHandledBy());
        assertNotNull(saved.getHandledAt());
        assertEquals("已复查并关注", saved.getHandlingResult());
    }

    @Test
    void handle_alreadyHandled_shouldConflict() {
        HealthWarning warning = new HealthWarning();
        warning.setId(9L);
        warning.setStatus(HealthIndicators.STATUS_HANDLED);
        when(healthWarningMapper.selectById(9L)).thenReturn(warning);

        HealthWarningHandleDTO dto = new HealthWarningHandleDTO();
        dto.setHandlingResult("重复处理");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> healthWarningService.handle(9L, dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void getByIdForFamily_shouldCheckBinding() {
        HealthWarning warning = new HealthWarning();
        warning.setId(11L);
        warning.setElderId(88L);
        warning.setStatus(HealthIndicators.STATUS_UNHANDLED);
        warning.setIndicator("HEART_RATE");
        warning.setActualValue("130");
        warning.setThresholdDesc("60~100");
        warning.setDirection("HIGH");
        warning.setWarningLevel("WARNING");
        warning.setGeneratedAt(LocalDateTime.now());
        when(healthWarningMapper.selectById(11L)).thenReturn(warning);

        healthWarningService.getByIdForFamily(11L);

        verify(dataPermissionService).checkFamilyAccess(2L, 88L);
    }
}
