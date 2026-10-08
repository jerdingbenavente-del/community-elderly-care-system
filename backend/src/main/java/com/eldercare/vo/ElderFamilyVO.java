package com.eldercare.vo;

import java.time.LocalDateTime;

public class ElderFamilyVO {

    private Long id;
    private Long elderId;
    private String elderName;
    private Long familyUserId;
    private String familyUsername;
    private String familyRealName;
    private String relationship;
    private Integer isPrimary;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public String getFamilyUsername() { return familyUsername; }
    public void setFamilyUsername(String familyUsername) { this.familyUsername = familyUsername; }
    public String getFamilyRealName() { return familyRealName; }
    public void setFamilyRealName(String familyRealName) { this.familyRealName = familyRealName; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public Integer getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Integer isPrimary) { this.isPrimary = isPrimary; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
