package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareStaffCreateDTO;
import com.eldercare.dto.CareStaffQueryDTO;
import com.eldercare.dto.CareStaffUpdateDTO;
import com.eldercare.service.CareStaffService;
import com.eldercare.vo.CareStaffVO;
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
@RequestMapping("/api/care-staff")
public class CareStaffController {

    private final CareStaffService careStaffService;

    public CareStaffController(CareStaffService careStaffService) {
        this.careStaffService = careStaffService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('care_staff:list')")
    public Result<PageResult<CareStaffVO>> page(@Valid CareStaffQueryDTO query) {
        return Result.success(careStaffService.page(query));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('care_staff:view')")
    public Result<CareStaffVO> me() {
        return Result.success(careStaffService.me());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care_staff:view')")
    public Result<CareStaffVO> detail(@PathVariable Long id) {
        return Result.success(careStaffService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care_staff:add')")
    public Result<Long> create(@Valid @RequestBody CareStaffCreateDTO dto, HttpServletRequest request) {
        return Result.success(careStaffService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care_staff:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CareStaffUpdateDTO dto,
                               HttpServletRequest request) {
        careStaffService.update(id, dto, request);
        return Result.success();
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('care_staff:enable')")
    public Result<Void> enable(@PathVariable Long id, HttpServletRequest request) {
        careStaffService.enable(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('care_staff:disable')")
    public Result<Void> disable(@PathVariable Long id, HttpServletRequest request) {
        careStaffService.disable(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care_staff:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        careStaffService.delete(id, request);
        return Result.success();
    }
}
