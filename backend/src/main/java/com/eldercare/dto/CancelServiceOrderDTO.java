package com.eldercare.dto;

import jakarta.validation.constraints.Size;

public class CancelServiceOrderDTO {

    @Size(max = 500, message = "取消原因不能超过500字")
    private String cancelReason;

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
