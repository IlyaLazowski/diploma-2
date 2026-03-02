package com.fakel.dto;

import java.math.BigDecimal;

public class ExerciseParameterDto {
    private Long id;
    private String code;
    private String unitCode;
    private BigDecimal defaultValue;

    public ExerciseParameterDto() {}

    public ExerciseParameterDto(Long id, String code, String unitCode, BigDecimal defaultValue) {
        this.id = id;
        this.code = code;
        this.unitCode = unitCode;
        this.defaultValue = defaultValue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public BigDecimal getDefaultValue() { return defaultValue; }
    public void setDefaultValue(BigDecimal defaultValue) { this.defaultValue = defaultValue; }
}