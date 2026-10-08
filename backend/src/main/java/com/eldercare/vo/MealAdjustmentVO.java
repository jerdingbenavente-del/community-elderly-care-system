package com.eldercare.vo;

import java.time.LocalDate;

public class MealAdjustmentVO {

    private Long id;
    private Long elderId;
    private String elderName;
    private LocalDate menuDate;
    private String mealType;
    private String publicDishName;
    private String adjustedContent;
    private String reason;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public LocalDate getMenuDate() { return menuDate; }
    public void setMenuDate(LocalDate menuDate) { this.menuDate = menuDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getPublicDishName() { return publicDishName; }
    public void setPublicDishName(String publicDishName) { this.publicDishName = publicDishName; }
    public String getAdjustedContent() { return adjustedContent; }
    public void setAdjustedContent(String adjustedContent) { this.adjustedContent = adjustedContent; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
