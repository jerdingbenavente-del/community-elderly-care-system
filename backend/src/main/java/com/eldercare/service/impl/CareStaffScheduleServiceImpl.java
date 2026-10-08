package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareScheduleStatuses;
import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffScheduleCreateDTO;
import com.eldercare.dto.CareStaffScheduleQueryDTO;
import com.eldercare.dto.CareStaffScheduleUpdateDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffSchedule;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.CareStaffScheduleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.CareStaffScheduleService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareStaffScheduleVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareStaffScheduleServiceImpl implements CareStaffScheduleService {

    private final CareStaffScheduleMapper careStaffScheduleMapper;
    private final CareStaffMapper careStaffMapper;
    private final CareStaffIdentityService careStaffIdentityService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public CareStaffScheduleServiceImpl(CareStaffScheduleMapper careStaffScheduleMapper,
                                        CareStaffMapper careStaffMapper,
                                        CareStaffIdentityService careStaffIdentityService,
                                        DataPermissionService dataPermissionService,
                                        OperationLogService operationLogService) {
        this.careStaffScheduleMapper = careStaffScheduleMapper;
        this.careStaffMapper = careStaffMapper;
        this.careStaffIdentityService = careStaffIdentityService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(CareStaffScheduleCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        CareStaff staff = requireActiveStaff(dto.getCareStaffId());
        assertValidTimeRange(dto.getStartTime(), dto.getEndTime());
        assertNoScheduleOverlap(staff.getId(), dto.getScheduleDate(), dto.getStartTime(), dto.getEndTime(), null);

        CareStaffSchedule schedule = new CareStaffSchedule();
        schedule.setCareStaffId(staff.getId());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setStatus(CareScheduleStatuses.AVAILABLE);
        schedule.setRemark(dto.getRemark());
        careStaffScheduleMapper.insert(schedule);

        operationLogService.record("care", "SCHEDULE_CREATE", "care_staff_schedule",
                String.valueOf(schedule.getId()), "SUCCESS", request);
        return schedule.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CareStaffScheduleUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        CareStaffSchedule schedule = requireSchedule(id);
        assertValidTimeRange(dto.getStartTime(), dto.getEndTime());
        if (CareScheduleStatuses.AVAILABLE.equals(dto.getStatus())) {
            requireActiveStaff(schedule.getCareStaffId());
        }
        assertNoScheduleOverlap(schedule.getCareStaffId(), dto.getScheduleDate(),
                dto.getStartTime(), dto.getEndTime(), id);

        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setStatus(dto.getStatus());
        schedule.setRemark(dto.getRemark());
        careStaffScheduleMapper.updateById(schedule);

        operationLogService.record("care", "SCHEDULE_UPDATE", "care_staff_schedule",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireSchedule(id);
        careStaffScheduleMapper.deleteById(id);
        operationLogService.record("care", "SCHEDULE_DELETE", "care_staff_schedule",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public CareStaffScheduleVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();
        CareStaffSchedule schedule = requireSchedule(id);
        assertCanViewSchedule(user, schedule.getCareStaffId());
        return toVo(schedule);
    }

    @Override
    public PageResult<CareStaffScheduleVO> page(CareStaffScheduleQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();

        Long scopedStaffId = resolveScopedStaffId(user, query.getCareStaffId());

        Page<CareStaffSchedule> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareStaffSchedule> wrapper = new LambdaQueryWrapper<>();
        if (scopedStaffId != null) {
            wrapper.eq(CareStaffSchedule::getCareStaffId, scopedStaffId);
        }
        if (query.getScheduleDate() != null) {
            wrapper.eq(CareStaffSchedule::getScheduleDate, query.getScheduleDate());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(CareStaffSchedule::getScheduleDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(CareStaffSchedule::getScheduleDate, query.getEndDate());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareStaffSchedule::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(CareStaffSchedule::getScheduleDate)
                .orderByAsc(CareStaffSchedule::getStartTime)
                .orderByDesc(CareStaffSchedule::getId);

        Page<CareStaffSchedule> result = careStaffScheduleMapper.selectPage(page, wrapper);
        List<CareStaffScheduleVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void assertScheduleCovers(Long careStaffId, LocalDateTime start, LocalDateTime end) {
        if (careStaffId == null || start == null || end == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "排班校验参数不完整");
        }
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        if (!startDate.equals(endDate)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "服务预约暂不支持跨日，请选择同一天时段");
        }
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        List<CareStaffSchedule> schedules = careStaffScheduleMapper.selectList(new LambdaQueryWrapper<CareStaffSchedule>()
                .eq(CareStaffSchedule::getCareStaffId, careStaffId)
                .eq(CareStaffSchedule::getScheduleDate, startDate)
                .eq(CareStaffSchedule::getStatus, CareScheduleStatuses.AVAILABLE));

        boolean covered = schedules.stream().anyMatch(s ->
                !startTime.isBefore(s.getStartTime()) && !endTime.isAfter(s.getEndTime()));
        if (!covered) {
            throw new BusinessException(ResultCode.CONFLICT, "护理员在该时段无有效排班覆盖");
        }
    }

    private Long resolveScopedStaffId(LoginUser user, Long requestedStaffId) {
        if (user.hasRole("ADMIN")) {
            return requestedStaffId;
        }
        if (user.hasRole("CARE_STAFF")) {
            CareStaff self = careStaffIdentityService.requireByUserId(user.getUserId());
            if (requestedStaffId != null && !requestedStaffId.equals(self.getId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的排班");
            }
            return self.getId();
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看排班");
    }

    private void assertCanViewSchedule(LoginUser user, Long scheduleStaffId) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            CareStaff self = careStaffIdentityService.requireByUserId(user.getUserId());
            if (!self.getId().equals(scheduleStaffId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的排班");
            }
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看排班");
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可管理排班");
        }
    }

    private CareStaff requireActiveStaff(Long careStaffId) {
        CareStaff staff = careStaffMapper.selectById(careStaffId);
        if (staff == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护理人员不存在");
        }
        if (!CareStaffStatuses.isEnabled(staff.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "护理人员已停用");
        }
        return staff;
    }

    private CareStaffSchedule requireSchedule(Long id) {
        CareStaffSchedule schedule = careStaffScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "排班不存在");
        }
        return schedule;
    }

    private void assertValidTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始/结束时间不能为空");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
    }

    private void assertNoScheduleOverlap(Long careStaffId, LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        List<CareStaffSchedule> existing = careStaffScheduleMapper.selectList(new LambdaQueryWrapper<CareStaffSchedule>()
                .eq(CareStaffSchedule::getCareStaffId, careStaffId)
                .eq(CareStaffSchedule::getScheduleDate, date)
                .ne(CareStaffSchedule::getStatus, CareScheduleStatuses.CANCELLED));
        for (CareStaffSchedule s : existing) {
            if (excludeId != null && excludeId.equals(s.getId())) {
                continue;
            }
            boolean overlap = start.isBefore(s.getEndTime()) && end.isAfter(s.getStartTime());
            if (overlap) {
                throw new BusinessException(ResultCode.CONFLICT, "该护理员存在重叠排班");
            }
        }
    }

    private CareStaffScheduleVO toVo(CareStaffSchedule schedule) {
        CareStaffScheduleVO vo = new CareStaffScheduleVO();
        vo.setId(schedule.getId());
        vo.setCareStaffId(schedule.getCareStaffId());
        vo.setScheduleDate(schedule.getScheduleDate());
        vo.setStartTime(schedule.getStartTime());
        vo.setEndTime(schedule.getEndTime());
        vo.setStatus(schedule.getStatus());
        vo.setRemark(schedule.getRemark());
        vo.setCreatedAt(schedule.getCreatedAt());
        vo.setUpdatedAt(schedule.getUpdatedAt());
        CareStaff staff = careStaffMapper.selectById(schedule.getCareStaffId());
        if (staff != null) {
            vo.setCareStaffName(staff.getName());
        }
        return vo;
    }
}
