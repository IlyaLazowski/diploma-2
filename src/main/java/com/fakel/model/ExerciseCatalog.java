package com.fakel.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise_catalogs")
public class ExerciseCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    private String description;

    @Column(length = 64)
    private String type; // Сила, Скорость, Выносливость

    @OneToMany(mappedBy = "exerciseCatalog", cascade = CascadeType.ALL)
    private List<ExerciseParameter> parameters = new ArrayList<>();

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<ExerciseParameter> getParameters() { return parameters; }
    public void setParameters(List<ExerciseParameter> parameters) { this.parameters = parameters; }
}