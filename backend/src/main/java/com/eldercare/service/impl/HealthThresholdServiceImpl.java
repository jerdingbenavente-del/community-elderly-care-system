package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.HealthIndicators;
import com.eldercare.entity.HealthRecord;
import com.eldercare.entity.HealthThreshold;
import com.eldercare.entity.HealthWarning;
import com.eldercare.mapper.HealthThresholdMapper;
import com.eldercare.service.HealthThresholdService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HealthThresholdServiceImpl implements HealthThresholdService {

    private final HealthThresholdMapper healthThresholdMapper;

    public HealthThresholdServiceImpl(HealthThresholdMapper healthThresholdMapper) {
        this.healthThresholdMapper = healthThresholdMapper;
    }

    @Override
    public Map<String, HealthThreshold> loadEnabledThresholdMap() {
        List<HealthThreshold> list = healthThresholdMapper.selectList(new LambdaQueryWrapper<HealthThreshold>()
                .eq(HealthThreshold::getStatus, 1));
        return list.stream().collect(Collectors.toMap(HealthThreshold::getIndicator, Function.identity(), (a, b) -> a));
    }

    @Override
    public List<HealthWarning> evaluate(HealthRecord record, Map<String, HealthThreshold> thresholdMap) {
        List<HealthWarning> warnings = new ArrayList<>();
        if (record.getSystolicPressure() != null) {
            addIfAbnormal(warnings, record, thresholdMap.get(HealthIndicators.SYSTOLIC_PRESSURE),
                    HealthIndicators.SYSTOLIC_PRESSURE, BigDecimal.valueOf(record.getSystolicPressure()));
        }
        if (record.getDiastolicPressure() != null) {
            addIfAbnormal(warnings, record, thresholdMap.get(HealthIndicators.DIASTOLIC_PRESSURE),
                    HealthIndicators.DIASTOLIC_PRESSURE, BigDecimal.valueOf(record.getDiastolicPressure()));
        }
        if (record.getBloodGlucose() != null) {
            addIfAbnormal(warnings, record, thresholdMap.get(HealthIndicators.BLOOD_GLUCOSE),
                    HealthIndicators.BLOOD_GLUCOSE, record.getBloodGlucose());
        }
        if (record.getBodyTemperature() != null) {
            addIfAbnormal(warnings, record, thresholdMap.get(HealthIndicators.TEMPERATURE),
                    HealthIndicators.TEMPERATURE, record.getBodyTemperature());
        }
        if (record.getHeartRate() != null) {
            addIfAbnormal(warnings, record, thresholdMap.get(HealthIndicators.HEART_RATE),
                    HealthIndicators.HEART_RATE, BigDecimal.valueOf(record.getHeartRate()));
        }
        return warnings;
    }

    private void addIfAbnormal(List<HealthWarning> warnings,
                               HealthRecord record,
                               HealthThreshold threshold,
                               String indicator,
                               BigDecimal actual) {
        if (threshold == null || actual == null) {
            return;
        }
        String direction = null;
        if (threshold.getMinValue() != null && actual.compareTo(threshold.getMinValue()) < 0) {
            direction = HealthIndicators.DIRECTION_LOW;
        } else if (threshold.getMaxValue() != null && actual.compareTo(threshold.getMaxValue()) > 0) {
            direction = HealthIndicators.DIRECTION_HIGH;
        }
        if (direction == null) {
            return;
        }
        HealthWarning warning = new HealthWarning();
        warning.setElderId(record.getElderId());
        warning.setHealthRecordId(record.getId());
        warning.setIndicator(indicator);
        warning.setActualValue(actual.stripTrailingZeros().toPlainString());
        warning.setThresholdDesc(buildThresholdDesc(threshold));
        warning.setDirection(direction);
        warning.setWarningLevel(threshold.getWarningLevel() == null ? "WARNING" : threshold.getWarningLevel());
        warning.setStatus(HealthIndicators.STATUS_UNHANDLED);
        warning.setGeneratedAt(LocalDateTime.now());
        warnings.add(warning);
    }

    private String buildThresholdDesc(HealthThreshold threshold) {
        String min = threshold.getMinValue() == null ? "-" : threshold.getMinValue().stripTrailingZeros().toPlainString();
        String max = threshold.getMaxValue() == null ? "-" : threshold.getMaxValue().stripTrailingZeros().toPlainString();
        String unit = threshold.getUnit() == null ? "" : threshold.getUnit();
        return min + "~" + max + (unit.isEmpty() ? "" : " " + unit);
    }

    /**
     * 便于单测：不访问数据库，直接评估。
     */
    public static List<HealthWarning> evaluateStatic(HealthRecord record, Map<String, HealthThreshold> thresholdMap) {
        HealthThresholdServiceImpl helper = new HealthThresholdServiceImpl(null);
        return helper.evaluate(record, thresholdMap == null ? new HashMap<>() : thresholdMap);
    }
}
