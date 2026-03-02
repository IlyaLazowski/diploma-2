package com.fakel.repository;

import com.fakel.model.ExerciseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseCatalogRepository extends JpaRepository<ExerciseCatalog, Long> {

    List<ExerciseCatalog> findByType(String type);

    Optional<ExerciseCatalog> findByCode(String code);

    @Query("SELECT ec FROM ExerciseCatalog ec LEFT JOIN FETCH ec.parameters WHERE ec.code = :code")
    Optional<ExerciseCatalog> findByCodeWithParameters(@Param("code") String code);
}