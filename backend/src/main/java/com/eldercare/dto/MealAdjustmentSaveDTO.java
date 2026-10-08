package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class MealAdjustmentSaveDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;
    @NotNull(message = "日期不能为空")
    private LocalDate menuDate;
    @NotBlank(message = "餐次不能为空")
    private String mealType;
    @NotBlank(message = "调整后的菜品不能为空")
    @Size(max = 200, message = "菜品不能超过200字")
    private String adjustedContent;
    @Size(max = 500, message = "原因不能超过500字")
    private String reason;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public LocalDate getMenuDate() { return menuDate; }
    public void setMenuDate(LocalDate menuDate) { this.menuDate = menuDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getAdjustedContent() { return adjustedContent; }
    public void setAdjustedContent(String adjustedContent) { this.adjustedContent = adjustedContent; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
