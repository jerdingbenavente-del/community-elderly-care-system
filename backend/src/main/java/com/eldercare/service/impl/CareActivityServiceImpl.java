package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.ActivityStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareActivityQueryDTO;
import com.eldercare.dto.CareActivitySaveDTO;
import com.eldercare.entity.CareActivity;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareActivityMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareActivityService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareActivityVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CareActivityServiceImpl implements CareActivityService {

    private final CareActivityMapper careActivityMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

    public CareActivityServiceImpl(CareActivityMapper careActivityMapper,
                                   SysUserMapper sysUserMapper,
                                   OperationLogService operationLogService) {
        this.careActivityMapper = careActivityMapper;
        this.sysUserMapper = sysUserMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<CareActivityVO> pageAdmin(CareActivityQueryDTO query) {
        requireAdmin();
        refreshCompleted();
        return pageInternal(query, false);
    }

    @Override
    public PageResult<CareActivityVO> pagePublic(CareActivityQueryDTO query) {
        refreshCompleted();
        return pageInternal(query, true);
    }

    @Override
    public CareActivityVO getById(Long id) {
        CareActivity row = requireActivity(id);
        refreshOne(row);
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN") && ActivityStatuses.DRAFT.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在或未发布");
        }
        return toVo(row, resolveNames(List.of(row)));
    }

    @Override
    @Transactional
    public Long create(CareActivitySaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        validateTime(dto);
        CareActivity row = new CareActivity();
        applyFields(row, dto);
        row.setStatus(ActivityStatuses.DRAFT);
        row.setCreatedBy(SecurityUtils.requireUserId());
        careActivityMapper.insert(row);
        operationLogService.record("care", "ACTIVITY_CREATE", "care_activity",
                String.valueOf(row.getId()), "SUCCESS", request);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CareActivitySaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        CareActivity row = requireActivity(id);
        refreshOne(row);
        if (ActivityStatuses.COMPLETED.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已结束活动不可修改");
        }
        if (ActivityStatuses.CANCELLED.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已取消活动不可修改");
        }
        validateTime(dto);
        // DRAFT / PUBLISHED：允许改名称、日期、时间、地点、简介
        applyFields(row, dto);
        careActivityMapper.updateById(row);
        operationLogService.record("care", "ACTIVITY_UPDATE", "care_activity",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void publish(Long id, HttpServletRequest request) {
        requireAdmin();
        CareActivity row = requireActivity(id);
        refreshOne(row);
        if (ActivityStatuses.PUBLISHED.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "活动已发布");
        }
        if (!ActivityStatuses.DRAFT.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅草稿活动可以发布");
        }
        row.setStatus(ActivityStatuses.PUBLISHED);
        careActivityMapper.updateById(row);
        operationLogService.record("care", "ACTIVITY_PUBLISH", "care_activity",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void cancel(Long id, HttpServletRequest request) {
        requireAdmin();
        CareActivity row = requireActivity(id);
        refreshOne(row);
        if (ActivityStatuses.CANCELLED.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "活动已取消");
        }
        if (ActivityStatuses.COMPLETED.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已结束活动不可取消");
        }
        if (!ActivityStatuses.PUBLISHED.equals(row.getStatus()) && !ActivityStatuses.DRAFT.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不可取消");
        }
        row.setStatus(ActivityStatuses.CANCELLED);
        careActivityMapper.updateById(row);
        operationLogService.record("care", "ACTIVITY_CANCEL", "care_activity",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        requireAdmin();
        CareActivity row = requireActivity(id);
        if (!ActivityStatuses.DRAFT.equals(row.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅草稿活动可删除，历史活动请保留");
        }
        careActivityMapper.deleteById(id);
        operationLogService.record("care", "ACTIVITY_DELETE", "care_activity",
                String.valueOf(id), "SUCCESS", request);
    }

    private PageResult<CareActivityVO> pageInternal(CareActivityQueryDTO query, boolean publicOnly) {
        LambdaQueryWrapper<CareActivity> wrapper = new LambdaQueryWrapper<>();
        if (publicOnly) {
            // 家属/护理员：不看草稿；取消可作为历史可见
            wrapper.ne(CareActivity::getStatus, ActivityStatuses.DRAFT);
            if (StringUtils.hasText(query.getStatus())) {
                if (ActivityStatuses.DRAFT.equals(query.getStatus())) {
                    return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
                }
                wrapper.eq(CareActivity::getStatus, query.getStatus().trim());
            }
        } else if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareActivity::getStatus, query.getStatus().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(CareActivity::getActivityName, kw)
                    .or().like(CareActivity::getLocation, kw));
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(CareActivity::getActivityDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(CareActivity::getActivityDate, query.getDateTo());
        }
        wrapper.orderByDesc(CareActivity::getActivityDate)
                .orderByDesc(CareActivity::getStartTime)
                .orderByDesc(CareActivity::getId);

        Page<CareActivity> page = careActivityMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        Map<Long, String> names = resolveNames(page.getRecords());
        List<CareActivityVO> records = page.getRecords().stream()
                .map(r -> toVo(r, names))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void refreshCompleted() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<CareActivity> published = careActivityMapper.selectList(new LambdaQueryWrapper<CareActivity>()
                .eq(CareActivity::getStatus, ActivityStatuses.PUBLISHED));
        for (CareActivity row : published) {
            if (isEnded(row, today, now)) {
                row.setStatus(ActivityStatuses.COMPLETED);
                careActivityMapper.updateById(row);
            }
        }
    }

    private void refreshOne(CareActivity row) {
        if (ActivityStatuses.PUBLISHED.equals(row.getStatus())
                && isEnded(row, LocalDate.now(), LocalTime.now())) {
            row.setStatus(ActivityStatuses.COMPLETED);
            careActivityMapper.updateById(row);
        }
    }

    private boolean isEnded(CareActivity row, LocalDate today, LocalTime now) {
        if (row.getActivityDate().isBefore(today)) {
            return true;
        }
        return row.getActivityDate().equals(today) && !row.getEndTime().isAfter(now);
    }

    private void validateTime(CareActivitySaveDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写开始与结束时间");
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
    }

    private void applyFields(CareActivity row, CareActivitySaveDTO dto) {
        row.setActivityName(dto.getActivityName().trim());
        row.setActivityDate(dto.getActivityDate());
        row.setStartTime(dto.getStartTime());
        row.setEndTime(dto.getEndTime());
        row.setLocation(dto.getLocation().trim());
        row.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
    }

    private CareActivity requireActivity(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "活动ID无效");
        }
        CareActivity row = careActivityMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        return row;
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可操作活动");
        }
    }

    private Map<Long, String> resolveNames(List<CareActivity> rows) {
        Set<Long> ids = rows.stream()
                .map(CareActivity::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> map = new HashMap<>();
        for (Long uid : ids) {
            SysUser u = sysUserMapper.selectById(uid);
            if (u != null) {
                map.put(uid, StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername());
            }
        }
        return map;
    }

    private CareActivityVO toVo(CareActivity row, Map<Long, String> names) {
        CareActivityVO vo = new CareActivityVO();
        vo.setId(row.getId());
        vo.setActivityName(row.getActivityName());
        vo.setActivityDate(row.getActivityDate());
        vo.setStartTime(row.getStartTime());
        vo.setEndTime(row.getEndTime());
        vo.setLocation(row.getLocation());
        vo.setDescription(row.getDescription());
        vo.setStatus(row.getStatus());
        vo.setCreatedBy(row.getCreatedBy());
        vo.setCreatedByName(names.get(row.getCreatedBy()));
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }
}
