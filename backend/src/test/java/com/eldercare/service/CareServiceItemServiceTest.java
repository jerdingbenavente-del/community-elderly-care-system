package com.eldercare.service;

import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareServiceItemCreateDTO;
import com.eldercare.entity.CareServiceItem;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareServiceItemServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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
class CareServiceItemServiceTest {

    @Mock
    private CareServiceItemMapper careServiceItemMapper;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private CareServiceItemServiceImpl careServiceItemService;

    @BeforeEach
    void setSecurity() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of("care:service:add"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldInsertWhenCodeUnique() {
        when(careServiceItemMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            CareServiceItem item = inv.getArgument(0);
            item.setId(10L);
            return 1;
        }).when(careServiceItemMapper).insert(any(CareServiceItem.class));

        CareServiceItemCreateDTO dto = new CareServiceItemCreateDTO();
        dto.setServiceCode("TEST_CARE");
        dto.setServiceName("测试照料");
        dto.setServiceType("DAILY");
        dto.setDurationMinutes(30);
        dto.setPrice(new BigDecimal("10.00"));

        Long id = careServiceItemService.create(dto, null);
        assertEquals(10L, id);
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_duplicateCode_shouldReject() {
        when(careServiceItemMapper.selectCount(any())).thenReturn(1L);
        CareServiceItemCreateDTO dto = new CareServiceItemCreateDTO();
        dto.setServiceCode("LIFE_CARE");
        dto.setServiceName("重复");
        dto.setDurationMinutes(60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceItemService.create(dto, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(careServiceItemMapper, never()).insert(any(CareServiceItem.class));
    }

    @Test
    void create_familyDenied() {
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "家属无权访问管理端接口"))
                .when(dataPermissionService).denyFamilyOnAdminApi();
        CareServiceItemCreateDTO dto = new CareServiceItemCreateDTO();
        dto.setServiceCode("X");
        dto.setServiceName("Y");
        dto.setDurationMinutes(10);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceItemService.create(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void getById_notFound() {
        when(careServiceItemMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceItemService.getById(999L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getById_ok() {
        CareServiceItem item = new CareServiceItem();
        item.setId(1L);
        item.setServiceCode("LIFE_CARE");
        item.setServiceName("生活照料");
        item.setStatus(CareOrderStatuses.ITEM_ENABLED);
        item.setDurationMinutes(60);
        when(careServiceItemMapper.selectById(1L)).thenReturn(item);

        assertEquals("生活照料", careServiceItemService.getById(1L).getServiceName());
    }

    @Test
    void delete_ok() {
        CareServiceItem item = new CareServiceItem();
        item.setId(8L);
        item.setServiceCode("P3_TEST");
        item.setServiceName("P3测试服务");
        when(careServiceItemMapper.selectById(8L)).thenReturn(item);

        careServiceItemService.delete(8L, null);

        verify(careServiceItemMapper).deleteById(8L);
        verify(operationLogService).record(
                org.mockito.ArgumentMatchers.eq("care"),
                org.mockito.ArgumentMatchers.eq("SERVICE_ITEM_DELETE"),
                org.mockito.ArgumentMatchers.eq("care_service_item"),
                org.mockito.ArgumentMatchers.eq("8"),
                org.mockito.ArgumentMatchers.eq("SUCCESS"),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void delete_notFound() {
        when(careServiceItemMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceItemService.delete(999L, null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        verify(careServiceItemMapper, never()).deleteById(999L);
    }
}
