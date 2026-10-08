package com.eldercare.service;

import com.eldercare.vo.DashboardStatisticsVO;

public interface DashboardStatisticsService {

    /**
     * 管理端 Dashboard 汇总（仅 ADMIN）。
     */
    DashboardStatisticsVO getAdminStatistics();
}
