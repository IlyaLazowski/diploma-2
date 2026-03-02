package com.fakel.model;

import com.fakel.converter.IntervalToDurationConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;

@Entity
@Table(name = "exercise_parameters")
public class ExerciseParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_catalog_id", nullable = false)
    private ExerciseCatalog exerciseCatalog;

    @Column(nullable = false, length = 64)
    private String code;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id", nullable = false)
    private MeasurementUnit measurementUnit;

    @Convert(converter = IntervalToDurationConverter.class)
    @Column(name = "default_time_value", columnDefinition = "interval")  // ← добавить columnDefinition
    private Duration defaultTimeValue;

    @Column(name = "default_int_value")
    private BigDecimal defaultIntValue;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExerciseCatalog getExerciseCatalog() { return exerciseCatalog; }
    public void setExerciseCatalog(ExerciseCatalog exerciseCatalog) { this.exerciseCatalog = exerciseCatalog; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public MeasurementUnit getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(MeasurementUnit measurementUnit) { this.measurementUnit = measurementUnit; }

    public Duration getDefaultTimeValue() { return defaultTimeValue; }
    public void setDefaultTimeValue(Duration defaultTimeValue) { this.defaultTimeValue = defaultTimeValue; }

    public BigDecimal getDefaultIntValue() { return defaultIntValue; }
    public void setDefaultIntValue(BigDecimal defaultIntValue) { this.defaultIntValue = defaultIntValue; }
}