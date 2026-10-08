package com.eldercare.service;

import com.eldercare.vo.MedicationReminderVO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MedicationReminderService {

    /**
     * 按数据库计划计算某天提醒。Redis 只标记到点槽位是否首次出现，不作为数据源。
     */
    List<MedicationReminderVO> listForElders(Collection<Long> elderIds, LocalDateTime now);
}
