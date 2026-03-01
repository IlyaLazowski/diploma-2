package com.fakel.repository;

import com.fakel.model.ControlStandard;
import com.fakel.model.ControlStandard.ControlStandardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ControlStandardRepository extends JpaRepository<ControlStandard, ControlStandardId> {

    List<ControlStandard> findByControlId(Long controlId);

    @Query("SELECT cs.id.standardNumber FROM ControlStandard cs WHERE cs.control.id = :controlId")
    List<Short> findStandardNumbersByControlId(@Param("controlId") Long controlId);
}