package com.fakel.repository;

import com.fakel.model.Control;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ControlRepository extends JpaRepository<Control, Long> {

    // Все контроли группы (сортировка по дате)
    Page<Control> findByGroupIdOrderByDateDesc(Long groupId, Pageable pageable);

    // Контроли группы с фильтром по дате (от и до)
    Page<Control> findByGroupIdAndDateBetweenOrderByDateDesc(
            Long groupId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Контроли группы с фильтром по типу
    Page<Control> findByGroupIdAndTypeOrderByDateDesc(
            Long groupId, String type, Pageable pageable);

    // Контроли группы с фильтром по дате и типу
    Page<Control> findByGroupIdAndTypeAndDateBetweenOrderByDateDesc(
            Long groupId, String type, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Для подсчета статистики
    Long countByGroupId(Long groupId);

    List<Control> findByGroupId(Long groupId);

    @Query("SELECT COUNT(DISTINCT cr.cadet.userId) FROM ControlResult cr WHERE cr.control.id = :controlId")
    Long countDistinctCadetsByControlId(@Param("controlId") Long controlId);
}