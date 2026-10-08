package com.eldercare.service;

import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareEvaluationCreateDTO;
import com.eldercare.entity.CareEvaluation;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareEvaluationMapper;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareEvaluationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareEvaluationServiceTest {

    @Mock
    private CareEvaluationMapper careEvaluationMapper;
    @Mock
    private CareServiceOrderMapper careServiceOrderMapper;
    @Mock
    private CareServiceItemMapper careServiceItemMapper;
    @Mock
    private CareStaffMapper careStaffMapper;
    @Mock
    private ElderMapper elderMapper;
    @Mock
    private ElderFamilyMapper elderFamilyMapper;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private CareEvaluationServiceImpl careEvaluationService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, List<String> roles) {
        LoginUser user = new LoginUser(userId, "u" + userId, "x", true, roles, List.of("family:evaluation:add"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareServiceOrder order(Long id, Long elderId, String status) {
        CareServiceOrder o = new CareServiceOrder();
        o.setId(id);
        o.setElderId(elderId);
        o.setStatus(status);
        o.setOrderNo("ORD" + id);
        o.setServiceItemId(1L);
        return o;
    }

    private CareEvaluationCreateDTO dto(int score, String content) {
        CareEvaluationCreateDTO d = new CareEvaluationCreateDTO();
        d.setScore(score);
        d.setContent(content);
        return d;
    }

    @Test
    void createEvaluation_success() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(10L)).thenReturn(order(10L, 1L, CareOrderStatuses.COMPLETED));
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 1L);
        when(careEvaluationMapper.selectCount(any())).thenReturn(0L);
        when(careEvaluationMapper.insert(any(CareEvaluation.class))).thenAnswer(inv -> {
            CareEvaluation e = inv.getArgument(0);
            e.setId(100L);
            return 1;
        });
        CareServiceOrder completed = order(10L, 1L, CareOrderStatuses.COMPLETED);
        when(careServiceOrderMapper.selectBatchIds(any())).thenReturn(List.of(completed));
        when(careServiceItemMapper.selectBatchIds(any())).thenReturn(List.of());
        when(elderMapper.selectBatchIds(any())).thenReturn(List.of());

        var vo = careEvaluationService.createForFamily(10L, dto(5, "服务很好"), null);
        assertNotNull(vo);
        assertEquals(100L, vo.getId());
        assertEquals(5, vo.getScore());
        verify(dataPermissionService).checkFamilyAccess(3L, 1L);
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createEvaluation_orderNotFound() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(99L, dto(5, null), null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        verify(careEvaluationMapper, never()).insert(any(CareEvaluation.class));
    }

    @Test
    void createEvaluation_notCompleted() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(11L)).thenReturn(order(11L, 1L, CareOrderStatuses.PENDING));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(11L, dto(4, "x"), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(dataPermissionService, never()).checkFamilyAccess(any(), any());
    }

    @Test
    void createEvaluation_confirmed_shouldReject() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(12L)).thenReturn(order(12L, 1L, CareOrderStatuses.CONFIRMED));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(12L, dto(3, null), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createEvaluation_inService_shouldReject() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(13L)).thenReturn(order(13L, 1L, CareOrderStatuses.IN_SERVICE));
        assertEquals(ResultCode.CONFLICT.getCode(),
                assertThrows(BusinessException.class,
                        () -> careEvaluationService.createForFamily(13L, dto(3, null), null)).getCode());
    }

    @Test
    void createEvaluation_cancelled_shouldReject() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(14L)).thenReturn(order(14L, 1L, CareOrderStatuses.CANCELLED));
        assertEquals(ResultCode.CONFLICT.getCode(),
                assertThrows(BusinessException.class,
                        () -> careEvaluationService.createForFamily(14L, dto(3, null), null)).getCode());
    }

    @Test
    void createEvaluation_noFamilyAccess() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(15L)).thenReturn(order(15L, 2L, CareOrderStatuses.COMPLETED));
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(3L, 2L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(15L, dto(5, null), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(careEvaluationMapper, never()).insert(any(CareEvaluation.class));
    }

    @Test
    void createEvaluation_duplicate() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(16L)).thenReturn(order(16L, 1L, CareOrderStatuses.COMPLETED));
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 1L);
        when(careEvaluationMapper.selectCount(any())).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(16L, dto(5, "再评"), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(careEvaluationMapper, never()).insert(any(CareEvaluation.class));
    }

    @Test
    void createEvaluation_duplicateConcurrent_uniqueConstraint() {
        login(3L, List.of("FAMILY"));
        when(careServiceOrderMapper.selectById(17L)).thenReturn(order(17L, 1L, CareOrderStatuses.COMPLETED));
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 1L);
        when(careEvaluationMapper.selectCount(any())).thenReturn(0L);
        when(careEvaluationMapper.insert(any(CareEvaluation.class)))
                .thenThrow(new DataIntegrityViolationException("uk_care_evaluation_order"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(17L, dto(5, null), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createEvaluation_multiRoleFamilySuccess() {
        login(100L, List.of("FAMILY", "CARE_STAFF"));
        when(careServiceOrderMapper.selectById(20L)).thenReturn(order(20L, 1L, CareOrderStatuses.COMPLETED));
        doNothing().when(dataPermissionService).checkFamilyAccess(100L, 1L);
        when(careEvaluationMapper.selectCount(any())).thenReturn(0L);
        when(careEvaluationMapper.insert(any(CareEvaluation.class))).thenAnswer(inv -> {
            CareEvaluation e = inv.getArgument(0);
            e.setId(200L);
            return 1;
        });
        CareServiceOrder completed = order(20L, 1L, CareOrderStatuses.COMPLETED);
        when(careServiceOrderMapper.selectBatchIds(any())).thenReturn(List.of(completed));
        when(careServiceItemMapper.selectBatchIds(any())).thenReturn(List.of());
        when(elderMapper.selectBatchIds(any())).thenReturn(List.of());

        var vo = careEvaluationService.createForFamily(20L, dto(4, "多角色成功"), null);
        assertEquals(200L, vo.getId());
        verify(dataPermissionService).checkFamilyAccess(100L, 1L);
    }

    @Test
    void createEvaluation_multiRoleFamilyDenied() {
        login(100L, List.of("FAMILY", "CARE_STAFF"));
        when(careServiceOrderMapper.selectById(21L)).thenReturn(order(21L, 2L, CareOrderStatuses.COMPLETED));
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(100L, 2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careEvaluationService.createForFamily(21L, dto(5, null), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(careEvaluationMapper, never()).insert(any(CareEvaluation.class));
    }
}
