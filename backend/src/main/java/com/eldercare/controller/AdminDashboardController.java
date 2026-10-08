package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.service.DashboardStatisticsService;
import com.eldercare.vo.DashboardStatisticsVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 Dashboard 统计（一次返回汇总，仅 ADMIN 可访问）。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardStatisticsService dashboardStatisticsService;

    public AdminDashboardController(DashboardStatisticsService dashboardStatisticsService) {
        this.dashboardStatisticsService = dashboardStatisticsService;
    }

    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public Result<DashboardStatisticsVO> statistics() {
        return Result.success(dashboardStatisticsService.getAdminStatistics());
    }
}
