package com.fakel.repository;

import com.fakel.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {

    /**
     * Найти все нормативы по номеру (все оценки, все курсы)
     */
    List<Standard> findByNumber(Short number);

    /**
     * Найти все нормативы по номеру и курсу, отсортированные по оценке
     */
    List<Standard> findByNumberAndCourseOrderByGrade(Short number, Short course);

    /**
     * Найти норматив по номеру, курсу и оценке
     */
    Optional<Standard> findByNumberAndCourseAndGrade(Short number, Short course, String grade);

    /**
     * Найти норматив, подходящий под результат
     */
    @Query(value = """
        SELECT s.* FROM standards s 
        WHERE s.number = :number 
        AND s.course = :course
        AND (
            (s.time_value IS NOT NULL AND s.time_value <= CAST(:timeValue AS interval))
            OR (s.int_value IS NOT NULL AND s.int_value >= :intValue)
        )
        ORDER BY 
            CASE s.grade 
                WHEN 'Отлично' THEN 1 
                WHEN 'Хорошо' THEN 2 
                WHEN 'Удовлетворительно' THEN 3 
            END
        LIMIT 1
        """, nativeQuery = true)
    Optional<Standard> findMatchingStandard(
            @Param("number") Short number,
            @Param("course") Short course,
            @Param("timeValue") String timeValue,
            @Param("intValue") Integer intValue);
}