package com.fakel.dto;

import java.math.BigDecimal;
import java.time.Duration;

public class StandardDto {
    private Long id;
    private Short number;
    private String name;
    private String measurementUnit;
    private Short course;
    private String grade;
    private Duration timeValue;  // Duration в DTO
    private BigDecimal intValue;
    private String weightCategory;

    public StandardDto() {}

    public StandardDto(Long id, Short number, String name, String measurementUnit,
                       Short course, String grade, Duration timeValue,
                       BigDecimal intValue, String weightCategory) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.measurementUnit = measurementUnit;
        this.course = course;
        this.grade = grade;
        this.timeValue = timeValue;
        this.intValue = intValue;
        this.weightCategory = weightCategory;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Short getNumber() { return number; }
    public void setNumber(Short number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(String measurementUnit) { this.measurementUnit = measurementUnit; }

    public Short getCourse() { return course; }
    public void setCourse(Short course) { this.course = course; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Duration getTimeValue() { return timeValue; }
    public void setTimeValue(Duration timeValue) { this.timeValue = timeValue; }

    public BigDecimal getIntValue() { return intValue; }
    public void setIntValue(BigDecimal intValue) { this.intValue = intValue; }

    public String getWeightCategory() { return weightCategory; }
    public void setWeightCategory(String weightCategory) { this.weightCategory = weightCategory; }
}