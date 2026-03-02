package com.fakel.dto;

import java.time.LocalDate;
import java.util.List;

public class TrainingDto {
    private Long id;
    private LocalDate date;
    private Double currentWeight;
    private String type;
    private Integer restPeriod; // отдых между упражнениями
    private List<ExerciseInTrainingDto> exercises;

    public TrainingDto() {}

    public TrainingDto(Long id, LocalDate date, Double currentWeight, String type,
                       Integer restPeriod, List<ExerciseInTrainingDto> exercises) {
        this.id = id;
        this.date = date;
        this.currentWeight = currentWeight;
        this.type = type;
        this.restPeriod = restPeriod;
        this.exercises = exercises;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Double currentWeight) { this.currentWeight = currentWeight; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getRestPeriod() { return restPeriod; }
    public void setRestPeriod(Integer restPeriod) { this.restPeriod = restPeriod; }

    public List<ExerciseInTrainingDto> getExercises() { return exercises; }
    public void setExercises(List<ExerciseInTrainingDto> exercises) { this.exercises = exercises; }
}