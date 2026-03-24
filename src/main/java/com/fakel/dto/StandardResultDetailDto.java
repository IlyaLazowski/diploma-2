package com.fakel.dto;

public class StandardResultDetailDto {
    private Integer standardNumber;
    private String standardName;
    private Short mark;
    private String grade;
    private String measurementUnit;

    public StandardResultDetailDto() {}

    public StandardResultDetailDto(Integer standardNumber, String standardName, Short mark,
                                   String grade, String measurementUnit) {
        this.standardNumber = standardNumber;
        this.standardName = standardName;
        this.mark = mark;
        this.grade = grade;
        this.measurementUnit = measurementUnit;
    }

    public Integer getStandardNumber() { return standardNumber; }
    public void setStandardNumber(Integer standardNumber) { this.standardNumber = standardNumber; }

    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }

    public Short getMark() { return mark; }
    public void setMark(Short mark) { this.mark = mark; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(String measurementUnit) { this.measurementUnit = measurementUnit; }
}