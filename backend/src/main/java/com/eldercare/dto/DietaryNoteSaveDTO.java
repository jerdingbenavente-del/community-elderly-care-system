package com.eldercare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DietaryNoteSaveDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @Size(max = 1000, message = "备注不能超过1000字")
    private String note;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
