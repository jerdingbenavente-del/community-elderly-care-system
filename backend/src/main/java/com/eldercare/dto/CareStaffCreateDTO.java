package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CareStaffCreateDTO {

    @NotNull(message = "系统用户ID不能为空")
    private Long userId;

    @NotBlank(message = "工号不能为空")
    @Size(max = 32)
    private String employeeNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64)
    private String name;

    /** 1男 2女，可选 */
    private Integer gender;

    @Size(max = 20)
    private String phone;

    @Size(max = 64)
    private String position;

    @Size(max = 500)
    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
