package com.fakel.dto;

public class StandardResultDto {
    private String standardName;
    private Integer standardNumber;
    private Short mark;

    public StandardResultDto() {}

    public StandardResultDto(String standardName, Integer standardNumber, Short mark) {
        this.standardName = standardName;
        this.standardNumber = standardNumber;
        this.mark = mark;
    }

    // Геттеры
    public String getStandardName() { return standardName; }
    public Integer getStandardNumber() { return standardNumber; }
    public Short getMark() { return mark; }

    // Сеттеры
    public void setStandardName(String standardName) { this.standardName = standardName; }
    public void setStandardNumber(Integer standardNumber) { this.standardNumber = standardNumber; }
    public void setMark(Short mark) { this.mark = mark; }
}