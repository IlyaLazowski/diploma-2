package com.fakel.dto;

import java.util.List;

public class ExerciseInTrainingDto {
    private Long id;
    private String exerciseCode;
    private String exerciseDescription;
    private Integer restPeriod; // отдых между подходами
    private List<ApproachDto> approaches;

    public ExerciseInTrainingDto() {}

    public ExerciseInTrainingDto(Long id, String exerciseCode, String exerciseDescription,
                                 Integer restPeriod, List<ApproachDto> approaches) {
        this.id = id;
        this.exerciseCode = exerciseCode;
        this.exerciseDescription = exerciseDescription;
        this.restPeriod = restPeriod;
        this.approaches = approaches;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExerciseCode() { return exerciseCode; }
    public void setExerciseCode(String exerciseCode) { this.exerciseCode = exerciseCode; }

    public String getExerciseDescription() { return exerciseDescription; }
    public void setExerciseDescription(String exerciseDescription) { this.exerciseDescription = exerciseDescription; }

    public Integer getRestPeriod() { return restPeriod; }
    public void setRestPeriod(Integer restPeriod) { this.restPeriod = restPeriod; }

    public List<ApproachDto> getApproaches() { return approaches; }
    public void setApproaches(List<ApproachDto> approaches) { this.approaches = approaches; }
}