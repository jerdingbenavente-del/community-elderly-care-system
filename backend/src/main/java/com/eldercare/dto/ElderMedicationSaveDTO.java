package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class ElderMedicationSaveDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @NotBlank(message = "药品名称不能为空")
    @Size(max = 100, message = "药品名称不能超过100字")
    private String medicineName;

    @NotBlank(message = "剂量不能为空")
    @Size(max = 32, message = "剂量不能超过32字")
    private String dosage;

    @NotBlank(message = "单位不能为空")
    @Size(max = 16, message = "单位不能超过16字")
    private String dosageUnit;

    @NotBlank(message = "服用方式不能为空")
    @Size(max = 32, message = "服用方式不能超过32字")
    private String usageMethod;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;

    @NotEmpty(message = "至少填写一个服药时间")
    private List<String> doseTimes;

    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getDosageUnit() { return dosageUnit; }
    public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }
    public String getUsageMethod() { return usageMethod; }
    public void setUsageMethod(String usageMethod) { this.usageMethod = usageMethod; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getDoseTimes() { return doseTimes; }
    public void setDoseTimes(List<String> doseTimes) { this.doseTimes = doseTimes; }
}
