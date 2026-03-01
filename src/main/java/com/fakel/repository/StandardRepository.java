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

    List<Standard> findByNumber(Short number);

    Optional<Standard> findFirstByNumber(Short number);

    @Query("SELECT s FROM Standard s WHERE s.number = :number AND s.course = :course ORDER BY s.grade")
    List<Standard> findByNumberAndCourseOrderByGrade(@Param("number") Short number,
                                                     @Param("course") Short course);
}