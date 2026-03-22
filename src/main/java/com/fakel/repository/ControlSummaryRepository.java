package com.fakel.repository;

import com.fakel.model.ControlSummary;
import com.fakel.model.ControlSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ControlSummaryRepository extends JpaRepository<ControlSummary, ControlSummaryId> {

    // Все итоги по контролю
    List<ControlSummary> findByControlId(Long controlId);

    @Query("SELECT cs FROM ControlSummary cs WHERE cs.control.id = :controlId AND cs.cadet.userId = :cadetId")
    Optional<ControlSummary> findByControlIdAndCadetId(@Param("controlId") Long controlId,
                                                       @Param("cadetId") Long cadetId);
}