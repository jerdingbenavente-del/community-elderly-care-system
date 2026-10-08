package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.MealTypes;
import com.eldercare.common.MenuStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.MealAdjustmentSaveDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderMealAdjustment;
import com.eldercare.entity.WeeklyMenu;
import com.eldercare.entity.WeeklyMenuItem;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMealAdjustmentMapper;
import com.eldercare.mapper.WeeklyMenuItemMapper;
import com.eldercare.mapper.WeeklyMenuMapper;
import com.eldercare.service.MealAdjustmentService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.MealAdjustmentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MealAdjustmentServiceImpl implements MealAdjustmentService {

    private final ElderMealAdjustmentMapper mealAdjustmentMapper;
    private final ElderMapper elderMapper;
    private final WeeklyMenuMapper weeklyMenuMapper;
    private final WeeklyMenuItemMapper weeklyMenuItemMapper;
    private final OperationLogService operationLogService;

    public MealAdjustmentServiceImpl(ElderMealAdjustmentMapper mealAdjustmentMapper,
                                     ElderMapper elderMapper,
                                     WeeklyMenuMapper weeklyMenuMapper,
                                     WeeklyMenuItemMapper weeklyMenuItemMapper,
                                     OperationLogService operationLogService) {
        this.mealAdjustmentMapper = mealAdjustmentMapper;
        this.elderMapper = elderMapper;
        this.weeklyMenuMapper = weeklyMenuMapper;
        this.weeklyMenuItemMapper = weeklyMenuItemMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public MealAdjustmentVO save(MealAdjustmentSaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        Elder elder = requireElder(dto.getElderId());
        if (!MealTypes.valid(dto.getMealType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "餐次不正确");
        }
        String content = dto.getAdjustedContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调整后的菜品不能为空");
        }
        WeeklyMenuItem publicItem = requirePublicItem(dto.getMenuDate(), dto.getMealType());
        Long userId = SecurityUtils.requireUserId();

        ElderMealAdjustment existing = mealAdjustmentMapper.selectAny(
                dto.getElderId(), dto.getMenuDate(), dto.getMealType());
        if (existing == null) {
            ElderMealAdjustment row = new ElderMealAdjustment();
            row.setElderId(dto.getElderId());
            row.setMenuDate(dto.getMenuDate());
            row.setMealType(dto.getMealType());
            row.setAdjustedContent(content);
            row.setReason(blankToNull(dto.getReason()));
            row.setStatus(MenuStatuses.ADJUST_ACTIVE);
            row.setCreatedBy(userId);
            row.setUpdatedBy(userId);
            mealAdjustmentMapper.insert(row);
            operationLogService.record("care", "MEAL_ADJUST_SAVE", "elder_meal_adjustment",
                    String.valueOf(row.getId()), "SUCCESS", request);
            return toVo(row, elder.getName(), publicItem.getDishName());
        }

        existing.setAdjustedContent(content);
        existing.setReason(blankToNull(dto.getReason()));
        existing.setStatus(MenuStatuses.ADJUST_ACTIVE);
        existing.setUpdatedBy(userId);
        if (existing.getDeleted() != null && existing.getDeleted() == 1) {
            mealAdjustmentMapper.restore(existing);
        } else {
            mealAdjustmentMapper.updateById(existing);
        }
        operationLogService.record("care", "MEAL_ADJUST_SAVE", "elder_meal_adjustment",
                String.valueOf(existing.getId()), "SUCCESS", request);
        ElderMealAdjustment saved = mealAdjustmentMapper.selectById(existing.getId());
        return toVo(saved, elder.getName(), publicItem.getDishName());
    }

    @Override
    @Transactional
    public void cancel(Long id, HttpServletRequest request) {
        requireAdmin();
        ElderMealAdjustment row = mealAdjustmentMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "个性化饮食调整不存在");
        }
        row.setStatus(MenuStatuses.ADJUST_CANCELLED);
        row.setUpdatedBy(SecurityUtils.requireUserId());
        mealAdjustmentMapper.updateById(row);
        operationLogService.record("care", "MEAL_ADJUST_CANCEL", "elder_meal_adjustment",
                String.valueOf(id), "SUCCESS", request);
    }

    private WeeklyMenuItem requirePublicItem(java.time.LocalDate menuDate, String mealType) {
        WeeklyMenu menu = weeklyMenuMapper.selectOne(new LambdaQueryWrapper<WeeklyMenu>()
                .le(WeeklyMenu::getWeekStartDate, menuDate)
                .ge(WeeklyMenu::getWeekEndDate, menuDate));
        if (menu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该日期尚无公共菜单，请先录入周菜单");
        }
        WeeklyMenuItem item = weeklyMenuItemMapper.selectOne(new LambdaQueryWrapper<WeeklyMenuItem>()
                .eq(WeeklyMenuItem::getWeeklyMenuId, menu.getId())
                .eq(WeeklyMenuItem::getMenuDate, menuDate)
                .eq(WeeklyMenuItem::getMealType, mealType));
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该餐次公共菜单不存在");
        }
        return item;
    }

    private void requireAdmin() {
        if (!SecurityUtils.requireLoginUser().hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可维护个性化饮食调整");
        }
    }

    private Elder requireElder(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private MealAdjustmentVO toVo(ElderMealAdjustment row, String elderName, String publicDish) {
        MealAdjustmentVO vo = new MealAdjustmentVO();
        vo.setId(row.getId());
        vo.setElderId(row.getElderId());
        vo.setElderName(elderName);
        vo.setMenuDate(row.getMenuDate());
        vo.setMealType(row.getMealType());
        vo.setPublicDishName(publicDish);
        vo.setAdjustedContent(row.getAdjustedContent());
        vo.setReason(row.getReason());
        vo.setStatus(row.getStatus());
        return vo;
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
