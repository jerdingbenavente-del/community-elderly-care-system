package com.eldercare.service;

import com.eldercare.vo.OperationsStatisticsVO;

import java.time.LocalDate;

public interface OperationsStatisticsService {

    /**
     * 管理端运营综合统计。dateFrom/dateTo 为空时默认当前自然月 1 日～今天。
     */
    OperationsStatisticsVO overview(LocalDate dateFrom, LocalDate dateTo);
}
