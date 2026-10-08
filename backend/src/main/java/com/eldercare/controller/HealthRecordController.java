package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.HealthRecordCreateDTO;
import com.eldercare.dto.HealthRecordQueryDTO;
import com.eldercare.dto.HealthRecordUpdateDTO;
import com.eldercare.service.HealthRecordService;
import com.eldercare.vo.HealthRecordVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('health:record:add')")
    public Result<Long> create(@Valid @RequestBody HealthRecordCreateDTO dto, HttpServletRequest request) {
        return Result.success(healthRecordService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('health:record:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody HealthRecordUpdateDTO dto,
                               HttpServletRequest request) {
        healthRecordService.update(id, dto, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('health:record:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        healthRecordService.delete(id, request);
        return Result.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('health:record:view')")
    public Result<HealthRecordVO> detail(@PathVariable Long id) {
        return Result.success(healthRecordService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('health:record:list')")
    public Result<PageResult<HealthRecordVO>> page(@Valid HealthRecordQueryDTO query) {
        return Result.success(healthRecordService.page(query));
    }

    @GetMapping("/trend/{elderId}")
    @PreAuthorize("hasAuthority('health:record:list')")
    public Result<List<HealthRecordVO>> trend(@PathVariable Long elderId, @Valid HealthRecordQueryDTO query) {
        return Result.success(healthRecordService.trend(elderId, query));
    }
}
