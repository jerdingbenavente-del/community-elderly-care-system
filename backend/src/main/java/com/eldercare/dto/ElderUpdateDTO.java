package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ElderUpdateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    @Min(value = 1, message = "性别取值无效")
    @Max(value = 2, message = "性别取值无效")
    private Integer gender;

    private LocalDate birthDate;

    @Size(max = 20, message = "手机号长度不能超过20")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    @Size(max = 32, message = "身份证号长度不能超过32")
    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @Size(max = 32, message = "护理等级长度不能超过32")
    private String careLevel;

    @Min(value = 0, message = "状态取值无效")
    @Max(value = 1, message = "状态取值无效")
    private Integer status;

    private LocalDate registeredAt;

    @Size(max = 1000, message = "病史长度不能超过1000")
    private String medicalHistory;

    @Size(max = 1000, message = "过敏史长度不能超过1000")
    private String allergyHistory;

    @Size(max = 1000, message = "特殊照护需求长度不能超过1000")
    private String specialCareRequirement;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getCareLevel() { return careLevel; }
    public void setCareLevel(String careLevel) { this.careLevel = careLevel; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDate registeredAt) { this.registeredAt = registeredAt; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public String getAllergyHistory() { return allergyHistory; }
    public void setAllergyHistory(String allergyHistory) { this.allergyHistory = allergyHistory; }
    public String getSpecialCareRequirement() { return specialCareRequirement; }
    public void setSpecialCareRequirement(String specialCareRequirement) { this.specialCareRequirement = specialCareRequirement; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
