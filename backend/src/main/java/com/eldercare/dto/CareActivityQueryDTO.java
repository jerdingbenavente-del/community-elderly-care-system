package com.eldercare.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CareActivityQueryDTO {

    private String status;
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private long page = 1;
    private long size = 10;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public long getPage() { return page <= 0 ? 1 : page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size <= 0 ? 10 : Math.min(size, 100); }
    public void setSize(long size) { this.size = size; }
}
