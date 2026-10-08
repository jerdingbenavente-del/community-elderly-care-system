package com.eldercare.mapper;

import com.eldercare.vo.DashboardStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dashboard 聚合查询：COUNT / AVG，禁止 SELECT * 全表回传。
 */
@Mapper
public interface DashboardStatisticsMapper {

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM elder WHERE deleted = 0) AS elderCount,
              (SELECT COUNT(1) FROM care_staff WHERE deleted = 0) AS careStaffCount,
              (SELECT COUNT(1) FROM care_service_item WHERE deleted = 0) AS serviceItemCount,
              (SELECT COUNT(1) FROM care_service_item WHERE deleted = 0 AND status = 'ENABLED') AS enabledServiceItemCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0) AS serviceOrderCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0 AND status = 'PENDING') AS pendingOrderCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0 AND status = 'CONFIRMED') AS confirmedOrderCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0 AND status = 'IN_SERVICE') AS inServiceOrderCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0 AND status = 'COMPLETED') AS completedOrderCount,
              (SELECT COUNT(1) FROM care_service_order WHERE deleted = 0 AND status = 'CANCELLED') AS cancelledOrderCount,
              (SELECT COUNT(1) FROM health_warning WHERE deleted = 0) AS warningCount,
              (SELECT COUNT(1) FROM health_warning WHERE deleted = 0 AND status = 'UNHANDLED') AS unhandledWarningCount,
              (SELECT COUNT(1) FROM health_warning WHERE deleted = 0 AND status = 'HANDLED') AS handledWarningCount,
              (SELECT COUNT(1) FROM care_evaluation WHERE deleted = 0) AS evaluationCount,
              (SELECT ROUND(AVG(score), 1) FROM care_evaluation WHERE deleted = 0) AS averageScore
            """)
    DashboardStatisticsVO selectOverview();

    @Select("""
            SELECT
              o.id AS id,
              o.order_no AS orderNo,
              e.name AS elderName,
              i.service_name AS serviceName,
              o.status AS status,
              o.scheduled_start_time AS scheduledStartTime
            FROM care_service_order o
            LEFT JOIN elder e ON e.id = o.elder_id AND e.deleted = 0
            LEFT JOIN care_service_item i ON i.id = o.service_item_id
            WHERE o.deleted = 0
            ORDER BY o.id DESC
            LIMIT #{limit}
            """)
    List<DashboardStatisticsVO.DashboardRecentOrderVO> selectRecentOrders(@Param("limit") int limit);

    @Select("""
            SELECT
              w.id AS id,
              e.name AS elderName,
              w.indicator AS indicator,
              w.warning_level AS warningLevel,
              w.status AS status,
              w.generated_at AS generatedAt
            FROM health_warning w
            LEFT JOIN elder e ON e.id = w.elder_id AND e.deleted = 0
            WHERE w.deleted = 0
            ORDER BY w.id DESC
            LIMIT #{limit}
            """)
    List<DashboardStatisticsVO.DashboardRecentWarningVO> selectRecentWarnings(@Param("limit") int limit);
}
