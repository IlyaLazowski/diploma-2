package com.fakel.repository;

import com.fakel.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // Все группы университета
    Page<Group> findByUniversityIdOrderByFoundationDateDescNumberAsc(Long universityId, Pageable pageable);

    // Группы университета по году
    Page<Group> findByUniversityIdAndFoundationDateBetween(
            Long universityId, LocalDate start, LocalDate end, Pageable pageable);

    // Найти группу по номеру (например "220302")
    Optional<Group> findByNumber(String number);
}