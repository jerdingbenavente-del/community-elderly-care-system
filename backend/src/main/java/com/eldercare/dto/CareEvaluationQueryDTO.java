package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CareEvaluationQueryDTO {

    private Long elderId;
    private Long serviceOrderId;
    private Integer score;

    /** 老人姓名模糊匹配（管理端） */
    @Size(max = 50)
    private String elderName;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getServiceOrderId() { return serviceOrderId; }
    public void setServiceOrderId(Long serviceOrderId) { this.serviceOrderId = serviceOrderId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
