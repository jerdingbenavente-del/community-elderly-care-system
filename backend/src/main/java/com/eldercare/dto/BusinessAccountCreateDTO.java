package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 业务开户（FAMILY / CARE_STAFF）：前端不传 username / password。
 */
public class BusinessAccountCreateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    /** FAMILY 或 CARE_STAFF（按 roleCode，不写死 roleId） */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64)
    private String roleCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
}
