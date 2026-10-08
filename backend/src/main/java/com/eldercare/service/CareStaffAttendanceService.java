package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareStaffAttendanceQueryDTO;
import com.eldercare.vo.CareStaffAttendanceVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareStaffAttendanceService {

    CareStaffAttendanceVO today();

    void checkIn(HttpServletRequest request);

    void checkOut(HttpServletRequest request);

    PageResult<CareStaffAttendanceVO> pageMine(CareStaffAttendanceQueryDTO query);

    PageResult<CareStaffAttendanceVO> pageAdmin(CareStaffAttendanceQueryDTO query);
}
