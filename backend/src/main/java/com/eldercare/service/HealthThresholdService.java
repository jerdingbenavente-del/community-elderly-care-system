package com.eldercare.service;

import com.eldercare.entity.HealthRecord;
import com.eldercare.entity.HealthThreshold;
import com.eldercare.entity.HealthWarning;

import java.util.List;
import java.util.Map;

public interface HealthThresholdService {

    Map<String, HealthThreshold> loadEnabledThresholdMap();

    /**
     * 根据阈值评估健康记录，返回待入库的预警（不含 id）。
     */
    List<HealthWarning> evaluate(HealthRecord record, Map<String, HealthThreshold> thresholdMap);
}
