package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public class ServiceOrderQueryDTO {

    private Long elderId;
    private Long serviceItemId;
    private Long careStaffId;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTo;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(Long serviceItemId) { this.serviceItemId = serviceItemId; }
    public Long getCareStaffId() { return careStaffId; }
    public void setCareStaffId(Long careStaffId) { this.careStaffId = careStaffId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getScheduledStartFrom() { return scheduledStartFrom; }
    public void setScheduledStartFrom(LocalDateTime scheduledStartFrom) { this.scheduledStartFrom = scheduledStartFrom; }
    public LocalDateTime getScheduledStartTo() { return scheduledStartTo; }
    public void setScheduledStartTo(LocalDateTime scheduledStartTo) { this.scheduledStartTo = scheduledStartTo; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
