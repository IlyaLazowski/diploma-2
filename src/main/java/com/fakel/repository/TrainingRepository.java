package com.fakel.repository;

import com.fakel.model.Training;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {

    // Базовые методы (Spring Data JPA сам поймет по названию)
    Page<Training> findByCadetId(Long cadetId, Pageable pageable);

    Page<Training> findByCadetIdAndDate(Long cadetId, LocalDate date, Pageable pageable);

    Page<Training> findByCadetIdAndDateBetween(Long cadetId, LocalDate start, LocalDate end, Pageable pageable);

    Page<Training> findByCadetIdAndType(Long cadetId, String type, Pageable pageable);

    Page<Training> findByCadetIdAndTypeAndDateBetween(Long cadetId, String type,
                                                      LocalDate start, LocalDate end, Pageable pageable);

    boolean existsByIdAndCadetId(Long id, Long cadetId);



    List<Training> findByCadetIdAndDateBetween(Long cadetId, LocalDate start, LocalDate end);

    @Query(value = """
        SELECT t.date, t.current_weight
        FROM trainings t
        WHERE t.cadet_id = :cadetId
        AND t.date BETWEEN :from AND :to
        AND t.current_weight IS NOT NULL
        ORDER BY t.date
    """, nativeQuery = true)
    List<Object[]> getWeightProgress(@Param("cadetId") Long cadetId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);
}