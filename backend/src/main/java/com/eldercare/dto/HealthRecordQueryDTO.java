package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public class HealthRecordQueryDTO {

    private Long elderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredTo;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public LocalDateTime getMeasuredFrom() { return measuredFrom; }
    public void setMeasuredFrom(LocalDateTime measuredFrom) { this.measuredFrom = measuredFrom; }
    public LocalDateTime getMeasuredTo() { return measuredTo; }
    public void setMeasuredTo(LocalDateTime measuredTo) { this.measuredTo = measuredTo; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
