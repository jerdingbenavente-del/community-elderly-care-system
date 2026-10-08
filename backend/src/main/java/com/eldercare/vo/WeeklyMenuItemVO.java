package com.eldercare.vo;

import java.time.LocalDate;

public class WeeklyMenuItemVO {

    private Long id;
    private LocalDate menuDate;
    private String mealType;
    private String dishName;
    private String description;
    private String publicDishName;
    private String displayDishName;
    private boolean adjusted;
    private Long adjustmentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getMenuDate() { return menuDate; }
    public void setMenuDate(LocalDate menuDate) { this.menuDate = menuDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPublicDishName() { return publicDishName; }
    public void setPublicDishName(String publicDishName) { this.publicDishName = publicDishName; }
    public String getDisplayDishName() { return displayDishName; }
    public void setDisplayDishName(String displayDishName) { this.displayDishName = displayDishName; }
    public boolean isAdjusted() { return adjusted; }
    public void setAdjusted(boolean adjusted) { this.adjusted = adjusted; }
    public Long getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(Long adjustmentId) { this.adjustmentId = adjustmentId; }
}
