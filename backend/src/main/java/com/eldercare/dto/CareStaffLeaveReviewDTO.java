package com.eldercare.dto;

import jakarta.validation.constraints.Size;

public class CareStaffLeaveReviewDTO {

    @Size(max = 500, message = "审批备注不能超过500字")
    private String reviewRemark;

    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
}
