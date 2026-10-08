package com.eldercare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CareServiceItemUpdateDTO {

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128)
    private String serviceName;

    @Size(max = 64)
    private String serviceType;

    @Size(max = 500)
    private String description;

    @NotNull(message = "服务时长不能为空")
    @Min(value = 1, message = "服务时长必须大于0")
    private Integer durationMinutes;

    private BigDecimal price;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能是 ENABLED 或 DISABLED")
    private String status;

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
