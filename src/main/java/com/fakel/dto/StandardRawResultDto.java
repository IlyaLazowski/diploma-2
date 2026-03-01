package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class StandardRawResultDto {
    @NotNull(message = "Номер норматива обязателен")
    private Short standardNumber;  // ← НОМЕР, а не ID!

    private BigDecimal timeValue;
    private Integer intValue;

    public Short getStandardNumber() { return standardNumber; }
    public void setStandardNumber(Short standardNumber) { this.standardNumber = standardNumber; }

    public BigDecimal getTimeValue() { return timeValue; }
    public void setTimeValue(BigDecimal timeValue) { this.timeValue = timeValue; }

    public Integer getIntValue() { return intValue; }
    public void setIntValue(Integer intValue) { this.intValue = intValue; }
}