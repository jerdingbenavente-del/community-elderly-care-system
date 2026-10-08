package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.dto.OperationLogQueryDTO;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysOperationLogMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.OperationLogServiceImpl;
import com.eldercare.vo.OperationLogVO;
import com.eldercare.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.entity.SysOperationLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock private SysOperationLogMapper sysOperationLogMapper;
    @Mock private DataPermissionService dataPermissionService;

    @InjectMocks
    private OperationLogServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(String role) {
        LoginUser user = new LoginUser(1L, "u", "x", true, List.of(role), List.of("system:log:list"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void familyForbidden() {
        login("FAMILY");
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.page(new OperationLogQueryDTO()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void careStaffForbidden() {
        login("CARE_STAFF");
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.page(new OperationLogQueryDTO()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void badDateRange() {
        login("ADMIN");
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        OperationLogQueryDTO q = new OperationLogQueryDTO();
        q.setDateFrom(LocalDate.of(2026, 9, 30));
        q.setDateTo(LocalDate.of(2026, 9, 1));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.page(q));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void adminPageOk() {
        login("ADMIN");
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        Page<SysOperationLog> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(sysOperationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);
        PageResult<OperationLogVO> result = service.page(new OperationLogQueryDTO());
        assertEquals(0, result.getTotal());
    }
}
