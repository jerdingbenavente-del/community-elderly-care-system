package com.eldercare.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端运营综合统计（实时聚合，不落库）。
 */
public class OperationsStatisticsVO {

    private String dateFrom;
    private String dateTo;

    private ElderStats elder = new ElderStats();
    private OrderStats orders = new OrderStats();
    private EvaluationStats evaluation = new EvaluationStats();
    private CareStaffStats careStaff = new CareStaffStats();
    private AttendanceStats attendance = new AttendanceStats();
    private LeaveStats leave = new LeaveStats();
    private MedicationStats medication = new MedicationStats();
    private MenuStats menu = new MenuStats();
    private ActivityStats activity = new ActivityStats();
    private List<TrendPoint> trend = new ArrayList<>();

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public ElderStats getElder() { return elder; }
    public void setElder(ElderStats elder) { this.elder = elder; }
    public OrderStats getOrders() { return orders; }
    public void setOrders(OrderStats orders) { this.orders = orders; }
    public EvaluationStats getEvaluation() { return evaluation; }
    public void setEvaluation(EvaluationStats evaluation) { this.evaluation = evaluation; }
    public CareStaffStats getCareStaff() { return careStaff; }
    public void setCareStaff(CareStaffStats careStaff) { this.careStaff = careStaff; }
    public AttendanceStats getAttendance() { return attendance; }
    public void setAttendance(AttendanceStats attendance) { this.attendance = attendance; }
    public LeaveStats getLeave() { return leave; }
    public void setLeave(LeaveStats leave) { this.leave = leave; }
    public MedicationStats getMedication() { return medication; }
    public void setMedication(MedicationStats medication) { this.medication = medication; }
    public MenuStats getMenu() { return menu; }
    public void setMenu(MenuStats menu) { this.menu = menu; }
    public ActivityStats getActivity() { return activity; }
    public void setActivity(ActivityStats activity) { this.activity = activity; }
    public List<TrendPoint> getTrend() { return trend; }
    public void setTrend(List<TrendPoint> trend) { this.trend = trend; }

    public static class ElderStats {
        private long total;
        private long active;
        private long inactive;
        private long boundFamily;
        private long unboundFamily;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getActive() { return active; }
        public void setActive(long active) { this.active = active; }
        public long getInactive() { return inactive; }
        public void setInactive(long inactive) { this.inactive = inactive; }
        public long getBoundFamily() { return boundFamily; }
        public void setBoundFamily(long boundFamily) { this.boundFamily = boundFamily; }
        public long getUnboundFamily() { return unboundFamily; }
        public void setUnboundFamily(long unboundFamily) { this.unboundFamily = unboundFamily; }
    }

