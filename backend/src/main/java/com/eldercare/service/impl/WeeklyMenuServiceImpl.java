package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.MealTypes;
import com.eldercare.common.MenuStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.common.WeekRanges;
import com.eldercare.dto.WeeklyMenuItemDTO;
import com.eldercare.dto.WeeklyMenuSaveDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderDietaryNote;
import com.eldercare.entity.ElderMealAdjustment;
import com.eldercare.entity.WeeklyMenu;
import com.eldercare.entity.WeeklyMenuItem;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.ElderDietaryNoteMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMealAdjustmentMapper;
import com.eldercare.mapper.WeeklyMenuItemMapper;
import com.eldercare.mapper.WeeklyMenuMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.service.WeeklyMenuService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.ElderMenuViewVO;
import com.eldercare.vo.WeeklyMenuItemVO;
import com.eldercare.vo.WeeklyMenuVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WeeklyMenuServiceImpl implements WeeklyMenuService {

    private final WeeklyMenuMapper weeklyMenuMapper;
    private final WeeklyMenuItemMapper weeklyMenuItemMapper;
    private final ElderMapper elderMapper;
    private final ElderDietaryNoteMapper dietaryNoteMapper;
    private final ElderMealAdjustmentMapper mealAdjustmentMapper;
    private final CareServiceOrderMapper careServiceOrderMapper;
    private final DataPermissionService dataPermissionService;
    private final CareStaffIdentityService careStaffIdentityService;
    private final OperationLogService operationLogService;

    public WeeklyMenuServiceImpl(WeeklyMenuMapper weeklyMenuMapper,
                                 WeeklyMenuItemMapper weeklyMenuItemMapper,
                                 ElderMapper elderMapper,
                                 ElderDietaryNoteMapper dietaryNoteMapper,
                                 ElderMealAdjustmentMapper mealAdjustmentMapper,
                                 CareServiceOrderMapper careServiceOrderMapper,
                                 DataPermissionService dataPermissionService,
                                 CareStaffIdentityService careStaffIdentityService,
                                 OperationLogService operationLogService) {
        this.weeklyMenuMapper = weeklyMenuMapper;
        this.weeklyMenuItemMapper = weeklyMenuItemMapper;
        this.elderMapper = elderMapper;
        this.dietaryNoteMapper = dietaryNoteMapper;
        this.mealAdjustmentMapper = mealAdjustmentMapper;
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.dataPermissionService = dataPermissionService;
        this.careStaffIdentityService = careStaffIdentityService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public WeeklyMenuVO create(WeeklyMenuSaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        List<WeeklyMenuItemDTO> items = normalizeItems(dto);
        LocalDate start = items.get(0).getMenuDate();
        LocalDate end = start.plusDays(6);
        assertWeekOpen(end);

        WeeklyMenu existing = weeklyMenuMapper.selectAnyByWeekStart(start);
        if (existing != null && !isDeleted(existing)) {
            throw new BusinessException(ResultCode.CONFLICT, "该周已有公共菜单，请直接修改");
        }

        Long userId = SecurityUtils.requireUserId();
        Long menuId;
        if (existing != null) {
            weeklyMenuMapper.restore(existing.getId(), MenuStatuses.PUBLISHED);
            menuId = existing.getId();
        } else {
            WeeklyMenu row = new WeeklyMenu();
            row.setWeekStartDate(start);
            row.setWeekEndDate(end);
            row.setStatus(MenuStatuses.PUBLISHED);
            row.setCreatedBy(userId);
            weeklyMenuMapper.insert(row);
            menuId = row.getId();
        }
        replaceItems(menuId, items);
        operationLogService.record("care", "WEEKLY_MENU_CREATE", "weekly_menu",
                String.valueOf(menuId), "SUCCESS", request);
        return requireMenuVo(menuId);
    }

    @Override
    @Transactional
    public WeeklyMenuVO update(Long id, WeeklyMenuSaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        WeeklyMenu menu = requireMenu(id);
        assertWeekOpen(menu.getWeekEndDate());
        List<WeeklyMenuItemDTO> items = normalizeItems(dto);
        LocalDate start = items.get(0).getMenuDate();
        if (!start.equals(menu.getWeekStartDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能把菜单改到另一周，请修改对应周的菜单");
        }
        replaceItems(id, items);
        operationLogService.record("care", "WEEKLY_MENU_UPDATE", "weekly_menu",
                String.valueOf(id), "SUCCESS", request);
        return requireMenuVo(id);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        requireAdmin();
        WeeklyMenu menu = requireMenu(id);
        assertWeekOpen(menu.getWeekEndDate());
        weeklyMenuMapper.deleteById(id);
        operationLogService.record("care", "WEEKLY_MENU_DELETE", "weekly_menu",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public WeeklyMenuVO getByWeek(LocalDate dateInWeek) {
        if (dateInWeek == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择周次");
        }
        LocalDate start = WeekRanges.mondayOf(dateInWeek);
        WeeklyMenu menu = weeklyMenuMapper.selectOne(new LambdaQueryWrapper<WeeklyMenu>()
                .eq(WeeklyMenu::getWeekStartDate, start));
        if (menu == null) {
            return null;
        }
        return toVo(menu);
    }

    @Override
    public List<WeeklyMenuVO> listRecent() {
        return weeklyMenuMapper.selectList(new LambdaQueryWrapper<WeeklyMenu>()
                        .orderByDesc(WeeklyMenu::getWeekStartDate)
                        .last("LIMIT 16"))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public ElderMenuViewVO elderView(Long elderId, LocalDate dateInWeek) {
        Elder elder = requireElder(elderId);
        assertCanViewElder(elderId);
        LocalDate anchor = dateInWeek == null ? LocalDate.now() : dateInWeek;
        LocalDate start = WeekRanges.mondayOf(anchor);
        WeeklyMenu menu = weeklyMenuMapper.selectOne(new LambdaQueryWrapper<WeeklyMenu>()
                .eq(WeeklyMenu::getWeekStartDate, start));
        ElderMenuViewVO vo = baseElderView(elder, start);
        if (menu == null) {
            return vo;
        }
        vo.setItems(overlay(menu, elderId, null));
        return vo;
    }

    @Override
    public List<ElderMenuViewVO> staffToday() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("CARE_STAFF") && !user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅护理员或管理员可查看今日膳食");
        }
        LocalDate today = LocalDate.now();
        LocalDate start = WeekRanges.mondayOf(today);
        WeeklyMenu menu = weeklyMenuMapper.selectOne(new LambdaQueryWrapper<WeeklyMenu>()
                .eq(WeeklyMenu::getWeekStartDate, start));
        Set<Long> elderIds = user.hasRole("ADMIN") && !user.hasRole("CARE_STAFF")
                ? allActiveElderIds()
                : staffElderIds();
        List<ElderMenuViewVO> result = new ArrayList<>();
        for (Long elderId : elderIds) {
            Elder elder = elderMapper.selectById(elderId);
            if (elder == null) {
                continue;
            }
            ElderMenuViewVO vo = baseElderView(elder, start);
            if (menu != null) {
                vo.setItems(overlay(menu, elderId, today));
            }
            result.add(vo);
        }
        return result;
    }

    private ElderMenuViewVO baseElderView(Elder elder, LocalDate start) {
        ElderMenuViewVO vo = new ElderMenuViewVO();
        vo.setElderId(elder.getId());
        vo.setElderName(elder.getName());
        vo.setWeekStartDate(start);
        vo.setWeekEndDate(WeekRanges.sundayOf(start));
        ElderDietaryNote note = dietaryNoteMapper.selectOne(new LambdaQueryWrapper<ElderDietaryNote>()
                .eq(ElderDietaryNote::getElderId, elder.getId())
                .eq(ElderDietaryNote::getStatus, MenuStatuses.DIETARY_ACTIVE));
        if (note != null && note.getNote() != null && !note.getNote().isBlank()) {
            vo.setDietaryNote(note.getNote().trim());
        }
        return vo;
    }

    private List<WeeklyMenuItemVO> overlay(WeeklyMenu menu, Long elderId, LocalDate onlyDate) {
        List<WeeklyMenuItem> items = weeklyMenuItemMapper.selectList(new LambdaQueryWrapper<WeeklyMenuItem>()
                .eq(WeeklyMenuItem::getWeeklyMenuId, menu.getId()));
        Map<String, ElderMealAdjustment> adjustments = mealAdjustmentMapper.selectList(
                        new LambdaQueryWrapper<ElderMealAdjustment>()
                                .eq(ElderMealAdjustment::getElderId, elderId)
                                .eq(ElderMealAdjustment::getStatus, MenuStatuses.ADJUST_ACTIVE)
                                .between(ElderMealAdjustment::getMenuDate, menu.getWeekStartDate(), menu.getWeekEndDate()))
                .stream()
                .collect(Collectors.toMap(a -> key(a.getMenuDate(), a.getMealType()), a -> a, (a, b) -> a));
        List<WeeklyMenuItemVO> views = new ArrayList<>();
        for (WeeklyMenuItem item : items) {
            if (onlyDate != null && !onlyDate.equals(item.getMenuDate())) {
                continue;
            }
            WeeklyMenuItemVO vo = toItemVo(item);
            vo.setPublicDishName(item.getDishName());
            ElderMealAdjustment adjustment = adjustments.get(key(item.getMenuDate(), item.getMealType()));
            if (adjustment != null) {
                vo.setAdjusted(true);
                vo.setAdjustmentId(adjustment.getId());
                vo.setDisplayDishName(adjustment.getAdjustedContent());
            } else {
                vo.setAdjusted(false);
                vo.setDisplayDishName(item.getDishName());
            }
            views.add(vo);
        }
        views.sort(itemOrder());
        return views;
    }

    private void replaceItems(Long menuId, List<WeeklyMenuItemDTO> items) {
        weeklyMenuItemMapper.delete(new LambdaQueryWrapper<WeeklyMenuItem>()
                .eq(WeeklyMenuItem::getWeeklyMenuId, menuId));
        for (WeeklyMenuItemDTO dto : items) {
            WeeklyMenuItem row = new WeeklyMenuItem();
            row.setWeeklyMenuId(menuId);
            row.setMenuDate(dto.getMenuDate());
            row.setMealType(dto.getMealType());
            row.setDishName(dto.getDishName().trim());
            row.setDescription(blankToNull(dto.getDescription()));
            weeklyMenuItemMapper.insert(row);
        }
    }

    private List<WeeklyMenuItemDTO> normalizeItems(WeeklyMenuSaveDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().size() != 21) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请一次性录入周一至周日的早餐、午餐和晚餐");
        }
        Map<String, WeeklyMenuItemDTO> unique = new LinkedHashMap<>();
        LocalDate min = null;
        LocalDate max = null;
        for (WeeklyMenuItemDTO item : dto.getItems()) {
            if (item.getMenuDate() == null || !MealTypes.valid(item.getMealType())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "餐次或日期不正确");
            }
            if (item.getDishName() == null || item.getDishName().isBlank() || item.getDishName().trim().length() > 200) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "菜品不能为空且不超过200字");
            }
            if (unique.put(key(item.getMenuDate(), item.getMealType()), item) != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "同一天同一餐次不能重复");
            }
            if (min == null || item.getMenuDate().isBefore(min)) {
                min = item.getMenuDate();
            }
            if (max == null || item.getMenuDate().isAfter(max)) {
                max = item.getMenuDate();
            }
        }
        if (min.getDayOfWeek() != java.time.DayOfWeek.MONDAY || !max.equals(min.plusDays(6))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "菜单必须覆盖连续的周一至周日");
        }
        for (LocalDate date = min; !date.isAfter(max); date = date.plusDays(1)) {
            for (String meal : MealTypes.ALL) {
                if (!unique.containsKey(key(date, meal))) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "请一次性录入周一至周日的早餐、午餐和晚餐");
                }
            }
        }
        return unique.values().stream().sorted(Comparator.comparing(WeeklyMenuItemDTO::getMenuDate)
                .thenComparingInt(i -> MealTypes.ALL.indexOf(i.getMealType()))).toList();
    }

    private void assertWeekOpen(LocalDate weekEnd) {
        if (weekEnd.isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.CONFLICT, "已结束的周菜单不能新建、修改或删除");
        }
    }

    private void assertCanViewElder(Long elderId) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            if (!staffElderIds().contains(elderId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己服务范围内老人的膳食");
            }
            return;
        }
        if (user.hasRole("FAMILY")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), elderId);
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看膳食菜单");
    }

    private Set<Long> staffElderIds() {
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        return careServiceOrderMapper.selectList(new LambdaQueryWrapper<CareServiceOrder>()
                        .eq(CareServiceOrder::getCareStaffId, staff.getId())
                        .ne(CareServiceOrder::getStatus, CareOrderStatuses.CANCELLED))
                .stream()
                .map(CareServiceOrder::getElderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> allActiveElderIds() {
        return elderMapper.selectList(new LambdaQueryWrapper<Elder>().eq(Elder::getStatus, 1))
                .stream()
                .map(Elder::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireAdmin() {
        if (!SecurityUtils.requireLoginUser().hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可维护公共周菜单");
        }
    }

    private Elder requireElder(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private WeeklyMenu requireMenu(Long id) {
        WeeklyMenu menu = weeklyMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "周菜单不存在");
        }
        return menu;
    }

    private WeeklyMenuVO requireMenuVo(Long id) {
        return toVo(requireMenu(id));
    }

    private WeeklyMenuVO toSummary(WeeklyMenu menu) {
        WeeklyMenuVO vo = new WeeklyMenuVO();
        vo.setId(menu.getId());
        vo.setWeekStartDate(menu.getWeekStartDate());
        vo.setWeekEndDate(menu.getWeekEndDate());
        vo.setStatus(menu.getStatus());
        return vo;
    }

    private WeeklyMenuVO toVo(WeeklyMenu menu) {
        WeeklyMenuVO vo = toSummary(menu);
        List<WeeklyMenuItemVO> items = weeklyMenuItemMapper.selectList(new LambdaQueryWrapper<WeeklyMenuItem>()
                        .eq(WeeklyMenuItem::getWeeklyMenuId, menu.getId()))
                .stream()
                .map(this::toItemVo)
                .sorted(itemOrder())
                .toList();
        vo.setItems(items);
        return vo;
    }

    private WeeklyMenuItemVO toItemVo(WeeklyMenuItem item) {
        WeeklyMenuItemVO vo = new WeeklyMenuItemVO();
        vo.setId(item.getId());
        vo.setMenuDate(item.getMenuDate());
        vo.setMealType(item.getMealType());
        vo.setDishName(item.getDishName());
        vo.setDescription(item.getDescription());
        vo.setPublicDishName(item.getDishName());
        vo.setDisplayDishName(item.getDishName());
        vo.setAdjusted(false);
        return vo;
    }

    private static Comparator<WeeklyMenuItemVO> itemOrder() {
        return Comparator.comparing(WeeklyMenuItemVO::getMenuDate)
                .thenComparingInt(i -> MealTypes.ALL.indexOf(i.getMealType()));
    }

    private static boolean isDeleted(WeeklyMenu menu) {
        return menu.getDeleted() != null && menu.getDeleted() == 1;
    }

    private static String key(LocalDate date, String mealType) {
        return date + "|" + mealType;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
