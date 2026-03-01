package com.fakel.repository;

import com.fakel.model.ControlResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ControlResultRepository extends JpaRepository<ControlResult, Long> {

    // Все результаты контроля
    List<ControlResult> findByControlId(Long controlId);

    // Все результаты контроля с пагинацией
    Page<ControlResult> findByControlId(Long controlId, Pageable pageable);

    // ⚠️ ИСПРАВЛЕНО: cadet.userId вместо cadet.id
    List<ControlResult> findByControlIdAndCadet_UserId(Long controlId, Long cadetId);

    // Количество курсантов в контроле
    Long countByControlId(Long controlId);

    // Количество сдавших (mark >= 3)
    @Query("SELECT COUNT(cr) FROM ControlResult cr WHERE cr.control.id = :controlId AND cr.mark >= 3")
    Long countPassedByControlId(@Param("controlId") Long controlId);

    // Средний балл
    @Query("SELECT AVG(cr.mark) FROM ControlResult cr WHERE cr.control.id = :controlId")
    Double averageMarkByControlId(@Param("controlId") Long controlId);

    // Статистика по статусам
    @Query("SELECT cr.status, COUNT(cr) FROM ControlResult cr WHERE cr.control.id = :controlId GROUP BY cr.status")
    List<Object[]> getStatusStatistics(@Param("controlId") Long controlId);

    // Все результаты с оценками выше N
    List<ControlResult> findByControlIdAndMarkGreaterThanEqual(Long controlId, Short mark);



}