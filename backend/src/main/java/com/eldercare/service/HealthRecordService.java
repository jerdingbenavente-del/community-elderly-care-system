package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.HealthRecordCreateDTO;
import com.eldercare.dto.HealthRecordQueryDTO;
import com.eldercare.dto.HealthRecordUpdateDTO;
import com.eldercare.vo.HealthRecordVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface HealthRecordService {

    Long create(HealthRecordCreateDTO dto, HttpServletRequest request);

    void update(Long id, HealthRecordUpdateDTO dto, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    HealthRecordVO getById(Long id);

    PageResult<HealthRecordVO> page(HealthRecordQueryDTO query);

    /** 趋势数据：按测量时间升序 */
    List<HealthRecordVO> trend(Long elderId, HealthRecordQueryDTO query);

    List<HealthRecordVO> listForFamily(Long elderId, HealthRecordQueryDTO query);

    HealthRecordVO getByIdForFamily(Long id);
}
