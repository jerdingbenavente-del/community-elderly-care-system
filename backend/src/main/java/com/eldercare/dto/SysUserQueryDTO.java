package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SysUserQueryDTO {

    private String username;
    private String realName;
    private Integer status;
    /** 按角色编码筛选，如 ADMIN / CARE_STAFF / FAMILY */
    private String roleCode;

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