    public static class OrderStats {
        private long total;
        private long pending;
        private long confirmed;
        private long inService;
        private long completed;
        private long cancelled;
        /** COMPLETED / (TOTAL - CANCELLED)，分母为 0 时为 0 */
        private BigDecimal completedRate = BigDecimal.ZERO;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getPending() { return pending; }
        public void setPending(long pending) { this.pending = pending; }
        public long getConfirmed() { return confirmed; }
        public void setConfirmed(long confirmed) { this.confirmed = confirmed; }
        public long getInService() { return inService; }
        public void setInService(long inService) { this.inService = inService; }
        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }
        public long getCancelled() { return cancelled; }
        public void setCancelled(long cancelled) { this.cancelled = cancelled; }
        public BigDecimal getCompletedRate() { return completedRate; }
        public void setCompletedRate(BigDecimal completedRate) { this.completedRate = completedRate; }
    }

    public static class EvaluationStats {
        private long total;
        private BigDecimal averageScore;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public BigDecimal getAverageScore() { return averageScore; }
        public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
    }

    public static class CareStaffStats {
        private long total;
        private long active;
        private long todayCheckedIn;
        private long todayCheckedOut;
        private long currentLeave;
        private long pendingLeave;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getActive() { return active; }
        public void setActive(long active) { this.active = active; }
        public long getTodayCheckedIn() { return todayCheckedIn; }
        public void setTodayCheckedIn(long todayCheckedIn) { this.todayCheckedIn = todayCheckedIn; }
        public long getTodayCheckedOut() { return todayCheckedOut; }
        public void setTodayCheckedOut(long todayCheckedOut) { this.todayCheckedOut = todayCheckedOut; }
        public long getCurrentLeave() { return currentLeave; }
        public void setCurrentLeave(long currentLeave) { this.currentLeave = currentLeave; }
        public long getPendingLeave() { return pendingLeave; }
        public void setPendingLeave(long pendingLeave) { this.pendingLeave = pendingLeave; }
    }

    public static class AttendanceStats {
        /** 快照日期（范围末日与今天取较早） */
        private String snapshotDate;
        private long checkedIn;
        private long completed;
        private long notChecked;
        private long leave;

        public String getSnapshotDate() { return snapshotDate; }
        public void setSnapshotDate(String snapshotDate) { this.snapshotDate = snapshotDate; }
        public long getCheckedIn() { return checkedIn; }
        public void setCheckedIn(long checkedIn) { this.checkedIn = checkedIn; }
        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }
        public long getNotChecked() { return notChecked; }
        public void setNotChecked(long notChecked) { this.notChecked = notChecked; }
        public long getLeave() { return leave; }
        public void setLeave(long leave) { this.leave = leave; }
    }

    public static class LeaveStats {
        private long currentLeave;
        private long pendingLeave;

        public long getCurrentLeave() { return currentLeave; }
        public void setCurrentLeave(long currentLeave) { this.currentLeave = currentLeave; }
        public long getPendingLeave() { return pendingLeave; }
        public void setPendingLeave(long pendingLeave) { this.pendingLeave = pendingLeave; }
    }

    public static class MedicationStats {
        private long active;
        private long inactive;
        private long todayReminder;
        private long todayDue;

        public long getActive() { return active; }
        public void setActive(long active) { this.active = active; }
        public long getInactive() { return inactive; }
        public void setInactive(long inactive) { this.inactive = inactive; }
        public long getTodayReminder() { return todayReminder; }
        public void setTodayReminder(long todayReminder) { this.todayReminder = todayReminder; }
        public long getTodayDue() { return todayDue; }
        public void setTodayDue(long todayDue) { this.todayDue = todayDue; }
    }

    public static class MenuStats {
        private long currentWeek;
        private long mealCount;
        private long dietaryNoteElderCount;
        private long adjustedElderCount;

        public long getCurrentWeek() { return currentWeek; }
        public void setCurrentWeek(long currentWeek) { this.currentWeek = currentWeek; }
        public long getMealCount() { return mealCount; }
        public void setMealCount(long mealCount) { this.mealCount = mealCount; }
        public long getDietaryNoteElderCount() { return dietaryNoteElderCount; }
        public void setDietaryNoteElderCount(long dietaryNoteElderCount) { this.dietaryNoteElderCount = dietaryNoteElderCount; }
        public long getAdjustedElderCount() { return adjustedElderCount; }
        public void setAdjustedElderCount(long adjustedElderCount) { this.adjustedElderCount = adjustedElderCount; }
    }

    public static class ActivityStats {
        private long upcoming;
        private long published;
        private long cancelled;
        private long completed;
        private long thisMonth;

        public long getUpcoming() { return upcoming; }
        public void setUpcoming(long upcoming) { this.upcoming = upcoming; }
        public long getPublished() { return published; }
        public void setPublished(long published) { this.published = published; }
        public long getCancelled() { return cancelled; }
        public void setCancelled(long cancelled) { this.cancelled = cancelled; }
        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }
        public long getThisMonth() { return thisMonth; }
        public void setThisMonth(long thisMonth) { this.thisMonth = thisMonth; }
    }

    public static class TrendPoint {
        private String date;
        private long total;
        private long completed;

        public TrendPoint() {}
        public TrendPoint(String date, long total, long completed) {
            this.date = date;
            this.total = total;
            this.completed = completed;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }
    }
}
