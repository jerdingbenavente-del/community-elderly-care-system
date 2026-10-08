package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareStaffCreateDTO;
import com.eldercare.dto.CareStaffQueryDTO;
import com.eldercare.dto.CareStaffUpdateDTO;
import com.eldercare.vo.CareStaffVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareStaffService {

    Long create(CareStaffCreateDTO dto, HttpServletRequest request);

    void update(Long id, CareStaffUpdateDTO dto, HttpServletRequest request);

    void enable(Long id, HttpServletRequest request);

    void disable(Long id, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    CareStaffVO getById(Long id);

    CareStaffVO me();

    PageResult<CareStaffVO> page(CareStaffQueryDTO query);
}
