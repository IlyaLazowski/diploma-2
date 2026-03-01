package com.fakel.repository;

import com.fakel.model.Inspector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InspectorRepository extends JpaRepository<Inspector, Long> {

    // Все инспекторы контроля
    List<Inspector> findByControlId(Long controlId);

    // Количество инспекторов в контроле
    Long countByControlId(Long controlId);

    // Внешние инспекторы
    List<Inspector> findByControlIdAndExternalTrue(Long controlId);

    // Внутренние (преподаватели)
    List<Inspector> findByControlIdAndExternalFalse(Long controlId);
}