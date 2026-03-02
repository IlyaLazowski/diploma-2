package com.fakel.repository;

import com.fakel.model.ExerciseParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExerciseParameterRepository extends JpaRepository<ExerciseParameter, Long> {
    List<ExerciseParameter> findByExerciseCatalogId(Long exerciseCatalogId);
}