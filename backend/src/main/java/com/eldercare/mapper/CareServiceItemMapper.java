package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.CareServiceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CareServiceItemMapper extends BaseMapper<CareServiceItem> {

    /**
     * 含逻辑删除记录，供历史订单展示服务名称（删项目 ≠ 删订单）。
     */
    @Select("SELECT * FROM care_service_item WHERE id = #{id} LIMIT 1")
    CareServiceItem selectByIdIncludeDeleted(@Param("id") Long id);
}
