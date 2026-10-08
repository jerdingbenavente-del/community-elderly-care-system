package com.eldercare.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeeklyMenuVO {

    private Long id;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String status;
    private List<WeeklyMenuItemVO> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<WeeklyMenuItemVO> getItems() { return items; }
    public void setItems(List<WeeklyMenuItemVO> items) { this.items = items; }
}
