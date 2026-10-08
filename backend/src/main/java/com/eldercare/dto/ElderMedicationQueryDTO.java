package com.eldercare.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ElderMedicationQueryDTO {

    private Long elderId;
    private String medicineName;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate onDate;
    private long page = 1;
    private long size = 10;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOnDate() { return onDate; }
    public void setOnDate(LocalDate onDate) { this.onDate = onDate; }
    public long getPage() { return page <= 0 ? 1 : page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size <= 0 ? 10 : Math.min(size, 100); }
    public void setSize(long size) { this.size = size; }
}
