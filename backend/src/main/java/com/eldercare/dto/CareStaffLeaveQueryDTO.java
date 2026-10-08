package com.eldercare.dto;

public class CareStaffLeaveQueryDTO {

    private String status;
    private Long careStaffId;
    private long page = 1;
    private long size = 10;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCareStaffId() { return careStaffId; }
    public void setCareStaffId(Long careStaffId) { this.careStaffId = careStaffId; }
    public long getPage() { return page <= 0 ? 1 : page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size <= 0 ? 10 : Math.min(size, 100); }
    public void setSize(long size) { this.size = size; }
}
