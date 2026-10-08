package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareStaffCreateDTO;
import com.eldercare.dto.CareStaffQueryDTO;
import com.eldercare.dto.CareStaffUpdateDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.CareStaffSchedule;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.CareStaffScheduleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.CareStaffService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareStaffVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareStaffServiceImpl implements CareStaffService {

    private final CareStaffMapper careStaffMapper;
    private final SysUserMapper sysUserMapper;
    private final CareServiceOrderMapper careServiceOrderMapper;
    private final CareStaffScheduleMapper careStaffScheduleMapper;
    private final CareStaffIdentityService careStaffIdentityService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public CareStaffServiceImpl(CareStaffMapper careStaffMapper,
                                SysUserMapper sysUserMapper,
                                CareServiceOrderMapper careServiceOrderMapper,
                                CareStaffScheduleMapper careStaffScheduleMapper,
                                CareStaffIdentityService careStaffIdentityService,
                                DataPermissionService dataPermissionService,
                                OperationLogService operationLogService) {
        this.careStaffMapper = careStaffMapper;
        this.sysUserMapper = sysUserMapper;
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.careStaffScheduleMapper = careStaffScheduleMapper;
        this.careStaffIdentityService = careStaffIdentityService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(CareStaffCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        careStaffIdentityService.validateUserBinding(dto.getUserId(), null);

        String employeeNo = dto.getEmployeeNo().trim();
        assertEmployeeNoUnique(employeeNo, null);

        CareStaff staff = new CareStaff();
        staff.setUserId(dto.getUserId());
        staff.setEmployeeNo(employeeNo);
        staff.setName(dto.getName().trim());
        staff.setGender(dto.getGender());
        staff.setPhone(dto.getPhone());
        staff.setPosition(dto.getPosition());
        staff.setRemark(dto.getRemark());
        staff.setStatus(CareStaffStatuses.ENABLED);
        careStaffMapper.insert(staff);

        operationLogService.record("care_staff", "CARE_STAFF_CREATE", "care_staff",
                String.valueOf(staff.getId()), "SUCCESS", request);
        return staff.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CareStaffUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        CareStaff staff = requireStaff(id);
        staff.setName(dto.getName().trim());
        staff.setGender(dto.getGender());
        staff.setPhone(dto.getPhone());
        staff.setPosition(dto.getPosition());
        staff.setRemark(dto.getRemark());
        careStaffMapper.updateById(staff);
        operationLogService.record("care_staff", "CARE_STAFF_UPDATE", "care_staff",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void enable(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        CareStaff staff = requireStaff(id);
        careStaffIdentityService.validateUserBinding(staff.getUserId(), staff.getId());
        staff.setStatus(CareStaffStatuses.ENABLED);
        careStaffMapper.updateById(staff);
        operationLogService.record("care_staff", "CARE_STAFF_ENABLE", "care_staff",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void disable(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        CareStaff staff = requireStaff(id);
        Long active = careServiceOrderMapper.selectCount(new LambdaQueryWrapper<CareServiceOrder>()
                .eq(CareServiceOrder::getCareStaffId, id)
                .in(CareServiceOrder::getStatus,
                        Arrays.asList(CareOrderStatuses.CONFIRMED, CareOrderStatuses.IN_SERVICE)));
        if (active != null && active > 0) {
            Long inService = careServiceOrderMapper.selectCount(new LambdaQueryWrapper<CareServiceOrder>()
                    .eq(CareServiceOrder::getCareStaffId, id)
                    .eq(CareServiceOrder::getStatus, CareOrderStatuses.IN_SERVICE));
            if (inService != null && inService > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "护理员存在正在执行的服务订单，不能停用");
            }
            throw new BusinessException(ResultCode.CONFLICT, "护理员存在已确认待执行的服务订单，不能停用");
        }
        staff.setStatus(CareStaffStatuses.DISABLED);
        careStaffMapper.updateById(staff);
        operationLogService.record("care_staff", "CARE_STAFF_DISABLE", "care_staff",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireAdmin();
        requireStaff(id);
        Long orderCount = careServiceOrderMapper.selectCount(new LambdaQueryWrapper<CareServiceOrder>()
                .eq(CareServiceOrder::getCareStaffId, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "该护理员存在历史服务订单或排班记录，不能删除，请使用停用功能。");
        }
        Long scheduleCount = careStaffScheduleMapper.selectCount(new LambdaQueryWrapper<CareStaffSchedule>()
                .eq(CareStaffSchedule::getCareStaffId, id));
        if (scheduleCount != null && scheduleCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "该护理员存在历史服务订单或排班记录，不能删除，请使用停用功能。");
        }
        careStaffMapper.deleteById(id);
        operationLogService.record("care_staff", "CARE_STAFF_DELETE", "care_staff",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public CareStaffVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();
        CareStaff staff = requireStaff(id);
        assertCanView(user, staff);
        return toVo(staff);
    }

    @Override
    public CareStaffVO me() {
        dataPermissionService.denyFamilyOnAdminApi();
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        return toVo(staff);
    }

    @Override
    public PageResult<CareStaffVO> page(CareStaffQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();

        Page<CareStaff> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareStaff> wrapper = new LambdaQueryWrapper<>();
        if (user.hasRole("ADMIN")) {
            if (StringUtils.hasText(query.getEmployeeNo())) {
                wrapper.eq(CareStaff::getEmployeeNo, query.getEmployeeNo().trim());
            }
            if (StringUtils.hasText(query.getName())) {
                wrapper.like(CareStaff::getName, query.getName().trim());
            }
            if (StringUtils.hasText(query.getPhone())) {
                wrapper.eq(CareStaff::getPhone, query.getPhone().trim());
            }
            if (query.getStatus() != null) {
                wrapper.eq(CareStaff::getStatus, query.getStatus());
            }
            if (query.getUserId() != null) {
                wrapper.eq(CareStaff::getUserId, query.getUserId());
            }
        } else if (user.hasRole("CARE_STAFF")) {
            CareStaff self = careStaffIdentityService.requireByUserId(user.getUserId());
            wrapper.eq(CareStaff::getId, self.getId());
        } else {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看护理员");
        }
        wrapper.orderByDesc(CareStaff::getId);
        Page<CareStaff> result = careStaffMapper.selectPage(page, wrapper);
        List<CareStaffVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void assertCanView(LoginUser user, CareStaff staff) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            CareStaff self = careStaffIdentityService.requireByUserId(user.getUserId());
            if (!self.getId().equals(staff.getId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问其他护理员资料");
            }
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看护理员");
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可管理护理员");
        }
    }

    private CareStaff requireStaff(Long id) {
        CareStaff staff = careStaffMapper.selectById(id);
        if (staff == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护理员不存在");
        }
        return staff;
    }

    private void assertEmployeeNoUnique(String employeeNo, Long excludeId) {
        LambdaQueryWrapper<CareStaff> wrapper = new LambdaQueryWrapper<CareStaff>()
                .eq(CareStaff::getEmployeeNo, employeeNo);
        if (excludeId != null) {
            wrapper.ne(CareStaff::getId, excludeId);
        }
        Long count = careStaffMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "护理员工号重复");
        }
    }

    private CareStaffVO toVo(CareStaff staff) {
        CareStaffVO vo = new CareStaffVO();
        vo.setId(staff.getId());
        vo.setUserId(staff.getUserId());
        vo.setEmployeeNo(staff.getEmployeeNo());
        vo.setName(staff.getName());
        vo.setGender(staff.getGender());
        vo.setPhone(staff.getPhone());
        vo.setPosition(staff.getPosition());
        vo.setRemark(staff.getRemark());
        vo.setStatus(staff.getStatus());
        vo.setCreatedAt(staff.getCreatedAt());
        vo.setUpdatedAt(staff.getUpdatedAt());
        if (staff.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(staff.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
            }
        }
        return vo;
    }
}
