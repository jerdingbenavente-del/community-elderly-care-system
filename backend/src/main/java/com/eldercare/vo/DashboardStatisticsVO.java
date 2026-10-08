package com.eldercare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端 Dashboard 汇总统计（一次返回，禁止前端拉全表再聚合）。
 */
public class DashboardStatisticsVO {

    private Long elderCount;
    private Long careStaffCount;
    private Long serviceItemCount;
    private Long enabledServiceItemCount;

    private Long serviceOrderCount;
    private Long pendingOrderCount;
    private Long confirmedOrderCount;
    private Long inServiceOrderCount;
    private Long completedOrderCount;
    private Long cancelledOrderCount;

    private Long warningCount;
    private Long unhandledWarningCount;
    private Long handledWarningCount;

    private Long evaluationCount;
    /** 无评价时为 null，避免 NaN */
    private BigDecimal averageScore;

    private List<DashboardRecentOrderVO> recentOrders = new ArrayList<>();
    private List<DashboardRecentWarningVO> recentWarnings = new ArrayList<>();

    public Long getElderCount() { return elderCount; }
    public void setElderCount(Long elderCount) { this.elderCount = elderCount; }
    public Long getCareStaffCount() { return careStaffCount; }
    public void setCareStaffCount(Long careStaffCount) { this.careStaffCount = careStaffCount; }
    public Long getServiceItemCount() { return serviceItemCount; }
    public void setServiceItemCount(Long serviceItemCount) { this.serviceItemCount = serviceItemCount; }
    public Long getEnabledServiceItemCount() { return enabledServiceItemCount; }
    public void setEnabledServiceItemCount(Long enabledServiceItemCount) { this.enabledServiceItemCount = enabledServiceItemCount; }
    public Long getServiceOrderCount() { return serviceOrderCount; }
    public void setServiceOrderCount(Long serviceOrderCount) { this.serviceOrderCount = serviceOrderCount; }
    public Long getPendingOrderCount() { return pendingOrderCount; }
    public void setPendingOrderCount(Long pendingOrderCount) { this.pendingOrderCount = pendingOrderCount; }
    public Long getConfirmedOrderCount() { return confirmedOrderCount; }
    public void setConfirmedOrderCount(Long confirmedOrderCount) { this.confirmedOrderCount = confirmedOrderCount; }
    public Long getInServiceOrderCount() { return inServiceOrderCount; }
    public void setInServiceOrderCount(Long inServiceOrderCount) { this.inServiceOrderCount = inServiceOrderCount; }
    public Long getCompletedOrderCount() { return completedOrderCount; }
    public void setCompletedOrderCount(Long completedOrderCount) { this.completedOrderCount = completedOrderCount; }
    public Long getCancelledOrderCount() { return cancelledOrderCount; }
    public void setCancelledOrderCount(Long cancelledOrderCount) { this.cancelledOrderCount = cancelledOrderCount; }
    public Long getWarningCount() { return warningCount; }
    public void setWarningCount(Long warningCount) { this.warningCount = warningCount; }
    public Long getUnhandledWarningCount() { return unhandledWarningCount; }
    public void setUnhandledWarningCount(Long unhandledWarningCount) { this.unhandledWarningCount = unhandledWarningCount; }
    public Long getHandledWarningCount() { return handledWarningCount; }
    public void setHandledWarningCount(Long handledWarningCount) { this.handledWarningCount = handledWarningCount; }
    public Long getEvaluationCount() { return evaluationCount; }
    public void setEvaluationCount(Long evaluationCount) { this.evaluationCount = evaluationCount; }
    public BigDecimal getAverageScore() { return averageScore; }
    public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
    public List<DashboardRecentOrderVO> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<DashboardRecentOrderVO> recentOrders) { this.recentOrders = recentOrders; }
    public List<DashboardRecentWarningVO> getRecentWarnings() { return recentWarnings; }
    public void setRecentWarnings(List<DashboardRecentWarningVO> recentWarnings) { this.recentWarnings = recentWarnings; }

    public static class DashboardRecentOrderVO {
        private Long id;
        private String orderNo;
        private String elderName;
        private String serviceName;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime scheduledStartTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getElderName() { return elderName; }
        public void setElderName(String elderName) { this.elderName = elderName; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getScheduledStartTime() { return scheduledStartTime; }
        public void setScheduledStartTime(LocalDateTime scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }
    }

    public static class DashboardRecentWarningVO {
        private Long id;
        private String elderName;
        private String indicator;
        private String warningLevel;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime generatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getElderName() { return elderName; }
        public void setElderName(String elderName) { this.elderName = elderName; }
        public String getIndicator() { return indicator; }
        public void setIndicator(String indicator) { this.indicator = indicator; }
        public String getWarningLevel() { return warningLevel; }
        public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }
}
