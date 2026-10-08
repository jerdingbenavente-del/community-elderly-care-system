package com.eldercare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class WeeklyMenuSaveDTO {

    @NotEmpty(message = "请录入整周餐次")
    @Valid
    private List<WeeklyMenuItemDTO> items;

    public List<WeeklyMenuItemDTO> getItems() { return items; }
    public void setItems(List<WeeklyMenuItemDTO> items) { this.items = items; }
}
