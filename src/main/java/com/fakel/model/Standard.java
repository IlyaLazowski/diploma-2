package com.fakel.model;

import com.fakel.converter.IntervalToDurationConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;

@Entity
@Table(name = "standards")
public class Standard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Short number;

    @Column(nullable = false, length = 512)
    private String name;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(nullable = false)
    private Short course;

    @Column(nullable = false, length = 32)
    private String grade;

    // ✅ Указываем, что это interval и нужен конвертер
    @Convert(converter = IntervalToDurationConverter.class)
    @Column(name = "time_value", columnDefinition = "interval")
    private Duration timeValue;

    @Column(name = "int_value")
    private BigDecimal intValue;

    @Column(name = "weight_category", length = 64)
    private String weightCategory;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Short getNumber() { return number; }
    public void setNumber(Short number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MeasurementUnit getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(MeasurementUnit measurementUnit) { this.measurementUnit = measurementUnit; }

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