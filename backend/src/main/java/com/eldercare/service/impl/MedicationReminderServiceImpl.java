package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.MedicationStatuses;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderMedication;
import com.eldercare.entity.ElderMedicationTime;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMedicationMapper;
import com.eldercare.mapper.ElderMedicationTimeMapper;
import com.eldercare.service.MedicationReminderService;
import com.eldercare.vo.MedicationReminderVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicationReminderServiceImpl implements MedicationReminderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HHmm");

    private final ElderMedicationMapper medicationMapper;
    private final ElderMedicationTimeMapper timeMapper;
    private final ElderMapper elderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public MedicationReminderServiceImpl(ElderMedicationMapper medicationMapper,
                                         ElderMedicationTimeMapper timeMapper,
                                         ElderMapper elderMapper,
                                         StringRedisTemplate stringRedisTemplate) {
        this.medicationMapper = medicationMapper;
        this.timeMapper = timeMapper;
        this.elderMapper = elderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<MedicationReminderVO> listForElders(Collection<Long> elderIds, LocalDateTime now) {
        if (elderIds == null || elderIds.isEmpty() || now == null) {
            return List.of();
        }
        LocalDate today = now.toLocalDate();
        LocalTime clock = now.toLocalTime().withSecond(0).withNano(0);
        List<ElderMedication> plans = medicationMapper.selectList(new LambdaQueryWrapper<ElderMedication>()
                .in(ElderMedication::getElderId, elderIds)
                .eq(ElderMedication::getStatus, MedicationStatuses.ACTIVE)
                .le(ElderMedication::getStartDate, today)
                .ge(ElderMedication::getEndDate, today));
        if (plans.isEmpty()) {
            return List.of();
        }
        plans = plans.stream()
                .filter(p -> MedicationStatuses.ACTIVE.equals(p.getStatus()))
                .filter(p -> p.getStartDate() != null && !p.getStartDate().isAfter(today))
                .filter(p -> p.getEndDate() != null && !p.getEndDate().isBefore(today))
                .toList();
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = plans.stream().map(ElderMedication::getId).toList();
        List<ElderMedicationTime> times = timeMapper.selectList(new LambdaQueryWrapper<ElderMedicationTime>()
                .in(ElderMedicationTime::getMedicationId, planIds)
                .orderByAsc(ElderMedicationTime::getDoseTime));
        Map<Long, List<LocalTime>> timeMap = new HashMap<>();
        for (ElderMedicationTime time : times) {
            timeMap.computeIfAbsent(time.getMedicationId(), k -> new ArrayList<>()).add(time.getDoseTime());
        }
        Map<Long, String> names = new HashMap<>();
        List<MedicationReminderVO> result = new ArrayList<>();
        for (ElderMedication plan : plans) {
            List<LocalTime> slots = timeMap.getOrDefault(plan.getId(), List.of());
            for (LocalTime slot : slots) {
                LocalTime slotClock = slot.withSecond(0).withNano(0);
                MedicationReminderVO vo = new MedicationReminderVO();
                vo.setMedicationId(plan.getId());
                vo.setElderId(plan.getElderId());
                vo.setElderName(names.computeIfAbsent(plan.getElderId(), this::elderName));
                vo.setMedicineName(plan.getMedicineName());
                vo.setDosage(plan.getDosage());
                vo.setDosageUnit(plan.getDosageUnit());
                vo.setUsageMethod(plan.getUsageMethod());
                vo.setRemindDate(today);
                vo.setDoseTime(slotClock);
                boolean due = !slotClock.isAfter(clock);
                vo.setRemindStatus(due ? MedicationStatuses.REMIND_DUE : MedicationStatuses.REMIND_PENDING);
                vo.setFirstMark(due && markFirst(plan.getId(), today, slotClock, now));
                result.add(vo);
            }
        }
        result.sort(Comparator.comparing(MedicationReminderVO::getDoseTime)
                .thenComparing(MedicationReminderVO::getElderName, Comparator.nullsLast(String::compareTo))
                .thenComparing(MedicationReminderVO::getMedicationId));
        return result;
    }

    /**
     * 同一计划、同一天、同一时刻只写入一次 Redis。失败或已存在都不影响列表仍由数据库算出。
     */
    private boolean markFirst(Long medicationId, LocalDate day, LocalTime slot, LocalDateTime now) {
        String key = "eldercare:med:slot:" + medicationId + ":" + day.format(DAY) + ":" + slot.format(HM);
        Duration ttl = Duration.between(now, day.plusDays(1).atStartOfDay());
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }
        try {
            Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(created);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String elderName(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        return elder == null ? null : elder.getName();
    }
}
