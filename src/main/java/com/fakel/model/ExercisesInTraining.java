package com.fakel.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercises_in_training")
public class ExercisesInTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "training_id", nullable = false)
    private Training training;

    @ManyToOne
    @JoinColumn(name = "exercise_catalog_id", nullable = false)
    private ExerciseCatalog exerciseCatalog;

    @Column(name = "rest_period")
    private Short restPeriod;

    @OneToMany(mappedBy = "exerciseInTraining", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Approach> approaches = new ArrayList<>();

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }

    public ExerciseCatalog getExerciseCatalog() { return exerciseCatalog; }
    public void setExerciseCatalog(ExerciseCatalog exerciseCatalog) { this.exerciseCatalog = exerciseCatalog; }

    public Short getRestPeriod() { return restPeriod; }
    public void setRestPeriod(Short restPeriod) { this.restPeriod = restPeriod; }

    public List<Approach> getApproaches() { return approaches; }
    public void setApproaches(List<Approach> approaches) { this.approaches = approaches; }
}