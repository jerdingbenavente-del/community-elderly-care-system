package com.eldercare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ElderFamilyCreateDTO {

    @NotNull(message = "老人ID不能为空")
    private Long elderId;

    @NotNull(message = "家属用户ID不能为空")
    private Long familyUserId;

    @Size(max = 32, message = "关系长度不能超过32")
    private String relationship;

    @Min(value = 0, message = "是否主要联系人取值无效")
    @Max(value = 1, message = "是否主要联系人取值无效")
    private Integer isPrimary;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public Long getFamilyUserId() { return familyUserId; }
    public void setFamilyUserId(Long familyUserId) { this.familyUserId = familyUserId; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public Integer getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Integer isPrimary) { this.isPrimary = isPrimary; }
}
