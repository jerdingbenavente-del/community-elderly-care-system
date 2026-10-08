package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareLeaveStatuses;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffLeaveCreateDTO;
import com.eldercare.dto.CareStaffLeaveQueryDTO;
import com.eldercare.dto.CareStaffLeaveReviewDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffLeaveApplication;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffLeaveApplicationMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.CareStaffLeaveService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareStaffLeaveVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CareStaffLeaveServiceImpl implements CareStaffLeaveService {

    private final CareStaffLeaveApplicationMapper leaveMapper;
    private final CareStaffMapper careStaffMapper;
    private final CareServiceOrderMapper careServiceOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final CareStaffIdentityService careStaffIdentityService;
    private final OperationLogService operationLogService;

    public CareStaffLeaveServiceImpl(CareStaffLeaveApplicationMapper leaveMapper,
                                     CareStaffMapper careStaffMapper,
                                     CareServiceOrderMapper careServiceOrderMapper,
                                     SysUserMapper sysUserMapper,
                                     CareStaffIdentityService careStaffIdentityService,
                                     OperationLogService operationLogService) {
        this.leaveMapper = leaveMapper;
        this.careStaffMapper = careStaffMapper;
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.sysUserMapper = sysUserMapper;
        this.careStaffIdentityService = careStaffIdentityService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(CareStaffLeaveCreateDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareStaff staff = careStaffIdentityService.requireEnabledCurrentCareStaff();
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        String reason = dto.getReason().trim();
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原因不能为空");
        }
        assertNoLeaveOverlap(staff.getId(), dto.getStartTime(), dto.getEndTime(), null);

        CareStaffLeaveApplication leave = new CareStaffLeaveApplication();
        leave.setCareStaffId(staff.getId());
        leave.setStartTime(dto.getStartTime());
        leave.setEndTime(dto.getEndTime());
        leave.setReason(reason);
        leave.setStatus(CareLeaveStatuses.PENDING);
        leave.setAffectedOrderCount(0);
        leaveMapper.insert(leave);

        operationLogService.record("care", "LEAVE_APPLY", "care_staff_leave_application",
                String.valueOf(leave.getId()), "SUCCESS", request);
        return leave.getId();
    }

    @Override
    public PageResult<CareStaffLeaveVO> pageMine(CareStaffLeaveQueryDTO query) {
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        Page<CareStaffLeaveApplication> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareStaffLeaveApplication> wrapper = new LambdaQueryWrapper<CareStaffLeaveApplication>()
                .eq(CareStaffLeaveApplication::getCareStaffId, staff.getId())
                .eq(StringUtils.hasText(query.getStatus()), CareStaffLeaveApplication::getStatus, query.getStatus())
                .orderByDesc(CareStaffLeaveApplication::getId);
        Page<CareStaffLeaveApplication> result = leaveMapper.selectPage(page, wrapper);
        List<CareStaffLeaveVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<CareStaffLeaveVO> pageAdmin(CareStaffLeaveQueryDTO query) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看全部请假申请");
        }
        Page<CareStaffLeaveApplication> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareStaffLeaveApplication> wrapper = new LambdaQueryWrapper<CareStaffLeaveApplication>()
                .eq(query.getCareStaffId() != null, CareStaffLeaveApplication::getCareStaffId, query.getCareStaffId())
                .eq(StringUtils.hasText(query.getStatus()), CareStaffLeaveApplication::getStatus, query.getStatus())
                .orderByDesc(CareStaffLeaveApplication::getId);
        Page<CareStaffLeaveApplication> result = leaveMapper.selectPage(page, wrapper);
        List<CareStaffLeaveVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public CareStaffLeaveVO getById(Long id) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareStaffLeaveApplication leave = requireLeave(id);
        assertCanView(user, leave);
        return toVo(leave);
    }

    @Override
    @Transactional
    public void cancel(Long id, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareStaffLeaveApplication leave = requireLeave(id);
        CareStaff self = careStaffIdentityService.requireCurrentCareStaff();
        if (!Objects.equals(leave.getCareStaffId(), self.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤销自己的请假申请");
        }
        if (!CareLeaveStatuses.PENDING.equals(leave.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待审批申请可撤销");
        }
        leaveMapper.deleteById(id);
        operationLogService.record("care", "LEAVE_CANCEL", "care_staff_leave_application",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void approve(Long id, CareStaffLeaveReviewDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可审批");
        }
        CareStaffLeaveApplication leave = requireLeave(id);
        if (!CareLeaveStatuses.PENDING.equals(leave.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待审批申请可审批通过");
        }

        LocalDateTime now = LocalDateTime.now();
        String remark = dto != null && StringUtils.hasText(dto.getReviewRemark())
                ? dto.getReviewRemark().trim() : null;

        // 乐观条件更新，防止并发双审
        UpdateWrapper<CareStaffLeaveApplication> uw = new UpdateWrapper<>();
        uw.eq("id", id)
                .eq("status", CareLeaveStatuses.PENDING)
                .set("status", CareLeaveStatuses.APPROVED)
                .set("reviewed_by", user.getUserId())
                .set("reviewed_at", now)
                .set("review_remark", remark);
        int updated = leaveMapper.update(null, uw);
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "申请状态已变更，请刷新后重试");
        }

        int affected = releaseAffectedOrders(leave);
        UpdateWrapper<CareStaffLeaveApplication> countUw = new UpdateWrapper<>();
        countUw.eq("id", id).set("affected_order_count", affected);
        leaveMapper.update(null, countUw);

        operationLogService.record("care", "LEAVE_APPROVE", "care_staff_leave_application",
                String.valueOf(id) + "|affected=" + affected, "SUCCESS", request);
    }

    @Override
    @Transactional
    public void reject(Long id, CareStaffLeaveReviewDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可审批");
        }
        CareStaffLeaveApplication leave = requireLeave(id);
        if (!CareLeaveStatuses.PENDING.equals(leave.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待审批申请可驳回");
        }

        LocalDateTime now = LocalDateTime.now();
        String remark = dto != null && StringUtils.hasText(dto.getReviewRemark())
                ? dto.getReviewRemark().trim() : null;

        UpdateWrapper<CareStaffLeaveApplication> uw = new UpdateWrapper<>();
        uw.eq("id", id)
                .eq("status", CareLeaveStatuses.PENDING)
                .set("status", CareLeaveStatuses.REJECTED)
                .set("reviewed_by", user.getUserId())
                .set("reviewed_at", now)
                .set("review_remark", remark);
        int updated = leaveMapper.update(null, uw);
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "申请状态已变更，请刷新后重试");
        }

        operationLogService.record("care", "LEAVE_REJECT", "care_staff_leave_application",
                String.valueOf(id), "SUCCESS", request);
    }

    /**
     * 受影响：该护理员 + CONFIRMED + 时间重叠 + 尚未开始服务。
     * 处理：careStaffId=null，status 回退 PENDING（复用「未分配」+ confirm 再分配）。
     */
    private int releaseAffectedOrders(CareStaffLeaveApplication leave) {
        LocalDateTime leaveStart = leave.getStartTime();
        LocalDateTime leaveEnd = leave.getEndTime();
        LocalDateTime now = LocalDateTime.now();

        List<CareServiceOrder> orders = careServiceOrderMapper.selectList(new LambdaQueryWrapper<CareServiceOrder>()
                .eq(CareServiceOrder::getCareStaffId, leave.getCareStaffId())
                .eq(CareServiceOrder::getStatus, CareOrderStatuses.CONFIRMED)
                .lt(CareServiceOrder::getScheduledStartTime, leaveEnd)
                .gt(CareServiceOrder::getScheduledEndTime, leaveStart));

        int count = 0;
        for (CareServiceOrder order : orders) {
            // 已开始/进行中的不处理（CONFIRMED 且开始时间已过也不强制拆；IN_SERVICE 本就不在查询中）
            if (order.getScheduledStartTime() != null && !order.getScheduledStartTime().isAfter(now)) {
                continue;
            }
            // MP 默认不写 null 字段，须用 UpdateWrapper 显式解除 care_staff_id
            UpdateWrapper<CareServiceOrder> ouw = new UpdateWrapper<>();
            ouw.eq("id", order.getId())
                    .eq("status", CareOrderStatuses.CONFIRMED)
                    .eq("care_staff_id", leave.getCareStaffId())
                    .set("care_staff_id", null)
                    .set("status", CareOrderStatuses.PENDING);
            int n = careServiceOrderMapper.update(null, ouw);
            if (n == 1) {
                count++;
            }
        }
        return count;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始/结束时间不能为空");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
    }

    private void assertNoLeaveOverlap(Long careStaffId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<CareStaffLeaveApplication> existing = leaveMapper.selectList(new LambdaQueryWrapper<CareStaffLeaveApplication>()
                .eq(CareStaffLeaveApplication::getCareStaffId, careStaffId)
                .in(CareStaffLeaveApplication::getStatus,
                        Arrays.asList(CareLeaveStatuses.PENDING, CareLeaveStatuses.APPROVED))
                .ne(excludeId != null, CareStaffLeaveApplication::getId, excludeId)
                .lt(CareStaffLeaveApplication::getStartTime, end)
                .gt(CareStaffLeaveApplication::getEndTime, start));
        if (!existing.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "该时段已存在待审批或已批准的请假申请");
        }
    }

    private CareStaffLeaveApplication requireLeave(Long id) {
        CareStaffLeaveApplication leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "请假申请不存在");
        }
        return leave;
    }

    private void assertCanView(LoginUser user, CareStaffLeaveApplication leave) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            CareStaff self = careStaffIdentityService.requireCurrentCareStaff();
            if (Objects.equals(leave.getCareStaffId(), self.getId())) {
                return;
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该请假申请");
    }

    private CareStaffLeaveVO toVo(CareStaffLeaveApplication leave) {
        CareStaffLeaveVO vo = new CareStaffLeaveVO();
        vo.setId(leave.getId());
        vo.setCareStaffId(leave.getCareStaffId());
        vo.setStartTime(leave.getStartTime());
        vo.setEndTime(leave.getEndTime());
        vo.setReason(leave.getReason());
        vo.setStatus(leave.getStatus());
        vo.setReviewedBy(leave.getReviewedBy());
        vo.setReviewedAt(leave.getReviewedAt());
        vo.setReviewRemark(leave.getReviewRemark());
        vo.setAffectedOrderCount(leave.getAffectedOrderCount());
        vo.setCreatedAt(leave.getCreatedAt());
        vo.setUpdatedAt(leave.getUpdatedAt());

        if (leave.getCareStaffId() != null) {
            CareStaff staff = careStaffMapper.selectById(leave.getCareStaffId());
            if (staff != null) {
                vo.setCareStaffName(staff.getName());
                vo.setEmployeeNo(staff.getEmployeeNo());
            }
        }
        if (leave.getReviewedBy() != null) {
            SysUser reviewer = sysUserMapper.selectById(leave.getReviewedBy());
            if (reviewer != null) {
                vo.setReviewedByName(StringUtils.hasText(reviewer.getRealName())
                        ? reviewer.getRealName() : reviewer.getUsername());
            }
        }
        return vo;
    }
}
