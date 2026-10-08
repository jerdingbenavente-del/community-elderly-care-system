package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.WeeklyMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface WeeklyMenuMapper extends BaseMapper<WeeklyMenu> {

    @Select("""
            SELECT id, week_start_date AS weekStartDate, week_end_date AS weekEndDate,
                   status, created_by AS createdBy, deleted,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM weekly_menu
            WHERE week_start_date = #{weekStart}
            LIMIT 1
            """)
    WeeklyMenu selectAnyByWeekStart(@Param("weekStart") LocalDate weekStart);

    @Update("UPDATE weekly_menu SET deleted = 0, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int restore(@Param("id") Long id, @Param("status") String status);
}
