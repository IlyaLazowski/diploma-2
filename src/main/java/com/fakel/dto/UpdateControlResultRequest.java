package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateControlResultRequest {

    @NotNull(message = "ID курсанта обязателен")
    private Long cadetId;

    @NotNull(message = "Номер норматива обязателен")
    private Short standardNumber;

    private String status;
    private BigDecimal timeValue;
    private Integer intValue;

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public Short getStandardNumber() { return standardNumber; }
    public void setStandardNumber(Short standardNumber) { this.standardNumber = standardNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTimeValue() { return timeValue; }
    public void setTimeValue(BigDecimal timeValue) { this.timeValue = timeValue; }

    public Integer getIntValue() { return intValue; }
    public void setIntValue(Integer intValue) { this.intValue = intValue; }
}