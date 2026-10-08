package com.eldercare.service.impl;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.DashboardStatisticsMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.DashboardStatisticsService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.DashboardStatisticsVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DashboardStatisticsServiceImpl implements DashboardStatisticsService {

    private static final int RECENT_LIMIT = 5;

    private final DashboardStatisticsMapper dashboardStatisticsMapper;
    private final DataPermissionService dataPermissionService;

    public DashboardStatisticsServiceImpl(DashboardStatisticsMapper dashboardStatisticsMapper,
                                          DataPermissionService dataPermissionService) {
        this.dashboardStatisticsMapper = dashboardStatisticsMapper;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    public DashboardStatisticsVO getAdminStatistics() {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看工作台统计");
        }

        DashboardStatisticsVO vo = dashboardStatisticsMapper.selectOverview();
        if (vo == null) {
            vo = new DashboardStatisticsVO();
        }
        normalizeCounts(vo);

        List<DashboardStatisticsVO.DashboardRecentOrderVO> orders =
                dashboardStatisticsMapper.selectRecentOrders(RECENT_LIMIT);
        vo.setRecentOrders(orders != null ? orders : Collections.emptyList());

        List<DashboardStatisticsVO.DashboardRecentWarningVO> warnings =
                dashboardStatisticsMapper.selectRecentWarnings(RECENT_LIMIT);
        vo.setRecentWarnings(warnings != null ? warnings : Collections.emptyList());

        // 无评价时 averageScore 保持 null，前端显示「暂无」
        if (vo.getEvaluationCount() == null || vo.getEvaluationCount() == 0L) {
            vo.setAverageScore(null);
        }
        return vo;
    }

    private void normalizeCounts(DashboardStatisticsVO vo) {
        if (vo.getElderCount() == null) vo.setElderCount(0L);
        if (vo.getCareStaffCount() == null) vo.setCareStaffCount(0L);
        if (vo.getServiceItemCount() == null) vo.setServiceItemCount(0L);
        if (vo.getEnabledServiceItemCount() == null) vo.setEnabledServiceItemCount(0L);
        if (vo.getServiceOrderCount() == null) vo.setServiceOrderCount(0L);
        if (vo.getPendingOrderCount() == null) vo.setPendingOrderCount(0L);
        if (vo.getConfirmedOrderCount() == null) vo.setConfirmedOrderCount(0L);
        if (vo.getInServiceOrderCount() == null) vo.setInServiceOrderCount(0L);
        if (vo.getCompletedOrderCount() == null) vo.setCompletedOrderCount(0L);
        if (vo.getCancelledOrderCount() == null) vo.setCancelledOrderCount(0L);
        if (vo.getWarningCount() == null) vo.setWarningCount(0L);
        if (vo.getUnhandledWarningCount() == null) vo.setUnhandledWarningCount(0L);
        if (vo.getHandledWarningCount() == null) vo.setHandledWarningCount(0L);
        if (vo.getEvaluationCount() == null) vo.setEvaluationCount(0L);
    }
}
