package com.eldercare.dto;

import jakarta.validation.constraints.NotNull;

public class ConfirmServiceOrderDTO {

    @NotNull(message = "护理员ID不能为空")
    private Long careStaffId;

    public Long getCareStaffId() { return careStaffId; }
    public void setCareStaffId(Long careStaffId) { this.careStaffId = careStaffId; }
}
