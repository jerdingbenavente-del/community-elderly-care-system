package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareAttendanceStatuses;
import com.eldercare.common.CareLeaveStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffAttendanceQueryDTO;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffAttendance;
import com.eldercare.entity.CareStaffLeaveApplication;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareStaffAttendanceMapper;
import com.eldercare.mapper.CareStaffLeaveApplicationMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffAttendanceService;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareStaffAttendanceVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareStaffAttendanceServiceImpl implements CareStaffAttendanceService {

    private final CareStaffAttendanceMapper attendanceMapper;
    private final CareStaffLeaveApplicationMapper leaveMapper;
    private final CareStaffMapper careStaffMapper;
    private final CareStaffIdentityService careStaffIdentityService;
    private final OperationLogService operationLogService;

    public CareStaffAttendanceServiceImpl(CareStaffAttendanceMapper attendanceMapper,
                                          CareStaffLeaveApplicationMapper leaveMapper,
                                          CareStaffMapper careStaffMapper,
                                          CareStaffIdentityService careStaffIdentityService,
                                          OperationLogService operationLogService) {
        this.attendanceMapper = attendanceMapper;
        this.leaveMapper = leaveMapper;
        this.careStaffMapper = careStaffMapper;
        this.careStaffIdentityService = careStaffIdentityService;
        this.operationLogService = operationLogService;
    }

    @Override
    public CareStaffAttendanceVO today() {
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        CareStaffAttendance row = findByStaffAndDate(staff.getId(), today);
        if (row == null && isOnApprovedLeave(staff.getId(), now)) {
            return emptyToday(staff, today, CareAttendanceStatuses.LEAVE);
        }
        if (row == null) {
            return emptyToday(staff, today, CareAttendanceStatuses.NOT_CHECKED);
        }
        return toVo(row, staff);
    }

    @Override
    @Transactional
    public void checkIn(HttpServletRequest request) {
        CareStaff staff = careStaffIdentityService.requireEnabledCurrentCareStaff();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        if (isOnApprovedLeave(staff.getId(), now)) {
            throw new BusinessException(ResultCode.CONFLICT, "当前处于已批准请假时段，不能签到");
        }
        CareStaffAttendance existing = findByStaffAndDate(staff.getId(), today);
        if (existing != null) {
            throw new BusinessException(ResultCode.CONFLICT, "今日已签到，不能重复签到");
        }

        CareStaffAttendance row = new CareStaffAttendance();
        row.setCareStaffId(staff.getId());
        row.setAttendanceDate(today);
        row.setCheckInTime(now);
        row.setStatus(CareAttendanceStatuses.WORKING);
        try {
            attendanceMapper.insert(row);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "今日已签到，不能重复签到");
        }
        operationLogService.record("care", "ATTENDANCE_CHECK_IN", "care_staff_attendance",
                String.valueOf(row.getId()), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void checkOut(HttpServletRequest request) {
        CareStaff staff = careStaffIdentityService.requireEnabledCurrentCareStaff();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        CareStaffAttendance existing = findByStaffAndDate(staff.getId(), today);
        if (existing == null || CareAttendanceStatuses.NOT_CHECKED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "尚未签到，不能签退");
        }
        if (CareAttendanceStatuses.COMPLETED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "今日已签退，不能重复签退");
        }
        if (!CareAttendanceStatuses.WORKING.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前考勤状态不能签退");
        }

        UpdateWrapper<CareStaffAttendance> uw = new UpdateWrapper<>();
        uw.eq("id", existing.getId())
                .eq("care_staff_id", staff.getId())
                .eq("status", CareAttendanceStatuses.WORKING)
                .set("check_out_time", now)
                .set("status", CareAttendanceStatuses.COMPLETED);
        int updated = attendanceMapper.update(null, uw);
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "今日已签退，不能重复签退");
        }
        operationLogService.record("care", "ATTENDANCE_CHECK_OUT", "care_staff_attendance",
                String.valueOf(existing.getId()), "SUCCESS", request);
    }

    @Override
    public PageResult<CareStaffAttendanceVO> pageMine(CareStaffAttendanceQueryDTO query) {
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        return page(query, staff.getId());
    }

    @Override
    public PageResult<CareStaffAttendanceVO> pageAdmin(CareStaffAttendanceQueryDTO query) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看全部考勤");
        }
        return page(query, query.getCareStaffId());
    }

    private PageResult<CareStaffAttendanceVO> page(CareStaffAttendanceQueryDTO query, Long staffId) {
        Page<CareStaffAttendance> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareStaffAttendance> wrapper = new LambdaQueryWrapper<CareStaffAttendance>()
                .eq(staffId != null, CareStaffAttendance::getCareStaffId, staffId)
                .eq(query.getAttendanceDate() != null, CareStaffAttendance::getAttendanceDate, query.getAttendanceDate())
                .ge(query.getStartDate() != null, CareStaffAttendance::getAttendanceDate, query.getStartDate())
                .le(query.getEndDate() != null, CareStaffAttendance::getAttendanceDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getStatus()), CareStaffAttendance::getStatus, query.getStatus())
                .orderByDesc(CareStaffAttendance::getAttendanceDate)
                .orderByDesc(CareStaffAttendance::getId);
        Page<CareStaffAttendance> result = attendanceMapper.selectPage(page, wrapper);
        List<CareStaffAttendanceVO> records = result.getRecords().stream().map(row -> toVo(row, null)).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private CareStaffAttendance findByStaffAndDate(Long careStaffId, LocalDate date) {
        return attendanceMapper.selectOne(new LambdaQueryWrapper<CareStaffAttendance>()
                .eq(CareStaffAttendance::getCareStaffId, careStaffId)
                .eq(CareStaffAttendance::getAttendanceDate, date)
                .last("LIMIT 1"));
    }

    /**
     * 当前时刻落在已批准请假区间内。不改 G5 审批数据。
     */
    private boolean isOnApprovedLeave(Long careStaffId, LocalDateTime now) {
        Long count = leaveMapper.selectCount(new LambdaQueryWrapper<CareStaffLeaveApplication>()
                .eq(CareStaffLeaveApplication::getCareStaffId, careStaffId)
                .eq(CareStaffLeaveApplication::getStatus, CareLeaveStatuses.APPROVED)
                .le(CareStaffLeaveApplication::getStartTime, now)
                .gt(CareStaffLeaveApplication::getEndTime, now));
        return count != null && count > 0;
    }

    private CareStaffAttendanceVO emptyToday(CareStaff staff, LocalDate date, String status) {
        CareStaffAttendanceVO vo = new CareStaffAttendanceVO();
        vo.setCareStaffId(staff.getId());
        vo.setCareStaffName(staff.getName());
        vo.setEmployeeNo(staff.getEmployeeNo());
        vo.setAttendanceDate(date);
        vo.setStatus(status);
        return vo;
    }

    private CareStaffAttendanceVO toVo(CareStaffAttendance row, CareStaff knownStaff) {
        CareStaffAttendanceVO vo = new CareStaffAttendanceVO();
        vo.setId(row.getId());
        vo.setCareStaffId(row.getCareStaffId());
        vo.setAttendanceDate(row.getAttendanceDate());
        vo.setCheckInTime(row.getCheckInTime());
        vo.setCheckOutTime(row.getCheckOutTime());
        vo.setStatus(row.getStatus());
        vo.setRemark(row.getRemark());
        CareStaff staff = knownStaff;
        if (staff == null && row.getCareStaffId() != null) {
            staff = careStaffMapper.selectById(row.getCareStaffId());
        }
        if (staff != null) {
            vo.setCareStaffName(staff.getName());
            vo.setEmployeeNo(staff.getEmployeeNo());
        }
        return vo;
    }
}
