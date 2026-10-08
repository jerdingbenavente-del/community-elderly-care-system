package com.eldercare.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ElderVO {

    private Long id;
    private String name;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String address;
    /** 列表脱敏；详情可同样脱敏 */
    private String idCard;
    private String careLevel;
    private Integer status;
    private LocalDate registeredAt;
    private String medicalHistory;
    private String allergyHistory;
    private String specialCareRequirement;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EmergencyContactVO> emergencyContacts;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<EmergencyContactVO> getEmergencyContacts() { return emergencyContacts; }
    public void setEmergencyContacts(List<EmergencyContactVO> emergencyContacts) { this.emergencyContacts = emergencyContacts; }
}
