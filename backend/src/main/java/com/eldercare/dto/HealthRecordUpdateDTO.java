package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HealthRecordUpdateDTO {

    @NotNull(message = "测量时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    private Integer systolicPressure;
    private Integer diastolicPressure;
    private BigDecimal bloodGlucose;
    private BigDecimal bodyTemperature;
    private Integer heartRate;
    private String remark;

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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
