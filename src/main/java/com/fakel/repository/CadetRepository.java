package com.fakel.repository;

import com.fakel.model.Cadet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadetRepository extends JpaRepository<Cadet, Long> {

    Optional<Cadet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Long countByGroupId(Long groupId);

    // ⚠️ ИСПРАВЛЕНО: правильный JPQL запрос
    @Query("SELECT c FROM Cadet c WHERE c.user.login = :login")
    Optional<Cadet> findByUserLogin(@Param("login") String login);

    List<Cadet> findByGroupId(Long groupId);
}