package com.fakel.repository;

import com.fakel.model.Approach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApproachRepository extends JpaRepository<Approach, Long> {
    List<Approach> findByExerciseInTrainingId(Long exerciseInTrainingId);
    void deleteByExerciseInTrainingId(Long exerciseInTrainingId);
}