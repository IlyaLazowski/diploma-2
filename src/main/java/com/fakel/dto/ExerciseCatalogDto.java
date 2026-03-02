package com.fakel.dto;

import java.util.List;

public class ExerciseCatalogDto {
    private Long id;
    private String code;
    private String description;
    private String type;
    private List<ExerciseParameterDto> defaultParameters;

    public ExerciseCatalogDto() {}

    public ExerciseCatalogDto(Long id, String code, String description, String type,
                              List<ExerciseParameterDto> defaultParameters) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.type = type;
        this.defaultParameters = defaultParameters;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<ExerciseParameterDto> getDefaultParameters() { return defaultParameters; }
    public void setDefaultParameters(List<ExerciseParameterDto> defaultParameters) { this.defaultParameters = defaultParameters; }
}