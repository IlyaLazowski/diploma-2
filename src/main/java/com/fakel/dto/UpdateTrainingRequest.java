package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class UpdateTrainingRequest {

    private LocalDate date;
    private BigDecimal currentWeight;
    private Integer restPeriod; // отдых между упражнениями
    private List<UpdateExerciseRequest> exercises;

    public static class UpdateExerciseRequest {
        private Long exerciseInTrainingId;
        private Integer restPeriod; // отдых между подходами
        private List<UpdateApproachRequest> approaches;

        // Геттеры и сеттеры
        public Long getExerciseInTrainingId() { return exerciseInTrainingId; }
        public void setExerciseInTrainingId(Long exerciseInTrainingId) { this.exerciseInTrainingId = exerciseInTrainingId; }

        public Integer getRestPeriod() { return restPeriod; }
        public void setRestPeriod(Integer restPeriod) { this.restPeriod = restPeriod; }

        public List<UpdateApproachRequest> getApproaches() { return approaches; }
        public void setApproaches(List<UpdateApproachRequest> approaches) { this.approaches = approaches; }
    }

    public static class UpdateApproachRequest {
        private Long approachId;
        private BigDecimal value;

        public Long getApproachId() { return approachId; }
        public void setApproachId(Long approachId) { this.approachId = approachId; }

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    // Геттеры и сеттеры основного класса
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal currentWeight) { this.currentWeight = currentWeight; }

    public Integer getRestPeriod() { return restPeriod; }
    public void setRestPeriod(Integer restPeriod) { this.restPeriod = restPeriod; }

    public List<UpdateExerciseRequest> getExercises() { return exercises; }
    public void setExercises(List<UpdateExerciseRequest> exercises) { this.exercises = exercises; }
}