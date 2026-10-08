package com.eldercare.dto;

import jakarta.validation.constraints.Size;

public class SysUserUpdateDTO {

    @Size(max = 64)
    private String realName;

    @Size(max = 20)
    private String phone;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
