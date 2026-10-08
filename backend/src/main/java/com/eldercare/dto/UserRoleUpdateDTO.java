package com.eldercare.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserRoleUpdateDTO {

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;

    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
