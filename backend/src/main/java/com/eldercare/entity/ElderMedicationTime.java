package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalTime;

@TableName("elder_medication_time")
public class ElderMedicationTime {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicationId;
    private LocalTime doseTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }
    public LocalTime getDoseTime() { return doseTime; }
    public void setDoseTime(LocalTime doseTime) { this.doseTime = doseTime; }
}
