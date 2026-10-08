package com.eldercare.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ElderMenuViewVO {

    private Long elderId;
    private String elderName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String dietaryNote;
    private List<WeeklyMenuItemVO> items = new ArrayList<>();

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    public String getDietaryNote() { return dietaryNote; }
    public void setDietaryNote(String dietaryNote) { this.dietaryNote = dietaryNote; }
    public List<WeeklyMenuItemVO> getItems() { return items; }
    public void setItems(List<WeeklyMenuItemVO> items) { this.items = items; }
}
