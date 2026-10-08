package com.eldercare.vo;

import java.time.LocalDate;
import java.time.LocalTime;

public class MedicationReminderVO {

    private Long medicationId;
    private Long elderId;
    private String elderName;
    private String medicineName;
    private String dosage;
    private String dosageUnit;
    private String usageMethod;
    private LocalDate remindDate;
    private LocalTime doseTime;
    /** PENDING 未到点 / DUE 到点提醒 */
    private String remindStatus;
    /** Redis 首次标记成功。刷新后仍返回该条，但 firstMark 为 false。 */
    private boolean firstMark;

    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }
    public Long getElderId() { return elderId; }
    public void setElderId(Long elderId) { this.elderId = elderId; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getDosageUnit() { return dosageUnit; }
    public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }
    public String getUsageMethod() { return usageMethod; }
    public void setUsageMethod(String usageMethod) { this.usageMethod = usageMethod; }
    public LocalDate getRemindDate() { return remindDate; }
    public void setRemindDate(LocalDate remindDate) { this.remindDate = remindDate; }
    public LocalTime getDoseTime() { return doseTime; }
    public void setDoseTime(LocalTime doseTime) { this.doseTime = doseTime; }
    public String getRemindStatus() { return remindStatus; }
    public void setRemindStatus(String remindStatus) { this.remindStatus = remindStatus; }
    public boolean isFirstMark() { return firstMark; }
    public void setFirstMark(boolean firstMark) { this.firstMark = firstMark; }
}
