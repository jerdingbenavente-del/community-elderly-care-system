package com.eldercare.service.impl;

import com.eldercare.common.MedicationStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.common.WeekRanges;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.OperationsStatisticsMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.MedicationReminderService;
import com.eldercare.service.OperationsStatisticsService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.MedicationReminderVO;
import com.eldercare.vo.OperationsStatisticsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationsStatisticsServiceImpl implements OperationsStatisticsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OperationsStatisticsMapper statisticsMapper;
    private final MedicationReminderService medicationReminderService;
    private final DataPermissionService dataPermissionService;

    public OperationsStatisticsServiceImpl(OperationsStatisticsMapper statisticsMapper,
                                           MedicationReminderService medicationReminderService,
                                           DataPermissionService dataPermissionService) {
        this.statisticsMapper = statisticsMapper;
        this.medicationReminderService = medicationReminderService;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    public OperationsStatisticsVO overview(LocalDate dateFrom, LocalDate dateTo) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看运营统计");
        }

        LocalDate today = LocalDate.now();
        LocalDate from = dateFrom;
        LocalDate to = dateTo;
        if (from == null && to == null) {
            from = today.withDayOfMonth(1);
            to = today;
        } else if (from == null) {
            from = to.withDayOfMonth(1);
        } else if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }

        LocalDateTime rangeFrom = from.atStartOfDay();
        LocalDateTime rangeToExclusive = to.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        OperationsStatisticsVO vo = new OperationsStatisticsVO();
        vo.setDateFrom(from.format(DAY));
        vo.setDateTo(to.format(DAY));

        fillElder(vo);
        fillOrders(vo, rangeFrom, rangeToExclusive);
        fillEvaluation(vo, rangeFrom, rangeToExclusive);
        fillCareStaffAndLeave(vo, today, now);
        fillAttendance(vo, today, to, now);
        fillMedication(vo, now);
        fillMenu(vo, today);
        fillActivity(vo, today, now);
        fillTrend(vo, today);

        return vo;
    }

    private void fillElder(OperationsStatisticsVO vo) {
        Map<String, Object> m = nullToEmpty(statisticsMapper.selectElderStats());
        OperationsStatisticsVO.ElderStats elder = vo.getElder();
        elder.setTotal(asLong(m.get("total")));
        elder.setActive(asLong(m.get("active")));
        elder.setInactive(asLong(m.get("inactive")));
        elder.setBoundFamily(asLong(m.get("boundFamily")));
        long unbound = Math.max(0, elder.getActive() - elder.getBoundFamily());
        elder.setUnboundFamily(unbound);
    }

    private void fillOrders(OperationsStatisticsVO vo, LocalDateTime from, LocalDateTime toExclusive) {
        Map<String, Object> m = nullToEmpty(statisticsMapper.selectOrderStats(from, toExclusive));
        OperationsStatisticsVO.OrderStats orders = vo.getOrders();
        orders.setTotal(asLong(m.get("total")));
        orders.setPending(asLong(m.get("pending")));
        orders.setConfirmed(asLong(m.get("confirmed")));
        orders.setInService(asLong(m.get("inService")));
        orders.setCompleted(asLong(m.get("completed")));
        orders.setCancelled(asLong(m.get("cancelled")));
        long effective = orders.getTotal() - orders.getCancelled();
        if (effective <= 0) {
            orders.setCompletedRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        } else {
            orders.setCompletedRate(BigDecimal.valueOf(orders.getCompleted())
                    .divide(BigDecimal.valueOf(effective), 4, RoundingMode.HALF_UP));
        }
    }

    private void fillEvaluation(OperationsStatisticsVO vo, LocalDateTime from, LocalDateTime toExclusive) {
        Map<String, Object> m = nullToEmpty(statisticsMapper.selectEvaluationStats(from, toExclusive));
        OperationsStatisticsVO.EvaluationStats evaluation = vo.getEvaluation();
        evaluation.setTotal(asLong(m.get("total")));
        if (evaluation.getTotal() == 0) {
            evaluation.setAverageScore(null);
        } else {
            evaluation.setAverageScore(asDecimal(m.get("averageScore")));
        }
    }

    private void fillCareStaffAndLeave(OperationsStatisticsVO vo, LocalDate today, LocalDateTime now) {
        Map<String, Object> m = nullToEmpty(statisticsMapper.selectCareStaffStats(today, now));
        OperationsStatisticsVO.CareStaffStats careStaff = vo.getCareStaff();
        careStaff.setTotal(asLong(m.get("total")));
        careStaff.setActive(asLong(m.get("active")));
        careStaff.setTodayCheckedIn(asLong(m.get("todayCheckedIn")));
        careStaff.setTodayCheckedOut(asLong(m.get("todayCheckedOut")));
        careStaff.setCurrentLeave(asLong(m.get("currentLeave")));
        careStaff.setPendingLeave(asLong(m.get("pendingLeave")));

        OperationsStatisticsVO.LeaveStats leave = vo.getLeave();
        leave.setCurrentLeave(careStaff.getCurrentLeave());
        leave.setPendingLeave(careStaff.getPendingLeave());
    }

    /**
     * 考勤按「快照日」统计：取 min(dateTo, today)，遵循 G6——库内仅 WORKING/COMPLETED，
     * NOT_CHECKED / LEAVE 为计算态。
     */
    private void fillAttendance(OperationsStatisticsVO vo, LocalDate today, LocalDate dateTo, LocalDateTime now) {
        LocalDate snapshot = dateTo.isAfter(today) ? today : dateTo;
        long activeStaff = statisticsMapper.countActiveCareStaff();
        long working = statisticsMapper.countAttendanceWorking(snapshot);
        long completed = statisticsMapper.countAttendanceCompleted(snapshot);
        long checkedIn = working + completed;
        long onLeave;
        if (snapshot.equals(today)) {
            onLeave = vo.getCareStaff().getCurrentLeave();
        } else {
            LocalDateTime dayStart = snapshot.atStartOfDay();
            LocalDateTime dayEnd = snapshot.plusDays(1).atStartOfDay();
            onLeave = statisticsMapper.countStaffOnLeaveDuringDay(dayStart, dayEnd);
        }
        // 请假且无考勤记录的才计入 LEAVE；已签到优先算考勤
        long leaveOnly = Math.max(0, onLeave);
        // 简化：启用护理员 − 已签到 − 请假（重叠时请假不重复扣未签到）
        // 未签到 = 启用数 − 已签到人数 − 当日请假且未签到
        // 已签到与请假互斥（G6：请假不可签到），故：
        long notChecked = Math.max(0, activeStaff - checkedIn - leaveOnly);

        OperationsStatisticsVO.AttendanceStats attendance = vo.getAttendance();
        attendance.setSnapshotDate(snapshot.format(DAY));
        attendance.setCheckedIn(checkedIn);
        attendance.setCompleted(completed);
        attendance.setNotChecked(notChecked);
        attendance.setLeave(leaveOnly);
    }

    private void fillMedication(OperationsStatisticsVO vo, LocalDateTime now) {
        OperationsStatisticsVO.MedicationStats medication = vo.getMedication();
        medication.setActive(statisticsMapper.countMedicationActive());
        medication.setInactive(statisticsMapper.countMedicationInactive());
        List<Long> elderIds = statisticsMapper.selectActiveElderIds();
        List<MedicationReminderVO> reminders = medicationReminderService.listForElders(
                elderIds != null ? elderIds : List.of(), now);
        long due = reminders.stream()
                .filter(r -> MedicationStatuses.REMIND_DUE.equals(r.getRemindStatus()))
                .count();
        medication.setTodayReminder(reminders.size());
        medication.setTodayDue(due);
    }

    private void fillMenu(OperationsStatisticsVO vo, LocalDate today) {
        LocalDate weekStart = WeekRanges.mondayOf(today);
        OperationsStatisticsVO.MenuStats menu = vo.getMenu();
        menu.setCurrentWeek(statisticsMapper.countCurrentWeekMenu(weekStart));
        menu.setMealCount(statisticsMapper.countWeekMeals(weekStart));
        menu.setDietaryNoteElderCount(statisticsMapper.countDietaryNoteElders());
        menu.setAdjustedElderCount(statisticsMapper.countAdjustedElders());
    }

    private void fillActivity(OperationsStatisticsVO vo, LocalDate today, LocalDateTime now) {
        YearMonth ym = YearMonth.from(today);
        Map<String, Object> m = nullToEmpty(statisticsMapper.selectActivityStats(
                today, now.toLocalTime(), ym.atDay(1), ym.atEndOfMonth()));
        OperationsStatisticsVO.ActivityStats activity = vo.getActivity();
        activity.setUpcoming(asLong(m.get("upcoming")));
        activity.setPublished(asLong(m.get("published")));
        activity.setCancelled(asLong(m.get("cancelled")));
        activity.setCompleted(asLong(m.get("completed")));
        activity.setThisMonth(asLong(m.get("thisMonth")));
    }

    private void fillTrend(OperationsStatisticsVO vo, LocalDate today) {
        LocalDate start = today.minusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(1).atStartOfDay();
        List<Map<String, Object>> rows = statisticsMapper.selectOrderTrend(from, toExclusive);
        Map<String, long[]> byDay = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object d = row.get("d");
                String key = d == null ? null : String.valueOf(d);
                if (key != null && key.length() >= 10) {
                    key = key.substring(0, 10);
                }
                if (key == null) continue;
                byDay.put(key, new long[]{asLong(row.get("total")), asLong(row.get("completed"))});
            }
        }
        List<OperationsStatisticsVO.TrendPoint> trend = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            String key = day.format(DAY);
            long[] v = byDay.getOrDefault(key, new long[]{0, 0});
            trend.add(new OperationsStatisticsVO.TrendPoint(key, v[0], v[1]));
        }
        vo.setTrend(trend);
    }

    private static Map<String, Object> nullToEmpty(Map<String, Object> m) {
        return m != null ? m : Map.of();
    }

    private static long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal asDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(1, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
