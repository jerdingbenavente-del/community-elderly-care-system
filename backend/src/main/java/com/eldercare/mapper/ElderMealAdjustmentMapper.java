package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.ElderMealAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface ElderMealAdjustmentMapper extends BaseMapper<ElderMealAdjustment> {

    @Select("""
            SELECT id, elder_id AS elderId, menu_date AS menuDate, meal_type AS mealType,
                   adjusted_content AS adjustedContent, reason, status,
                   created_by AS createdBy, updated_by AS updatedBy, deleted,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM elder_meal_adjustment
            WHERE elder_id = #{elderId}
              AND menu_date = #{menuDate}
              AND meal_type = #{mealType}
            LIMIT 1
            """)
    ElderMealAdjustment selectAny(@Param("elderId") Long elderId,
                                  @Param("menuDate") LocalDate menuDate,
                                  @Param("mealType") String mealType);

    @Update("""
            UPDATE elder_meal_adjustment
            SET deleted = 0,
                status = #{status},
                adjusted_content = #{adjustedContent},
                reason = #{reason},
                updated_by = #{updatedBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int restore(ElderMealAdjustment row);
}
