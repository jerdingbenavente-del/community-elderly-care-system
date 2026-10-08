package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareStaffScheduleCreateDTO;
import com.eldercare.dto.CareStaffScheduleQueryDTO;
import com.eldercare.dto.CareStaffScheduleUpdateDTO;
import com.eldercare.service.CareStaffScheduleService;
import com.eldercare.vo.CareStaffScheduleVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-staff-schedules")
public class CareStaffScheduleController {

    private final CareStaffScheduleService careStaffScheduleService;

    public CareStaffScheduleController(CareStaffScheduleService careStaffScheduleService) {
        this.careStaffScheduleService = careStaffScheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('care:schedule:list')")
    public Result<PageResult<CareStaffScheduleVO>> page(@Valid CareStaffScheduleQueryDTO query) {
        return Result.success(careStaffScheduleService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care:schedule:view')")
    public Result<CareStaffScheduleVO> detail(@PathVariable Long id) {
        return Result.success(careStaffScheduleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:schedule:add')")
    public Result<Long> create(@Valid @RequestBody CareStaffScheduleCreateDTO dto, HttpServletRequest request) {
        return Result.success(careStaffScheduleService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care:schedule:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CareStaffScheduleUpdateDTO dto,
                               HttpServletRequest request) {
        careStaffScheduleService.update(id, dto, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:schedule:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        careStaffScheduleService.delete(id, request);
        return Result.success();
    }
}
