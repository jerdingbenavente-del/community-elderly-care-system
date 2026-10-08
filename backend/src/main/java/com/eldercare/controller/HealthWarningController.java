package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.HealthWarningHandleDTO;
import com.eldercare.dto.HealthWarningQueryDTO;
import com.eldercare.service.HealthWarningService;
import com.eldercare.vo.HealthWarningVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-warnings")
public class HealthWarningController {

    private final HealthWarningService healthWarningService;

    public HealthWarningController(HealthWarningService healthWarningService) {
        this.healthWarningService = healthWarningService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('health:warning:list')")
    public Result<PageResult<HealthWarningVO>> page(@Valid HealthWarningQueryDTO query) {
        return Result.success(healthWarningService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('health:warning:view')")
    public Result<HealthWarningVO> detail(@PathVariable Long id) {
        return Result.success(healthWarningService.getById(id));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('health:warning:handle')")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody HealthWarningHandleDTO dto,
                               HttpServletRequest request) {
        healthWarningService.handle(id, dto, request);
        return Result.success();
    }
}
