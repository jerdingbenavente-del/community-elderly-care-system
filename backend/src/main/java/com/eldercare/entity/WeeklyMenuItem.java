package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

@TableName("weekly_menu_item")
public class WeeklyMenuItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long weeklyMenuId;
    private LocalDate menuDate;
    private String mealType;
    private String dishName;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWeeklyMenuId() { return weeklyMenuId; }
    public void setWeeklyMenuId(Long weeklyMenuId) { this.weeklyMenuId = weeklyMenuId; }
    public LocalDate getMenuDate() { return menuDate; }
    public void setMenuDate(LocalDate menuDate) { this.menuDate = menuDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
