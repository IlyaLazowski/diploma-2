package com.fakel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trainings")
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cadet_id", nullable = false)
    private Long cadetId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "current_weight")
    private BigDecimal currentWeight;

    @Column(name = "rest_period")
    private Short restPeriod;  // ← ИСПРАВЛЕНО: Integer → Short

    @Column(length = 64)
    private String type; // Сила, Скорость, Выносливость

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExercisesInTraining> exercises = new ArrayList<>();

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal currentWeight) { this.currentWeight = currentWeight; }

    public Short getRestPeriod() { return restPeriod; }  // ← ИСПРАВЛЕНО
    public void setRestPeriod(Short restPeriod) { this.restPeriod = restPeriod; }  // ← ИСПРАВЛЕНО

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<ExercisesInTraining> getExercises() { return exercises; }
    public void setExercises(List<ExercisesInTraining> exercises) { this.exercises = exercises; }
}