package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareStaffLeaveCreateDTO;
import com.eldercare.dto.CareStaffLeaveQueryDTO;
import com.eldercare.dto.CareStaffLeaveReviewDTO;
import com.eldercare.service.CareStaffLeaveService;
import com.eldercare.vo.CareStaffLeaveVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-staff-leaves")
public class CareStaffLeaveController {

    private final CareStaffLeaveService careStaffLeaveService;

    public CareStaffLeaveController(CareStaffLeaveService careStaffLeaveService) {
        this.careStaffLeaveService = careStaffLeaveService;
    }

    /** 护理员：本人申请列表 */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('care:leave:list')")
    public Result<PageResult<CareStaffLeaveVO>> myPage(@Valid CareStaffLeaveQueryDTO query) {
        return Result.success(careStaffLeaveService.pageMine(query));
    }

    /** 管理员：全部申请列表 */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('care:leave:admin:list')")
    public Result<PageResult<CareStaffLeaveVO>> adminPage(@Valid CareStaffLeaveQueryDTO query) {
        return Result.success(careStaffLeaveService.pageAdmin(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care:leave:view')")
    public Result<CareStaffLeaveVO> detail(@PathVariable Long id) {
        return Result.success(careStaffLeaveService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:leave:add')")
    public Result<Long> create(@Valid @RequestBody CareStaffLeaveCreateDTO dto, HttpServletRequest request) {
        return Result.success(careStaffLeaveService.create(dto, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:leave:cancel')")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        careStaffLeaveService.cancel(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('care:leave:approve')")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) CareStaffLeaveReviewDTO dto,
                                HttpServletRequest request) {
        careStaffLeaveService.approve(id, dto != null ? dto : new CareStaffLeaveReviewDTO(), request);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('care:leave:approve')")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) CareStaffLeaveReviewDTO dto,
                               HttpServletRequest request) {
        careStaffLeaveService.reject(id, dto != null ? dto : new CareStaffLeaveReviewDTO(), request);
        return Result.success();
    }
}
