package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareStaffAttendanceQueryDTO;
import com.eldercare.service.CareStaffAttendanceService;
import com.eldercare.vo.CareStaffAttendanceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-staff-attendance")
public class CareStaffAttendanceController {

    private final CareStaffAttendanceService careStaffAttendanceService;

    public CareStaffAttendanceController(CareStaffAttendanceService careStaffAttendanceService) {
        this.careStaffAttendanceService = careStaffAttendanceService;
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('care:attendance:today')")
    public Result<CareStaffAttendanceVO> today() {
        return Result.success(careStaffAttendanceService.today());
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('care:attendance:check-in')")
    public Result<Void> checkIn(HttpServletRequest request) {
        careStaffAttendanceService.checkIn(request);
        return Result.success();
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('care:attendance:check-out')")
    public Result<Void> checkOut(HttpServletRequest request) {
        careStaffAttendanceService.checkOut(request);
        return Result.success();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('care:attendance:list')")
    public Result<PageResult<CareStaffAttendanceVO>> myPage(@Valid CareStaffAttendanceQueryDTO query) {
        return Result.success(careStaffAttendanceService.pageMine(query));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('care:attendance:admin:list')")
    public Result<PageResult<CareStaffAttendanceVO>> adminPage(@Valid CareStaffAttendanceQueryDTO query) {
        return Result.success(careStaffAttendanceService.pageAdmin(query));
    }
}
