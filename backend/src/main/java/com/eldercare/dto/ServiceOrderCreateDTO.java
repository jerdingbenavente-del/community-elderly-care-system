package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 创建服务订单。careStaffId 不在此 DTO 中：创建时一律为 null，仅 confirm 时由 ADMIN 分配。
 */
public class ServiceOrderCreateDTO {

    @NotNull(message = "老人ID不能为空")
    private Long elderId;

    @NotNull(message = "服务项目ID不能为空")
    private Long serviceItemId;

    @NotNull(message = "预约开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "预约结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    @Size(max = 500)
    private String remark;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public LocalDateTime getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(LocalDateTime scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }
    public LocalDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(LocalDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
