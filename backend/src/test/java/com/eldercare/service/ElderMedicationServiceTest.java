package com.eldercare.service;

import com.eldercare.common.MedicationStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.ElderMedicationQueryDTO;
import com.eldercare.dto.ElderMedicationSaveDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderMedication;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMedicationMapper;
import com.eldercare.mapper.ElderMedicationTimeMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.ElderMedicationServiceImpl;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElderMedicationServiceTest {

    @Mock private ElderMedicationMapper medicationMapper;
    @Mock private ElderMedicationTimeMapper timeMapper;
    @Mock private ElderMapper elderMapper;
    @Mock private CareServiceOrderMapper careServiceOrderMapper;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private CareStaffIdentityService careStaffIdentityService;
    @Mock private MedicationReminderService medicationReminderService;
    @Mock private OperationLogService operationLogService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private ElderMedicationServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long id, String name, List<String> roles) {
        LoginUser user = new LoginUser(id, name, "x", true, roles, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private ElderMedicationSaveDTO dto() {
        ElderMedicationSaveDTO dto = new ElderMedicationSaveDTO();
        dto.setElderId(18L);
        dto.setMedicineName("测试维生素");
        dto.setDosage("1");
        dto.setDosageUnit("片");
        dto.setUsageMethod("口服");
        dto.setStartDate(LocalDate.of(2026, 10, 1));
        dto.setEndDate(LocalDate.of(2026, 10, 7));
        dto.setDoseTimes(List.of("08:00", "20:00"));
        return dto;
    }

    @Test
    void create_success() {
        login(1L, "admin", List.of("ADMIN"));
        when(elderMapper.selectById(18L)).thenReturn(new Elder());
        doAnswer(inv -> {
            ElderMedication row = inv.getArgument(0);
            row.setId(3L);
            return 1;
        }).when(medicationMapper).insert(any(ElderMedication.class));
        Long id = service.create(dto(), request);
        assertEquals(3L, id);
        verify(timeMapper, org.mockito.Mockito.times(2)).insert(any(com.eldercare.entity.ElderMedicationTime.class));
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_missingElder() {
        login(1L, "admin", List.of("ADMIN"));
        when(elderMapper.selectById(18L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto(), request));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void update_and_status_and_delete() {
        login(1L, "admin", List.of("ADMIN"));
        ElderMedication row = new ElderMedication();
        row.setId(3L);
        row.setElderId(18L);
        row.setStatus(MedicationStatuses.ACTIVE);
        when(elderMapper.selectById(18L)).thenReturn(new Elder());
        when(medicationMapper.selectById(3L)).thenReturn(row);
        service.update(3L, dto(), request);
        service.changeStatus(3L, MedicationStatuses.INACTIVE, request);
        assertEquals(MedicationStatuses.INACTIVE, row.getStatus());
        service.changeStatus(3L, MedicationStatuses.ACTIVE, request);
        assertEquals(MedicationStatuses.ACTIVE, row.getStatus());
        service.delete(3L, request);
        verify(medicationMapper).deleteById(3L);
    }

    @Test
    void missingMedication_404() {
        login(1L, "admin", List.of("ADMIN"));
        when(medicationMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(9L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void familyOtherElder_403() {
        login(4L, "family01", List.of("FAMILY"));
        ElderMedication row = new ElderMedication();
        row.setId(3L);
        row.setElderId(99L);
        when(medicationMapper.selectById(3L)).thenReturn(row);
        org.mockito.Mockito.doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(4L, 99L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(3L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void staffOutsideScope_403() {
        login(2L, "care01", List.of("CARE_STAFF"));
        CareStaff staff = new CareStaff();
        staff.setId(1L);
        when(careStaffIdentityService.requireCurrentCareStaff()).thenReturn(staff);
        CareServiceOrder order = new CareServiceOrder();
        order.setElderId(18L);
        order.setCareStaffId(1L);
        when(careServiceOrderMapper.selectList(any())).thenReturn(List.of(order));
        ElderMedicationQueryDTO query = new ElderMedicationQueryDTO();
        query.setElderId(99L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.pageStaff(query));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void adminPage_ok() {
        login(1L, "admin", List.of("ADMIN"));
        when(medicationMapper.selectPage(any(), any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        service.pageAdmin(new ElderMedicationQueryDTO());
        verify(medicationMapper).selectPage(any(), any());
    }
}
