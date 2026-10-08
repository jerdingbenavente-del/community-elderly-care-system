package com.eldercare.service;

import com.eldercare.entity.HealthRecord;
import com.eldercare.entity.HealthThreshold;
import com.eldercare.entity.HealthWarning;
import com.eldercare.service.impl.HealthThresholdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阈值评估单测（不访问数据库）。
 */
class HealthThresholdServiceTest {

    private HealthThresholdServiceImpl service;
    private Map<String, HealthThreshold> thresholds;

    @BeforeEach
    void setUp() {
        service = new HealthThresholdServiceImpl(null);
        thresholds = new HashMap<>();
        thresholds.put("SYSTOLIC_PRESSURE", rule("SYSTOLIC_PRESSURE", "90", "139"));
        thresholds.put("DIASTOLIC_PRESSURE", rule("DIASTOLIC_PRESSURE", "60", "89"));
        thresholds.put("BLOOD_GLUCOSE", rule("BLOOD_GLUCOSE", "3.9", "7.0"));
        thresholds.put("TEMPERATURE", rule("TEMPERATURE", "36.0", "37.2"));
        thresholds.put("HEART_RATE", rule("HEART_RATE", "60", "100"));
    }

    @Test
    void normalBloodPressure_shouldNotGenerateWarning() {
        HealthRecord record = baseRecord();
        record.setSystolicPressure(120);
        record.setDiastolicPressure(80);
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertTrue(warnings.stream().noneMatch(w -> w.getIndicator().contains("PRESSURE")));
    }

    @Test
    void abnormalSystolic_shouldGenerateWarning() {
        HealthRecord record = baseRecord();
        record.setSystolicPressure(150);
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(1, warnings.size());
        assertEquals("SYSTOLIC_PRESSURE", warnings.get(0).getIndicator());
        assertEquals("HIGH", warnings.get(0).getDirection());
        assertEquals("UNHANDLED", warnings.get(0).getStatus());
    }

    @Test
    void abnormalBloodGlucose_shouldGenerateWarning() {
        HealthRecord record = baseRecord();
        record.setBloodGlucose(new BigDecimal("8.5"));
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(1, warnings.size());
        assertEquals("BLOOD_GLUCOSE", warnings.get(0).getIndicator());
    }

    @Test
    void abnormalTemperature_shouldGenerateWarning() {
        HealthRecord record = baseRecord();
        record.setBodyTemperature(new BigDecimal("38.0"));
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(1, warnings.size());
        assertEquals("TEMPERATURE", warnings.get(0).getIndicator());
    }

    @Test
    void abnormalHeartRate_shouldGenerateWarning() {
        HealthRecord record = baseRecord();
        record.setHeartRate(125);
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(1, warnings.size());
        assertEquals("HEART_RATE", warnings.get(0).getIndicator());
        assertEquals("125", warnings.get(0).getActualValue());
    }

    @Test
    void multipleAbnormalIndicators_shouldGenerateMultipleWarnings() {
        HealthRecord record = baseRecord();
        record.setSystolicPressure(160);
        record.setDiastolicPressure(70);
        record.setBloodGlucose(new BigDecimal("5.0"));
        record.setBodyTemperature(new BigDecimal("36.5"));
        record.setHeartRate(130);
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(2, warnings.size());
        var indicators = warnings.stream().map(HealthWarning::getIndicator).collect(Collectors.toSet());
        assertTrue(indicators.contains("SYSTOLIC_PRESSURE"));
        assertTrue(indicators.contains("HEART_RATE"));
    }

    @Test
    void lowHeartRate_shouldGenerateLowDirectionWarning() {
        HealthRecord record = baseRecord();
        record.setHeartRate(45);
        List<HealthWarning> warnings = service.evaluate(record, thresholds);
        assertEquals(1, warnings.size());
        assertEquals("LOW", warnings.get(0).getDirection());
        assertEquals("UNHANDLED", warnings.get(0).getStatus());
    }

    private HealthRecord baseRecord() {
        HealthRecord record = new HealthRecord();
        record.setId(1L);
        record.setElderId(10L);
        return record;
    }

    private HealthThreshold rule(String indicator, String min, String max) {
        HealthThreshold t = new HealthThreshold();
        t.setIndicator(indicator);
        t.setMinValue(new BigDecimal(min));
        t.setMaxValue(new BigDecimal(max));
        t.setWarningLevel("WARNING");
        t.setUnit("u");
        t.setStatus(1);
        return t;
    }
}
