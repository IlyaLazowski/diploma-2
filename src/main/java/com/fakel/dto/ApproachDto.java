package com.fakel.dto;

import java.math.BigDecimal;

public class ApproachDto {
    private Long id;
    private Integer approachNumber;
    private String parameterCode;
    private String parameterName;
    private String unitCode;
    private BigDecimal value;

    public ApproachDto() {}

    public ApproachDto(Long id, Integer approachNumber, String parameterCode,
                       String parameterName, String unitCode, BigDecimal value) {
        this.id = id;
        this.approachNumber = approachNumber;
        this.parameterCode = parameterCode;
        this.parameterName = parameterName;
        this.unitCode = unitCode;
        this.value = value;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getApproachNumber() { return approachNumber; }
    public void setApproachNumber(Integer approachNumber) { this.approachNumber = approachNumber; }

    public String getParameterCode() { return parameterCode; }
    public void setParameterCode(String parameterCode) { this.parameterCode = parameterCode; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}