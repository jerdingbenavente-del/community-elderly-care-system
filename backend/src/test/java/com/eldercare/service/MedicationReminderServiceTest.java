package com.eldercare.service;

import com.eldercare.common.MedicationStatuses;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderMedication;
import com.eldercare.entity.ElderMedicationTime;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMedicationMapper;
import com.eldercare.mapper.ElderMedicationTimeMapper;
import com.eldercare.service.impl.MedicationReminderServiceImpl;
import com.eldercare.vo.MedicationReminderVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationReminderServiceTest {

    @Mock
    private ElderMedicationMapper medicationMapper;
    @Mock
    private ElderMedicationTimeMapper timeMapper;
    @Mock
    private ElderMapper elderMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MedicationReminderServiceImpl service;

    private ElderMedication plan(LocalDate start, LocalDate end, String status) {
        ElderMedication row = new ElderMedication();
        row.setId(7L);
        row.setElderId(18L);
        row.setMedicineName("测试维生素");
        row.setDosage("1");
        row.setDosageUnit("片");
        row.setUsageMethod("口服");
        row.setStartDate(start);
        row.setEndDate(end);
        row.setStatus(status);
        return row;
    }

    private void times(LocalTime time) {
        ElderMedicationTime slot = new ElderMedicationTime();
        slot.setMedicationId(7L);
        slot.setDoseTime(time);
        when(timeMapper.selectList(any())).thenReturn(List.of(slot));
        Elder elder = new Elder();
        elder.setId(18L);
        elder.setName("王虎");
        when(elderMapper.selectById(18L)).thenReturn(elder);
    }

    @Test
    void beforeStartDate_noReminder() {
        LocalDate today = LocalDate.of(2026, 10, 1);
        when(medicationMapper.selectList(any())).thenReturn(List.of(
                plan(today.plusDays(1), today.plusDays(3), MedicationStatuses.ACTIVE)));
        List<MedicationReminderVO> list = service.listForElders(List.of(18L), today.atTime(9, 0));
        assertTrue(list.isEmpty());
    }

    @Test
    void afterEndDate_noReminder() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        when(medicationMapper.selectList(any())).thenReturn(List.of(
                plan(today.minusDays(7), today.minusDays(1), MedicationStatuses.ACTIVE)));
        assertTrue(service.listForElders(List.of(18L), today.atTime(9, 0)).isEmpty());
    }

    @Test
    void inactive_noReminder() {
        LocalDate today = LocalDate.of(2026, 10, 2);
        when(medicationMapper.selectList(any())).thenReturn(List.of(
                plan(today.minusDays(1), today.plusDays(1), MedicationStatuses.INACTIVE)));
        assertTrue(service.listForElders(List.of(18L), today.atTime(9, 0)).isEmpty());
    }

    @Test
    void dueWhenClockReachesSlot_andNotBefore() {
        LocalDate today = LocalDate.of(2026, 10, 2);
        when(medicationMapper.selectList(any())).thenReturn(List.of(
                plan(today, today.plusDays(1), MedicationStatuses.ACTIVE)));
        times(LocalTime.of(9, 0));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        MedicationReminderVO early = service.listForElders(List.of(18L), LocalDateTime.of(today, LocalTime.of(8, 59))).get(0);
        assertEquals(MedicationStatuses.REMIND_PENDING, early.getRemindStatus());
        assertFalse(early.isFirstMark());

        MedicationReminderVO due = service.listForElders(List.of(18L), LocalDateTime.of(today, LocalTime.of(9, 0))).get(0);
        assertEquals(MedicationStatuses.REMIND_DUE, due.getRemindStatus());
        assertTrue(due.isFirstMark());
    }

    @Test
    void sameSlotMarkedOnce() {
        LocalDate today = LocalDate.of(2026, 10, 2);
        when(medicationMapper.selectList(any())).thenReturn(List.of(
                plan(today, today, MedicationStatuses.ACTIVE)));
        times(LocalTime.of(9, 0));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true, false);

        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(9, 5));
        List<MedicationReminderVO> first = service.listForElders(List.of(18L), now);
        List<MedicationReminderVO> second = service.listForElders(List.of(18L), now);
        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertTrue(first.get(0).isFirstMark());
        assertFalse(second.get(0).isFirstMark());
        assertEquals(first.get(0).getMedicationId(), second.get(0).getMedicationId());
        assertEquals(first.get(0).getDoseTime(), second.get(0).getDoseTime());
    }
}
