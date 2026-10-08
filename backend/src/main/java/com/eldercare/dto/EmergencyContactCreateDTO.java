package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmergencyContactCreateDTO {

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    @Size(max = 32, message = "关系长度不能超过32")
    private String relationship;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    private Integer priority;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
