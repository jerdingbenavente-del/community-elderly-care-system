package com.eldercare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class HealthWarningVO {

    private Long id;
    private Long elderId;
    private String elderName;
    private Long healthRecordId;
    private String indicator;
    private String actualValue;
    private String thresholdDesc;
    private String direction;
    private String warningLevel;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;
    private Long handledBy;
    private String handledByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;
    private String handlingResult;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public Long getHealthRecordId() { return healthRecordId; }
    public void setHealthRecordId(Long healthRecordId) { this.healthRecordId = healthRecordId; }
    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public String getThresholdDesc() { return thresholdDesc; }
    public void setThresholdDesc(String thresholdDesc) { this.thresholdDesc = thresholdDesc; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public String getHandledByName() { return handledByName; }
    public void setHandledByName(String handledByName) { this.handledByName = handledByName; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getHandlingResult() { return handlingResult; }
    public void setHandlingResult(String handlingResult) { this.handlingResult = handlingResult; }
}
