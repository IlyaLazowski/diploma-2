package com.fakel.repository;

import com.fakel.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT t FROM Teacher t WHERE t.user.login = :login")
    Optional<Teacher> findByUserLogin(@Param("login") String login);
}