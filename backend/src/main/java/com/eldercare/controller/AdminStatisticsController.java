package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.service.OperationsStatisticsService;
import com.eldercare.vo.OperationsStatisticsVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端运营综合统计（仅 ADMIN / dashboard:statistics:view）。
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final OperationsStatisticsService operationsStatisticsService;

    public AdminStatisticsController(OperationsStatisticsService operationsStatisticsService) {
        this.operationsStatisticsService = operationsStatisticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:statistics:view')")
    public Result<OperationsStatisticsVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.success(operationsStatisticsService.overview(dateFrom, dateTo));
    }
}
