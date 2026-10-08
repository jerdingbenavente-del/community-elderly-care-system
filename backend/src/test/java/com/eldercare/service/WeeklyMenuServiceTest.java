package com.eldercare.service;

import com.eldercare.common.MealTypes;
import com.eldercare.common.MenuStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.common.WeekRanges;
import com.eldercare.dto.WeeklyMenuItemDTO;
import com.eldercare.dto.WeeklyMenuSaveDTO;
import com.eldercare.entity.WeeklyMenu;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.ElderDietaryNoteMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMealAdjustmentMapper;
import com.eldercare.mapper.WeeklyMenuItemMapper;
import com.eldercare.mapper.WeeklyMenuMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.WeeklyMenuServiceImpl;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyMenuServiceTest {

    @Mock private WeeklyMenuMapper weeklyMenuMapper;
    @Mock private WeeklyMenuItemMapper weeklyMenuItemMapper;
    @Mock private ElderMapper elderMapper;
    @Mock private ElderDietaryNoteMapper dietaryNoteMapper;
    @Mock private ElderMealAdjustmentMapper mealAdjustmentMapper;
    @Mock private CareServiceOrderMapper careServiceOrderMapper;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private CareStaffIdentityService careStaffIdentityService;
    @Mock private OperationLogService operationLogService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private WeeklyMenuServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private WeeklyMenuSaveDTO fullWeek(LocalDate monday) {
        WeeklyMenuSaveDTO dto = new WeeklyMenuSaveDTO();
        List<WeeklyMenuItemDTO> items = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            LocalDate date = monday.plusDays(d);
            for (String meal : MealTypes.ALL) {
                WeeklyMenuItemDTO item = new WeeklyMenuItemDTO();
                item.setMenuDate(date);
                item.setMealType(meal);
                item.setDishName(meal + "-" + date);
                items.add(item);
            }
        }
        dto.setItems(items);
        return dto;
    }

    @Test
    void create_duplicateWeek_409() {
        loginAdmin();
        LocalDate monday = WeekRanges.nextWeekMonday(LocalDate.now());
        WeeklyMenu existing = new WeeklyMenu();
        existing.setId(2L);
        existing.setDeleted(0);
        when(weeklyMenuMapper.selectAnyByWeekStart(monday)).thenReturn(existing);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(fullWeek(monday), request));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void create_success() {
        loginAdmin();
        LocalDate monday = WeekRanges.nextWeekMonday(LocalDate.now());
        when(weeklyMenuMapper.selectAnyByWeekStart(monday)).thenReturn(null);
        doAnswer(inv -> {
            WeeklyMenu row = inv.getArgument(0);
            row.setId(5L);
            return 1;
        }).when(weeklyMenuMapper).insert(any(WeeklyMenu.class));
        when(weeklyMenuMapper.selectById(5L)).thenAnswer(inv -> {
            WeeklyMenu row = new WeeklyMenu();
            row.setId(5L);
            row.setWeekStartDate(monday);
            row.setWeekEndDate(monday.plusDays(6));
            row.setStatus(MenuStatuses.PUBLISHED);
            return row;
        });
        when(weeklyMenuItemMapper.selectList(any())).thenReturn(List.of());
        var vo = service.create(fullWeek(monday), request);
        assertEquals(5L, vo.getId());
        verify(operationLogService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_incompleteItems_400() {
        loginAdmin();
        WeeklyMenuSaveDTO dto = new WeeklyMenuSaveDTO();
        dto.setItems(List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto, request));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }
}
