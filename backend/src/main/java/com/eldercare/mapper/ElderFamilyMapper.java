package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.ElderFamily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ElderFamilyMapper extends BaseMapper<ElderFamily> {

    /**
     * 含逻辑删除记录的查询，用于恢复绑定。
     */
    @Select("""
            SELECT id, elder_id, family_user_id, relationship, is_primary, status, deleted, created_at, updated_at
            FROM elder_family
            WHERE elder_id = #{elderId}
              AND family_user_id = #{familyUserId}
            LIMIT 1
            """)
    ElderFamily selectAnyByElderAndUser(@Param("elderId") Long elderId,
                                        @Param("familyUserId") Long familyUserId);

    @Update("""
            UPDATE elder_family
            SET deleted = 0,
                status = 1,
                relationship = #{relationship},
                is_primary = #{isPrimary},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int restoreById(ElderFamily family);
}
