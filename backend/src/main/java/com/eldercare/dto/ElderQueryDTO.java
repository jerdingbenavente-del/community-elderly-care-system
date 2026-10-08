package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ElderQueryDTO {

    private String name;
    private String phone;
    private Integer status;

    @Min(value = 1, message = "页码从1开始")
    private long page = 1;

    @Min(value = 1, message = "每页条数至少为1")
    @Max(value = 100, message = "每页条数不能超过100")
    private long size = 10;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
