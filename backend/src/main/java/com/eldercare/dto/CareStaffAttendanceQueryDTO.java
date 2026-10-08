package com.eldercare.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CareStaffAttendanceQueryDTO {

    private Long careStaffId;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate attendanceDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private long page = 1;
    private long size = 10;

    public Long getCareStaffId() { return careStaffId; }
    public void setCareStaffId(Long careStaffId) { this.careStaffId = careStaffId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public long getPage() { return page <= 0 ? 1 : page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size <= 0 ? 10 : Math.min(size, 100); }
    public void setSize(long size) { this.size = size; }
}
