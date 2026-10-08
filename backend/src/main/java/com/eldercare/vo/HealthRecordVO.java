package com.eldercare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class HealthRecordVO {

    private Long id;
    private Long elderId;
    private String elderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private BigDecimal bloodGlucose;
    private BigDecimal bodyTemperature;
    private Integer heartRate;
    private Long recordedBy;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private List<HealthWarningVO> warnings;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public LocalDateTime getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(LocalDateTime measuredAt) { this.measuredAt = measuredAt; }
    public Integer getSystolicPressure() { return systolicPressure; }
    public void setSystolicPressure(Integer systolicPressure) { this.systolicPressure = systolicPressure; }
    public Integer getDiastolicPressure() { return diastolicPressure; }
    public void setDiastolicPressure(Integer diastolicPressure) { this.diastolicPressure = diastolicPressure; }
    public BigDecimal getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(BigDecimal bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    public BigDecimal getBodyTemperature() { return bodyTemperature; }
    public void setBodyTemperature(BigDecimal bodyTemperature) { this.bodyTemperature = bodyTemperature; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<HealthWarningVO> getWarnings() { return warnings; }
    public void setWarnings(List<HealthWarningVO> warnings) { this.warnings = warnings; }
}
