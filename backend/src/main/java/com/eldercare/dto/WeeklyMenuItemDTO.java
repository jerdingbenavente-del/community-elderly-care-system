package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class WeeklyMenuItemDTO {

    @NotNull(message = "日期不能为空")
    private LocalDate menuDate;
    @NotBlank(message = "餐次不能为空")
    private String mealType;
    @NotBlank(message = "菜品不能为空")
    private String dishName;
    private String description;

    public LocalDate getMenuDate() { return menuDate; }
    public void setMenuDate(LocalDate menuDate) { this.menuDate = menuDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
