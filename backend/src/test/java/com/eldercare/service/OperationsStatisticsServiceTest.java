package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.OperationsStatisticsMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.OperationsStatisticsServiceImpl;
import com.eldercare.vo.MedicationReminderVO;
import com.eldercare.vo.OperationsStatisticsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsStatisticsServiceTest {

    @Mock private OperationsStatisticsMapper statisticsMapper;
    @Mock private MedicationReminderService medicationReminderService;
    @Mock private DataPermissionService dataPermissionService;

    @InjectMocks
    private OperationsStatisticsServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of("dashboard:statistics:view"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void loginFamily() {
        LoginUser user = new LoginUser(3L, "family01", "x", true, List.of("FAMILY"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void stubEmptyAggregates() {
        when(statisticsMapper.selectElderStats()).thenReturn(Map.of(
                "total", 0, "active", 0, "inactive", 0, "boundFamily", 0));
        when(statisticsMapper.selectOrderStats(any(), any())).thenReturn(Map.of(
                "total", 0, "pending", 0, "confirmed", 0, "inService", 0, "completed", 0, "cancelled", 0));
        when(statisticsMapper.selectEvaluationStats(any(), any())).thenReturn(Map.of("total", 0));
        when(statisticsMapper.selectCareStaffStats(any(), any())).thenReturn(Map.of(
                "total", 0, "active", 0, "todayCheckedIn", 0, "todayCheckedOut", 0,
                "currentLeave", 0, "pendingLeave", 0));
        when(statisticsMapper.countActiveCareStaff()).thenReturn(0L);
        when(statisticsMapper.countAttendanceWorking(any())).thenReturn(0L);
        when(statisticsMapper.countAttendanceCompleted(any())).thenReturn(0L);
        when(statisticsMapper.countMedicationActive()).thenReturn(0L);
        when(statisticsMapper.countMedicationInactive()).thenReturn(0L);
        when(statisticsMapper.selectActiveElderIds()).thenReturn(List.of());
        when(medicationReminderService.listForElders(anyCollection(), any())).thenReturn(List.of());
        when(statisticsMapper.countCurrentWeekMenu(any())).thenReturn(0L);
        when(statisticsMapper.countWeekMeals(any())).thenReturn(0L);
        when(statisticsMapper.countDietaryNoteElders()).thenReturn(0L);
        when(statisticsMapper.countAdjustedElders()).thenReturn(0L);
        when(statisticsMapper.selectActivityStats(any(), any(), any(), any())).thenReturn(Map.of(
                "upcoming", 0, "published", 0, "cancelled", 0, "completed", 0, "thisMonth", 0));
        when(statisticsMapper.selectOrderTrend(any(), any())).thenReturn(List.of());
    }

    @Test
    void familyForbidden() {
        loginFamily();
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        // denyFamily may throw; also ADMIN check
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.overview(null, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void dateFromAfterDateTo() {
        loginAdmin();
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.overview(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void emptyDataAllZeroAndTrendLength7() {
        loginAdmin();
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        stubEmptyAggregates();
        OperationsStatisticsVO vo = service.overview(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 22));
        assertNotNull(vo);
        assertEquals(0, vo.getElder().getTotal());
        assertEquals(0, vo.getOrders().getTotal());
        assertEquals(0, vo.getOrders().getCompletedRate().compareTo(java.math.BigDecimal.ZERO));
        assertEquals(7, vo.getTrend().size());
        assertTrue(vo.getTrend().stream().allMatch(p -> p.getTotal() == 0 && p.getCompleted() == 0));
        verify(dataPermissionService).denyFamilyOnAdminApi();
    }

    @Test
    void completedRateExcludesCancelled() {
        loginAdmin();
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        stubEmptyAggregates();
        Map<String, Object> orders = new HashMap<>();
        orders.put("total", 10);
        orders.put("pending", 1);
        orders.put("confirmed", 1);
        orders.put("inService", 1);
        orders.put("completed", 5);
        orders.put("cancelled", 2);
        when(statisticsMapper.selectOrderStats(any(), any())).thenReturn(orders);

        OperationsStatisticsVO vo = service.overview(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        // 5 / (10-2) = 0.6250
        assertEquals(0, new java.math.BigDecimal("0.6250").compareTo(vo.getOrders().getCompletedRate()));
    }

    @Test
    void unboundFamilyUsesActiveMinusBound() {
        loginAdmin();
        doNothing().when(dataPermissionService).denyFamilyOnAdminApi();
        stubEmptyAggregates();
        when(statisticsMapper.selectElderStats()).thenReturn(Map.of(
                "total", 10, "active", 8, "inactive", 2, "boundFamily", 3));
        OperationsStatisticsVO vo = service.overview(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertEquals(8, vo.getElder().getActive());
        assertEquals(3, vo.getElder().getBoundFamily());
        assertEquals(5, vo.getElder().getUnboundFamily());
    }
}
