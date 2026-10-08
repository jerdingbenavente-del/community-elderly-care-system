package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.HealthIndicators;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.HealthWarningHandleDTO;
import com.eldercare.dto.HealthWarningQueryDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.HealthRecord;
import com.eldercare.entity.HealthThreshold;
import com.eldercare.entity.HealthWarning;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.HealthWarningMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.HealthThresholdService;
import com.eldercare.service.HealthWarningService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.HealthWarningVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthWarningServiceImpl implements HealthWarningService {

    private final HealthWarningMapper healthWarningMapper;
    private final HealthThresholdService healthThresholdService;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public HealthWarningServiceImpl(HealthWarningMapper healthWarningMapper,
                                    HealthThresholdService healthThresholdService,
                                    ElderMapper elderMapper,
                                    SysUserMapper sysUserMapper,
                                    DataPermissionService dataPermissionService,
                                    OperationLogService operationLogService) {
        this.healthWarningMapper = healthWarningMapper;
        this.healthThresholdService = healthThresholdService;
        this.elderMapper = elderMapper;
        this.sysUserMapper = sysUserMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public void generateWarningsForRecord(HealthRecord record) {
        Map<String, HealthThreshold> map = healthThresholdService.loadEnabledThresholdMap();
        List<HealthWarning> warnings = healthThresholdService.evaluate(record, map);
        for (HealthWarning warning : warnings) {
            warning.setHealthRecordId(record.getId());
            warning.setElderId(record.getElderId());
            healthWarningMapper.insert(warning);
        }
    }

    @Override
    public void refreshUnhandledWarnings(HealthRecord record) {
        List<HealthWarning> unhandled = healthWarningMapper.selectList(new LambdaQueryWrapper<HealthWarning>()
                .eq(HealthWarning::getHealthRecordId, record.getId())
                .eq(HealthWarning::getStatus, HealthIndicators.STATUS_UNHANDLED));
        for (HealthWarning warning : unhandled) {
            healthWarningMapper.deleteById(warning.getId());
        }
        generateWarningsForRecord(record);
    }

    @Override
    public PageResult<HealthWarningVO> page(HealthWarningQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        Page<HealthWarning> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<HealthWarning> wrapper = buildWrapper(query);
        Page<HealthWarning> result = healthWarningMapper.selectPage(page, wrapper);
        List<HealthWarningVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public HealthWarningVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        return toVo(requireWarning(id));
    }

    @Override
    @Transactional
    public void handle(Long id, HealthWarningHandleDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        HealthWarning warning = requireWarning(id);
        if (HealthIndicators.STATUS_HANDLED.equals(warning.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该预警已处理");
        }
        Long userId = SecurityUtils.requireUserId();
        warning.setStatus(HealthIndicators.STATUS_HANDLED);
        warning.setHandledBy(userId);
        warning.setHandledAt(LocalDateTime.now());
        warning.setHandlingResult(dto.getHandlingResult().trim());
        healthWarningMapper.updateById(warning);
        operationLogService.record("health", "WARNING_HANDLE", "health_warning",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public List<HealthWarningVO> listByElderForFamily(Long elderId) {
        Long userId = SecurityUtils.requireUserId();
        dataPermissionService.checkFamilyAccess(userId, elderId);
        return healthWarningMapper.selectList(new LambdaQueryWrapper<HealthWarning>()
                        .eq(HealthWarning::getElderId, elderId)
                        .orderByDesc(HealthWarning::getGeneratedAt))
                .stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public HealthWarningVO getByIdForFamily(Long id) {
        Long userId = SecurityUtils.requireUserId();
        HealthWarning warning = requireWarning(id);
        dataPermissionService.checkFamilyAccess(userId, warning.getElderId());
        return toVo(warning);
    }

    @Override
    public List<HealthWarningVO> listByRecordId(Long healthRecordId) {
        return healthWarningMapper.selectList(new LambdaQueryWrapper<HealthWarning>()
                        .eq(HealthWarning::getHealthRecordId, healthRecordId)
                        .orderByAsc(HealthWarning::getId))
                .stream().map(this::toVo).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<HealthWarning> buildWrapper(HealthWarningQueryDTO query) {
        LambdaQueryWrapper<HealthWarning> wrapper = new LambdaQueryWrapper<>();
        if (query.getElderId() != null) {
            wrapper.eq(HealthWarning::getElderId, query.getElderId());
        }
        if (query.getHealthRecordId() != null) {
            wrapper.eq(HealthWarning::getHealthRecordId, query.getHealthRecordId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(HealthWarning::getStatus, query.getStatus().trim());
        }
        if (StringUtils.hasText(query.getIndicator())) {
            wrapper.eq(HealthWarning::getIndicator, query.getIndicator().trim());
        }
        wrapper.orderByDesc(HealthWarning::getGeneratedAt).orderByDesc(HealthWarning::getId);
        return wrapper;
    }

    private HealthWarning requireWarning(Long id) {
        HealthWarning warning = healthWarningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "健康预警不存在");
        }
        return warning;
    }

    private HealthWarningVO toVo(HealthWarning warning) {
        HealthWarningVO vo = new HealthWarningVO();
        vo.setId(warning.getId());
        vo.setElderId(warning.getElderId());
        vo.setHealthRecordId(warning.getHealthRecordId());
        vo.setIndicator(warning.getIndicator());
        vo.setActualValue(warning.getActualValue());
        vo.setThresholdDesc(warning.getThresholdDesc());
        vo.setDirection(warning.getDirection());
        vo.setWarningLevel(warning.getWarningLevel());
        vo.setStatus(warning.getStatus());
        vo.setGeneratedAt(warning.getGeneratedAt());
        vo.setHandledBy(warning.getHandledBy());
        vo.setHandledAt(warning.getHandledAt());
        vo.setHandlingResult(warning.getHandlingResult());
        Elder elder = elderMapper.selectById(warning.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        if (warning.getHandledBy() != null) {
            SysUser user = sysUserMapper.selectById(warning.getHandledBy());
            if (user != null) {
                vo.setHandledByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }
        return vo;
    }
}
