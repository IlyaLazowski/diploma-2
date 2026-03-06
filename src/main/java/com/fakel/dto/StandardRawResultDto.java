package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class StandardRawResultDto {

    @NotNull(message = "Номер норматива обязателен")
    private Short standardNumber;

    private BigDecimal timeValue;
    private Integer intValue;

    public Short getStandardNumber() { return standardNumber; }
    public void setStandardNumber(Short standardNumber) { this.standardNumber = standardNumber; }

    public BigDecimal getTimeValue() { return timeValue; }
    public void setTimeValue(BigDecimal timeValue) { this.timeValue = timeValue; }

    public Integer getIntValue() { return intValue; }
    public void setIntValue(Integer intValue) { this.intValue = intValue; }
}