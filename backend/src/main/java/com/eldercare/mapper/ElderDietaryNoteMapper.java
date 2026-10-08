package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.ElderDietaryNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ElderDietaryNoteMapper extends BaseMapper<ElderDietaryNote> {

    @Select("""
            SELECT id, elder_id AS elderId, note, status, updated_by AS updatedBy,
                   deleted, created_at AS createdAt, updated_at AS updatedAt
            FROM elder_dietary_note
            WHERE elder_id = #{elderId}
            LIMIT 1
            """)
    ElderDietaryNote selectAnyByElderId(@Param("elderId") Long elderId);

    @Update("""
            UPDATE elder_dietary_note
            SET deleted = 0, status = #{status}, note = #{note}, updated_by = #{updatedBy}, updated_at = NOW()
            WHERE id = #{id}
            """)
    int restore(ElderDietaryNote note);
}
