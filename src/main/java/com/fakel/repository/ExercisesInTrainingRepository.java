package com.fakel.repository;

import com.fakel.model.ExercisesInTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExercisesInTrainingRepository extends JpaRepository<ExercisesInTraining, Long> {
    List<ExercisesInTraining> findByTrainingId(Long trainingId);
    void deleteByTrainingId(Long trainingId);
}