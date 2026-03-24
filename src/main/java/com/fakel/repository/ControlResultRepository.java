package com.fakel.repository;

import com.fakel.model.ControlResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ControlResultRepository extends JpaRepository<ControlResult, Long> {

    List<ControlResult> findByControlId(Long controlId);

    Page<ControlResult> findByControlId(Long controlId, Pageable pageable);

    // ИСПРАВЛЕНО: правильное название метода
    Optional<ControlResult> findByControlIdAndCadet_UserIdAndStandardId(Long controlId, Long cadetId, Long standardId);

    List<ControlResult> findByControlIdAndCadet_UserId(Long controlId, Long cadetId);

    Long countByControlId(Long controlId);

    @Query("SELECT COUNT(DISTINCT cr.cadet.userId) FROM ControlResult cr WHERE cr.control.id = :controlId AND cr.mark >= 3")
    Long countPassedByControlId(@Param("controlId") Long controlId);

    @Query("SELECT AVG(cr.mark) FROM ControlResult cr WHERE cr.control.id = :controlId")
    Double averageMarkByControlId(@Param("controlId") Long controlId);

    @Query("SELECT cr.status, COUNT(cr) FROM ControlResult cr WHERE cr.control.id = :controlId GROUP BY cr.status")
    List<Object[]> getStatusStatistics(@Param("controlId") Long controlId);

    List<ControlResult> findByControlIdAndMarkGreaterThanEqual(Long controlId, Short mark);
    @Query("SELECT COUNT(DISTINCT cr.cadet.userId) FROM ControlResult cr WHERE cr.control.id = :controlId")
    Long countDistinctCadetsByControlId(@Param("controlId") Long controlId);


}