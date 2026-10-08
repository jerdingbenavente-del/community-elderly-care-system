package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareStaffScheduleCreateDTO;
import com.eldercare.dto.CareStaffScheduleQueryDTO;
import com.eldercare.dto.CareStaffScheduleUpdateDTO;
import com.eldercare.vo.CareStaffScheduleVO;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

public interface CareStaffScheduleService {

    Long create(CareStaffScheduleCreateDTO dto, HttpServletRequest request);

    void update(Long id, CareStaffScheduleUpdateDTO dto, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    CareStaffScheduleVO getById(Long id);

    PageResult<CareStaffScheduleVO> page(CareStaffScheduleQueryDTO query);

    /**
     * 校验护理员在 [start,end] 是否被某条 AVAILABLE 排班完整覆盖；不通过则抛业务异常。
     */
    void assertScheduleCovers(Long careStaffId, LocalDateTime start, LocalDateTime end);
}
