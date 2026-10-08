package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HealthWarningHandleDTO {

    @NotBlank(message = "处理备注不能为空")
    @Size(max = 500, message = "处理备注不能超过500字")
    private String handlingResult;

    public String getHandlingResult() { return handlingResult; }
    public void setHandlingResult(String handlingResult) { this.handlingResult = handlingResult; }
}
