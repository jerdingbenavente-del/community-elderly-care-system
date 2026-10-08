package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.HealthRecordCreateDTO;
import com.eldercare.dto.HealthRecordQueryDTO;
import com.eldercare.dto.HealthRecordUpdateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.HealthRecord;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.HealthRecordMapper;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.HealthRecordService;
import com.eldercare.service.HealthWarningService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.HealthRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;
    private final ElderMapper elderMapper;
    private final HealthWarningService healthWarningService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public HealthRecordServiceImpl(HealthRecordMapper healthRecordMapper,
                                   ElderMapper elderMapper,
                                   HealthWarningService healthWarningService,
                                   DataPermissionService dataPermissionService,
                                   OperationLogService operationLogService) {
        this.healthRecordMapper = healthRecordMapper;
        this.elderMapper = elderMapper;
        this.healthWarningService = healthWarningService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(HealthRecordCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = requireActiveElder(dto.getElderId());
        validateIndicators(dto.getSystolicPressure(), dto.getDiastolicPressure(),
                dto.getBloodGlucose(), dto.getBodyTemperature(), dto.getHeartRate());

        HealthRecord record = new HealthRecord();
        record.setElderId(elder.getId());
        record.setMeasuredAt(dto.getMeasuredAt());
        fillIndicators(record, dto.getSystolicPressure(), dto.getDiastolicPressure(),
                dto.getBloodGlucose(), dto.getBodyTemperature(), dto.getHeartRate(), dto.getRemark());
        record.setRecordedBy(SecurityUtils.requireUserId());
        healthRecordMapper.insert(record);

        healthWarningService.generateWarningsForRecord(record);
        operationLogService.record("health", "HEALTH_RECORD_CREATE", "health_record",
                String.valueOf(record.getId()), "SUCCESS", request);
        return record.getId();
    }

    @Override
    @Transactional
    public void update(Long id, HealthRecordUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        HealthRecord record = requireRecord(id);
        validateIndicators(dto.getSystolicPressure(), dto.getDiastolicPressure(),
                dto.getBloodGlucose(), dto.getBodyTemperature(), dto.getHeartRate());

        record.setMeasuredAt(dto.getMeasuredAt());
        fillIndicators(record, dto.getSystolicPressure(), dto.getDiastolicPressure(),
                dto.getBloodGlucose(), dto.getBodyTemperature(), dto.getHeartRate(), dto.getRemark());
        healthRecordMapper.updateById(record);

        healthWarningService.refreshUnhandledWarnings(record);
        operationLogService.record("health", "HEALTH_RECORD_UPDATE", "health_record",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireRecord(id);
        healthRecordMapper.deleteById(id);
        operationLogService.record("health", "HEALTH_RECORD_DELETE", "health_record",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public HealthRecordVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        HealthRecord record = requireRecord(id);
        HealthRecordVO vo = toVo(record);
        vo.setWarnings(healthWarningService.listByRecordId(id));
        return vo;
    }

    @Override
    public PageResult<HealthRecordVO> page(HealthRecordQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        Page<HealthRecord> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<HealthRecord> wrapper = buildWrapper(query);
        wrapper.orderByDesc(HealthRecord::getMeasuredAt).orderByDesc(HealthRecord::getId);
        Page<HealthRecord> result = healthRecordMapper.selectPage(page, wrapper);
        List<HealthRecordVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<HealthRecordVO> trend(Long elderId, HealthRecordQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireElderExists(elderId);
        HealthRecordQueryDTO q = query == null ? new HealthRecordQueryDTO() : query;
        q.setElderId(elderId);
        LambdaQueryWrapper<HealthRecord> wrapper = buildWrapper(q);
        wrapper.orderByAsc(HealthRecord::getMeasuredAt).orderByAsc(HealthRecord::getId);
        return healthRecordMapper.selectList(wrapper).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public List<HealthRecordVO> listForFamily(Long elderId, HealthRecordQueryDTO query) {
        Long userId = SecurityUtils.requireUserId();
        dataPermissionService.checkFamilyAccess(userId, elderId);
        HealthRecordQueryDTO q = query == null ? new HealthRecordQueryDTO() : query;
        q.setElderId(elderId);
        LambdaQueryWrapper<HealthRecord> wrapper = buildWrapper(q);
        wrapper.orderByDesc(HealthRecord::getMeasuredAt);
        long limit = Math.min(Math.max(q.getSize(), 1), 100);
        wrapper.last("LIMIT " + limit);
        return healthRecordMapper.selectList(wrapper).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public HealthRecordVO getByIdForFamily(Long id) {
        Long userId = SecurityUtils.requireUserId();
        HealthRecord record = requireRecord(id);
        dataPermissionService.checkFamilyAccess(userId, record.getElderId());
        HealthRecordVO vo = toVo(record);
        vo.setWarnings(healthWarningService.listByRecordId(id));
        return vo;
    }

    private LambdaQueryWrapper<HealthRecord> buildWrapper(HealthRecordQueryDTO query) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getElderId() != null) {
            wrapper.eq(HealthRecord::getElderId, query.getElderId());
        }
        if (query.getMeasuredFrom() != null) {
            wrapper.ge(HealthRecord::getMeasuredAt, query.getMeasuredFrom());
        }
        if (query.getMeasuredTo() != null) {
            wrapper.le(HealthRecord::getMeasuredAt, query.getMeasuredTo());
        }
        return wrapper;
    }

    private Elder requireActiveElder(Long elderId) {
        Elder elder = requireElderExists(elderId);
        if (elder.getStatus() == null || elder.getStatus() != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "老人已停用，不能录入健康记录");
        }
        return elder;
    }

    private Elder requireElderExists(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private HealthRecord requireRecord(Long id) {
        HealthRecord record = healthRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "健康记录不存在");
        }
        return record;
    }

    /**
     * 参数合法性校验（不等于阈值异常判断）。
     */
    void validateIndicators(Integer systolic, Integer diastolic, BigDecimal glucose,
                            BigDecimal temperature, Integer heartRate) {
        boolean hasAny = systolic != null || diastolic != null || glucose != null
                || temperature != null || heartRate != null;
        if (!hasAny) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少填写一项健康指标");
        }
        if (systolic != null && (systolic < 40 || systolic > 250)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "收缩压数值不合法");
        }
        if (diastolic != null && (diastolic < 20 || diastolic > 150)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "舒张压数值不合法");
        }
        if (systolic != null && diastolic != null && systolic < diastolic) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "收缩压不能小于舒张压");
        }
        if (glucose != null && (glucose.compareTo(new BigDecimal("0.5")) < 0
                || glucose.compareTo(new BigDecimal("40")) > 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "血糖数值不合法");
        }
        if (temperature != null && (temperature.compareTo(new BigDecimal("30.0")) < 0
                || temperature.compareTo(new BigDecimal("45.0")) > 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "体温数值不合法");
        }
        if (heartRate != null && (heartRate < 20 || heartRate > 250)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "心率数值不合法");
        }
    }

    private void fillIndicators(HealthRecord record, Integer systolic, Integer diastolic,
                                BigDecimal glucose, BigDecimal temperature, Integer heartRate, String remark) {
        record.setSystolicPressure(systolic);
        record.setDiastolicPressure(diastolic);
        record.setBloodGlucose(glucose);
        record.setBodyTemperature(temperature);
        record.setHeartRate(heartRate);
        record.setRemark(remark);
    }

    private HealthRecordVO toVo(HealthRecord record) {
        HealthRecordVO vo = new HealthRecordVO();
        vo.setId(record.getId());
        vo.setElderId(record.getElderId());
        vo.setMeasuredAt(record.getMeasuredAt());
        vo.setSystolicPressure(record.getSystolicPressure());
        vo.setDiastolicPressure(record.getDiastolicPressure());
        vo.setBloodGlucose(record.getBloodGlucose());
        vo.setBodyTemperature(record.getBodyTemperature());
        vo.setHeartRate(record.getHeartRate());
        vo.setRecordedBy(record.getRecordedBy());
        vo.setRemark(record.getRemark());
        vo.setCreatedAt(record.getCreatedAt());
        Elder elder = elderMapper.selectById(record.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        return vo;
    }
}
