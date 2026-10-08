package com.eldercare.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运营综合统计：仅 COUNT / AVG / GROUP BY，不 SELECT *。
 */
@Mapper
public interface OperationsStatisticsMapper {

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM elder WHERE deleted = 0) AS total,
              (SELECT COUNT(1) FROM elder WHERE deleted = 0 AND status = 1) AS active,
              (SELECT COUNT(1) FROM elder WHERE deleted = 0 AND status = 0) AS inactive,
              (SELECT COUNT(DISTINCT ef.elder_id) FROM elder_family ef
                 INNER JOIN elder e ON e.id = ef.elder_id AND e.deleted = 0
               WHERE ef.deleted = 0 AND ef.status = 1) AS boundFamily
            """)
    Map<String, Object> selectElderStats();

    @Select("""
            SELECT
              COUNT(1) AS total,
              SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
              SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmed,
              SUM(CASE WHEN status = 'IN_SERVICE' THEN 1 ELSE 0 END) AS inService,
              SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
              SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled
            FROM care_service_order
            WHERE deleted = 0
              AND scheduled_start_time >= #{from}
              AND scheduled_start_time < #{toExclusive}
            """)
    Map<String, Object> selectOrderStats(@Param("from") LocalDateTime from,
                                         @Param("toExclusive") LocalDateTime toExclusive);

    @Select("""
            SELECT
              COUNT(1) AS total,
              ROUND(AVG(score), 1) AS averageScore
            FROM care_evaluation
            WHERE deleted = 0
              AND created_at >= #{from}
              AND created_at < #{toExclusive}
            """)
    Map<String, Object> selectEvaluationStats(@Param("from") LocalDateTime from,
                                              @Param("toExclusive") LocalDateTime toExclusive);

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM care_staff WHERE deleted = 0) AS total,
              (SELECT COUNT(1) FROM care_staff WHERE deleted = 0 AND status = 1) AS active,
              (SELECT COUNT(1) FROM care_staff_attendance
                WHERE deleted = 0 AND attendance_date = #{today}
                  AND status IN ('WORKING', 'COMPLETED')) AS todayCheckedIn,
              (SELECT COUNT(1) FROM care_staff_attendance
                WHERE deleted = 0 AND attendance_date = #{today}
                  AND status = 'COMPLETED') AS todayCheckedOut,
              (SELECT COUNT(DISTINCT care_staff_id) FROM care_staff_leave_application
                WHERE deleted = 0 AND status = 'APPROVED'
                  AND start_time <= #{now} AND end_time > #{now}) AS currentLeave,
              (SELECT COUNT(1) FROM care_staff_leave_application
                WHERE deleted = 0 AND status = 'PENDING') AS pendingLeave
            """)
    Map<String, Object> selectCareStaffStats(@Param("today") LocalDate today,
                                             @Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(1) FROM care_staff_attendance
            WHERE deleted = 0 AND attendance_date = #{day} AND status = 'WORKING'
            """)
    long countAttendanceWorking(@Param("day") LocalDate day);

    @Select("""
            SELECT COUNT(1) FROM care_staff_attendance
            WHERE deleted = 0 AND attendance_date = #{day} AND status = 'COMPLETED'
            """)
    long countAttendanceCompleted(@Param("day") LocalDate day);

    @Select("""
            SELECT COUNT(DISTINCT care_staff_id) FROM care_staff_leave_application
            WHERE deleted = 0 AND status = 'APPROVED'
              AND start_time <= #{dayEnd} AND end_time > #{dayStart}
            """)
    long countStaffOnLeaveDuringDay(@Param("dayStart") LocalDateTime dayStart,
                                    @Param("dayEnd") LocalDateTime dayEnd);

    @Select("""
            SELECT COUNT(1) FROM care_staff WHERE deleted = 0 AND status = 1
            """)
    long countActiveCareStaff();

    @Select("""
            SELECT COUNT(1) FROM elder_medication WHERE deleted = 0 AND status = 'ACTIVE'
            """)
    long countMedicationActive();

    @Select("""
            SELECT COUNT(1) FROM elder_medication WHERE deleted = 0 AND status = 'INACTIVE'
            """)
    long countMedicationInactive();

    @Select("""
            SELECT id FROM elder WHERE deleted = 0 AND status = 1
            """)
    List<Long> selectActiveElderIds();

    @Select("""
            SELECT COUNT(1) FROM weekly_menu
            WHERE deleted = 0 AND week_start_date = #{weekStart}
            """)
    long countCurrentWeekMenu(@Param("weekStart") LocalDate weekStart);

    @Select("""
            SELECT COUNT(1) FROM weekly_menu_item i
            INNER JOIN weekly_menu m ON m.id = i.weekly_menu_id AND m.deleted = 0
            WHERE m.week_start_date = #{weekStart}
            """)
    long countWeekMeals(@Param("weekStart") LocalDate weekStart);

    @Select("""
            SELECT COUNT(1) FROM elder_dietary_note
            WHERE deleted = 0 AND status = 'ACTIVE'
              AND note IS NOT NULL AND TRIM(note) <> ''
            """)
    long countDietaryNoteElders();

    @Select("""
            SELECT COUNT(DISTINCT elder_id) FROM elder_meal_adjustment
            WHERE deleted = 0 AND status = 'ACTIVE'
            """)
    long countAdjustedElders();

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM care_activity
                WHERE deleted = 0 AND status = 'PUBLISHED'
                  AND (activity_date > #{today}
                       OR (activity_date = #{today} AND end_time > #{nowTime}))) AS upcoming,
              (SELECT COUNT(1) FROM care_activity WHERE deleted = 0 AND status = 'PUBLISHED') AS published,
              (SELECT COUNT(1) FROM care_activity WHERE deleted = 0 AND status = 'CANCELLED') AS cancelled,
              (SELECT COUNT(1) FROM care_activity WHERE deleted = 0 AND status = 'COMPLETED') AS completed,
              (SELECT COUNT(1) FROM care_activity
                WHERE deleted = 0
                  AND activity_date >= #{monthStart}
                  AND activity_date <= #{monthEnd}) AS thisMonth
            """)
    Map<String, Object> selectActivityStats(@Param("today") LocalDate today,
                                            @Param("nowTime") java.time.LocalTime nowTime,
                                            @Param("monthStart") LocalDate monthStart,
                                            @Param("monthEnd") LocalDate monthEnd);

    @Select("""
            SELECT DATE(scheduled_start_time) AS d,
                   COUNT(1) AS total,
                   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
            FROM care_service_order
            WHERE deleted = 0
              AND scheduled_start_time >= #{from}
              AND scheduled_start_time < #{toExclusive}
            GROUP BY DATE(scheduled_start_time)
            """)
    List<Map<String, Object>> selectOrderTrend(@Param("from") LocalDateTime from,
                                               @Param("toExclusive") LocalDateTime toExclusive);
}
