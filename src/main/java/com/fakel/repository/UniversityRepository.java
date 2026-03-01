package com.fakel.repository;

import com.fakel.model.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    // Найти все университеты, отсортированные по оценке
    List<University> findAllByOrderByMarkDesc();
}