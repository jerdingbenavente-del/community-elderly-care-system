package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
}
