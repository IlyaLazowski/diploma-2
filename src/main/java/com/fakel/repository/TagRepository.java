package com.fakel.repository;

import com.fakel.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByCode(String code);
    List<Tag> findByCodeIn(List<String> codes);
}