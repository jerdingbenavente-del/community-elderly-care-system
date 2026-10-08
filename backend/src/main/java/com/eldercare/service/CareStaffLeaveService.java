package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareStaffLeaveCreateDTO;
import com.eldercare.dto.CareStaffLeaveQueryDTO;
import com.eldercare.dto.CareStaffLeaveReviewDTO;
import com.eldercare.vo.CareStaffLeaveVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareStaffLeaveService {

    Long create(CareStaffLeaveCreateDTO dto, HttpServletRequest request);

    PageResult<CareStaffLeaveVO> pageMine(CareStaffLeaveQueryDTO query);

    PageResult<CareStaffLeaveVO> pageAdmin(CareStaffLeaveQueryDTO query);

    CareStaffLeaveVO getById(Long id);

    void cancel(Long id, HttpServletRequest request);

    void approve(Long id, CareStaffLeaveReviewDTO dto, HttpServletRequest request);

    void reject(Long id, CareStaffLeaveReviewDTO dto, HttpServletRequest request);
}
