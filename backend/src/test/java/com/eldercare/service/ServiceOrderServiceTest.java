package com.eldercare.service;

import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CancelServiceOrderDTO;
import com.eldercare.dto.ConfirmServiceOrderDTO;
import com.eldercare.dto.ServiceOrderCreateDTO;
import com.eldercare.dto.ServiceOrderQueryDTO;
import com.eldercare.entity.CareServiceItem;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.CareServiceOrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

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
    private CareStaffScheduleService careStaffScheduleService;
    @Mock
    private CareStaffIdentityService careStaffIdentityService;
    @Mock
    private DataPermissionService dataPermissionService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private CareServiceOrderServiceImpl careServiceOrderService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String username, List<String> roles, List<String> perms) {
        LoginUser user = new LoginUser(userId, username, "x", true, roles, perms);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CareServiceItem enabledItem(int minutes) {
        CareServiceItem item = new CareServiceItem();
        item.setId(1L);
        item.setStatus(CareOrderStatuses.ITEM_ENABLED);
        item.setDurationMinutes(minutes);
        item.setServiceName("生活照料");
        item.setPrice(new BigDecimal("80.00"));
        return item;
    }

    private Elder activeElder(Long id) {
        Elder elder = new Elder();
        elder.setId(id);
        elder.setStatus(1);
        elder.setName("测试老人");
        return elder;
    }

    private ConfirmServiceOrderDTO confirmDto(Long staffId) {
        ConfirmServiceOrderDTO dto = new ConfirmServiceOrderDTO();
        dto.setCareStaffId(staffId);
        return dto;
    }

    @Test
    void create_alwaysNullCareStaffId() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:add"));
        when(elderMapper.selectById(3L)).thenReturn(activeElder(3L));
        when(careServiceItemMapper.selectById(1L)).thenReturn(enabledItem(60));
        doAnswer(inv -> {
            CareServiceOrder o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        }).when(careServiceOrderMapper).insert(any(CareServiceOrder.class));

        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 9, 0);
        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO();
        dto.setElderId(3L);
        dto.setServiceItemId(1L);
        dto.setScheduledStartTime(start);
        dto.setScheduledEndTime(start.plusMinutes(60));

        Long id = careServiceOrderService.create(dto, null);
        assertEquals(100L, id);
        ArgumentCaptor<CareServiceOrder> cap = ArgumentCaptor.forClass(CareServiceOrder.class);
        verify(careServiceOrderMapper).insert(cap.capture());
        assertNull(cap.getValue().getCareStaffId());
        assertEquals(CareOrderStatuses.PENDING, cap.getValue().getStatus());
        assertEquals(new BigDecimal("80.00"), cap.getValue().getAmount());
        assertEquals("UNPAID", cap.getValue().getPaymentStatus());
        assertNull(cap.getValue().getPaidAt());
    }

    @Test
    void pay_family_ok() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:pay"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(50L);
        order.setElderId(3L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setPaymentStatus("UNPAID");
        order.setAmount(new BigDecimal("80.00"));
        when(careServiceOrderMapper.selectById(50L)).thenReturn(order);
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 3L);

        careServiceOrderService.pay(50L, null);
        assertEquals("PAID", order.getPaymentStatus());
        assertNotNull(order.getPaidAt());
        assertEquals(CareOrderStatuses.PENDING, order.getStatus());
        verify(careServiceOrderMapper).updateById(order);
    }

    @Test
    void pay_repeat_shouldReject() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:pay"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(50L);
        order.setElderId(3L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setPaymentStatus("PAID");
        when(careServiceOrderMapper.selectById(50L)).thenReturn(order);
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.pay(50L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        verify(careServiceOrderMapper, never()).updateById(any(CareServiceOrder.class));
    }

    @Test
    void pay_staff_shouldReject() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:order:start"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.pay(50L, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void pay_admin_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:list"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.pay(50L, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void pay_unboundFamily_shouldReject() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:pay"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(50L);
        order.setElderId(9L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setPaymentStatus("UNPAID");
        when(careServiceOrderMapper.selectById(50L)).thenReturn(order);
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(3L, 9L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.pay(50L, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(careServiceOrderMapper, never()).updateById(any(CareServiceOrder.class));
    }

    @Test
    void pay_cancelled_shouldReject() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:pay"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(50L);
        order.setElderId(3L);
        order.setStatus(CareOrderStatuses.CANCELLED);
        order.setPaymentStatus("UNPAID");
        when(careServiceOrderMapper.selectById(50L)).thenReturn(order);
        doNothing().when(dataPermissionService).checkFamilyAccess(3L, 3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.pay(50L, null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_familyUnbound_shouldReject() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:add"));
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(3L, 9L);

        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO();
        dto.setElderId(9L);
        dto.setServiceItemId(1L);
        dto.setScheduledStartTime(LocalDateTime.now().plusDays(1));
        dto.setScheduledEndTime(LocalDateTime.now().plusDays(1).plusMinutes(60));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.create(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void create_durationMismatch_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:add"));
        when(elderMapper.selectById(3L)).thenReturn(activeElder(3L));
        when(careServiceItemMapper.selectById(1L)).thenReturn(enabledItem(60));

        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 9, 0);
        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO();
        dto.setElderId(3L);
        dto.setServiceItemId(1L);
        dto.setScheduledStartTime(start);
        dto.setScheduledEndTime(start.plusMinutes(30));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.create(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(careServiceOrderMapper, never()).insert(any(CareServiceOrder.class));
    }

    @Test
    void confirm_assignStaff_ok() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        LocalDateTime start = LocalDateTime.of(2026, 9, 11, 10, 0);
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setScheduledStartTime(start);
        order.setScheduledEndTime(start.plusMinutes(60));
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(1);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        when(careServiceOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        careServiceOrderService.confirm(1L, confirmDto(1L), null);
        assertEquals(CareOrderStatuses.CONFIRMED, order.getStatus());
        assertEquals(1L, order.getCareStaffId());
        verify(careStaffScheduleService).assertScheduleCovers(eq(1L), eq(start), eq(start.plusMinutes(60)));
        verify(careServiceOrderMapper).updateById(order);
    }

    @Test
    void confirm_staffNotFound() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setScheduledStartTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        order.setScheduledEndTime(LocalDateTime.of(2026, 9, 11, 11, 0));
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);
        when(careStaffMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(99L), null));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void confirm_staffDisabled() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setScheduledStartTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        order.setScheduledEndTime(LocalDateTime.of(2026, 9, 11, 11, 0));
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(0);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void confirm_noScheduleCover() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        LocalDateTime start = LocalDateTime.of(2026, 9, 11, 10, 0);
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setScheduledStartTime(start);
        order.setScheduledEndTime(start.plusMinutes(60));
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(1);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);
        doThrow(new BusinessException(ResultCode.CONFLICT, "护理员在该时段无有效排班覆盖"))
                .when(careStaffScheduleService).assertScheduleCovers(any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void confirm_timeConflict() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        LocalDateTime start = LocalDateTime.of(2026, 9, 11, 10, 0);
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setScheduledStartTime(start);
        order.setScheduledEndTime(start.plusMinutes(60));
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setStatus(1);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);

        CareServiceOrder existing = new CareServiceOrder();
        existing.setId(50L);
        existing.setCareStaffId(1L);
        existing.setStatus(CareOrderStatuses.CONFIRMED);
        existing.setScheduledStartTime(start.plusMinutes(30));
        existing.setScheduledEndTime(start.plusMinutes(90));
        when(careServiceOrderMapper.selectList(any())).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void confirm_cancelledOrder_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.CANCELLED);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void confirm_completedOrder_shouldReject() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:confirm"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.COMPLETED);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void confirm_nonAdmin_shouldReject() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:order:start"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void confirm_family_shouldReject() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:view"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.confirm(1L, confirmDto(1L), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void start_onlyAssignedStaff() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:order:start"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(1);
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff);
        when(careStaffMapper.selectById(1L)).thenReturn(staff);

        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.CONFIRMED);
        order.setCareStaffId(1L);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        careServiceOrderService.start(1L, null);
        assertEquals(CareOrderStatuses.IN_SERVICE, order.getStatus());
    }

    @Test
    void start_otherStaff_shouldReject() {
        login(2L, "care01", List.of("CARE_STAFF"), List.of("care:order:start"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        staff.setUserId(2L);
        staff.setStatus(1);
        when(careStaffIdentityService.requireEnabledCurrentCareStaff()).thenReturn(staff);

        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.CONFIRMED);
        order.setCareStaffId(99L);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.start(1L, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void complete_setsServerCompletedAt() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:complete"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.IN_SERVICE);
        order.setCareStaffId(1L);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        careServiceOrderService.complete(1L, null);
        assertEquals(CareOrderStatuses.COMPLETED, order.getStatus());
        assertNotNull(order.getCompletedAt());
        assertEquals(1L, order.getCareStaffId());
    }

    @Test
    void cancel_confirmed_keepsCareStaffId() {
        login(1L, "admin", List.of("ADMIN"), List.of("care:order:cancel"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.CONFIRMED);
        order.setCareStaffId(1L);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        CancelServiceOrderDTO dto = new CancelServiceOrderDTO();
        dto.setCancelReason("改派前取消");
        careServiceOrderService.cancel(1L, dto, null);
        assertEquals(CareOrderStatuses.CANCELLED, order.getStatus());
        assertEquals(1L, order.getCareStaffId());
    }

    @Test
    void family_cannotStart() {
        login(3L, "family01", List.of("FAMILY"), List.of("family:order:view"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(1L);
        order.setStatus(CareOrderStatuses.CONFIRMED);
        order.setCareStaffId(1L);
        when(careServiceOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.start(1L, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    /** P6.1：FAMILY+CARE_STAFF 创建绑定老人订单须走 checkFamilyAccess 且成功 */
    @Test
    void create_familyCareStaff_boundElder_ok() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"),
                List.of("family:order:add", "care:order:add"));
        doNothing().when(dataPermissionService).checkFamilyAccess(100L, 1L);
        when(elderMapper.selectById(1L)).thenReturn(activeElder(1L));
        when(careServiceItemMapper.selectById(1L)).thenReturn(enabledItem(60));
        doAnswer(inv -> {
            CareServiceOrder o = inv.getArgument(0);
            o.setId(201L);
            return 1;
        }).when(careServiceOrderMapper).insert(any(CareServiceOrder.class));

        LocalDateTime start = LocalDateTime.of(2026, 9, 12, 9, 0);
        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO();
        dto.setElderId(1L);
        dto.setServiceItemId(1L);
        dto.setScheduledStartTime(start);
        dto.setScheduledEndTime(start.plusMinutes(60));

        Long id = careServiceOrderService.create(dto, null);
        assertEquals(201L, id);
        verify(dataPermissionService).checkFamilyAccess(100L, 1L);
    }

    /** P6.1 核心：FAMILY+CARE_STAFF 未绑定老人不得因 isFamilyOnly=false 而跳过校验 */
    @Test
    void create_familyCareStaff_unboundElder_shouldReject() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"),
                List.of("family:order:add", "care:order:add"));
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(100L, 2L);

        ServiceOrderCreateDTO dto = new ServiceOrderCreateDTO();
        dto.setElderId(2L);
        dto.setServiceItemId(1L);
        dto.setScheduledStartTime(LocalDateTime.now().plusDays(1));
        dto.setScheduledEndTime(LocalDateTime.now().plusDays(1).plusMinutes(60));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.create(dto, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(dataPermissionService).checkFamilyAccess(100L, 2L);
        verify(careServiceOrderMapper, never()).insert(any(CareServiceOrder.class));
    }

    @Test
    void getById_familyCareStaff_bound_ok() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"), List.of("family:order:view"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(10L);
        order.setElderId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        when(careServiceOrderMapper.selectById(10L)).thenReturn(order);
        when(dataPermissionService.isFamilyBound(100L, 1L)).thenReturn(true);

        assertNotNull(careServiceOrderService.getById(10L));
    }

    @Test
    void getById_familyCareStaff_unbound_shouldReject() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"), List.of("family:order:view"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(11L);
        order.setElderId(2L);
        order.setStatus(CareOrderStatuses.PENDING);
        order.setCareStaffId(null);
        when(careServiceOrderMapper.selectById(11L)).thenReturn(order);
        when(dataPermissionService.isFamilyBound(100L, 2L)).thenReturn(false);
        when(careStaffIdentityService.getByUserId(100L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.getById(11L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void page_familyCareStaff_unboundElderId_shouldReject() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"), List.of("family:order:list"));
        ElderFamily bind = new ElderFamily();
        bind.setElderId(1L);
        when(elderFamilyMapper.selectList(any())).thenReturn(List.of(bind));

        ServiceOrderQueryDTO query = new ServiceOrderQueryDTO();
        query.setElderId(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.page(query));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void cancel_familyCareStaff_bound_ok() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"), List.of("family:order:cancel"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(12L);
        order.setElderId(1L);
        order.setStatus(CareOrderStatuses.PENDING);
        when(careServiceOrderMapper.selectById(12L)).thenReturn(order);
        doNothing().when(dataPermissionService).checkFamilyAccess(100L, 1L);

        CancelServiceOrderDTO dto = new CancelServiceOrderDTO();
        dto.setCancelReason("多角色取消");
        careServiceOrderService.cancel(12L, dto, null);
        assertEquals(CareOrderStatuses.CANCELLED, order.getStatus());
        verify(dataPermissionService).checkFamilyAccess(100L, 1L);
    }

    @Test
    void cancel_familyCareStaff_unbound_shouldReject() {
        login(100L, "dual", List.of("FAMILY", "CARE_STAFF"), List.of("family:order:cancel"));
        CareServiceOrder order = new CareServiceOrder();
        order.setId(13L);
        order.setElderId(2L);
        order.setStatus(CareOrderStatuses.PENDING);
        when(careServiceOrderMapper.selectById(13L)).thenReturn(order);
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(100L, 2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> careServiceOrderService.cancel(13L, new CancelServiceOrderDTO(), null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(careServiceOrderMapper, never()).updateById(any(CareServiceOrder.class));
    }
}
