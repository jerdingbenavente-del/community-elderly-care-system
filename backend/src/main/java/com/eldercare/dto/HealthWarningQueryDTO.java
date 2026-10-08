package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class HealthWarningQueryDTO {

    private Long elderId;
    private Long healthRecordId;
    private String status;
    private String indicator;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getHealthRecordId() { return healthRecordId; }
    public void setHealthRecordId(Long healthRecordId) { this.healthRecordId = healthRecordId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
