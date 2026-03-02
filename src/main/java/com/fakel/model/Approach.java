package com.fakel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "approaches")
public class Approach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_in_training_id", nullable = false)
    private ExercisesInTraining exerciseInTraining;

    @Column(name = "approach_number", nullable = false)
    private Short approachNumber;  // ← ИСПРАВЛЕНО: Integer → Short

    @ManyToOne
    @JoinColumn(name = "exercise_parameter_id", nullable = false)
    private ExerciseParameter exerciseParameter;

    @Column(nullable = false)
    private BigDecimal value;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExercisesInTraining getExerciseInTraining() { return exerciseInTraining; }
    public void setExerciseInTraining(ExercisesInTraining exerciseInTraining) { this.exerciseInTraining = exerciseInTraining; }

    public Short getApproachNumber() { return approachNumber; }  // ← ИСПРАВЛЕНО
    public void setApproachNumber(Short approachNumber) { this.approachNumber = approachNumber; }  // ← ИСПРАВЛЕНО

    public ExerciseParameter getExerciseParameter() { return exerciseParameter; }
    public void setExerciseParameter(ExerciseParameter exerciseParameter) { this.exerciseParameter = exerciseParameter; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}